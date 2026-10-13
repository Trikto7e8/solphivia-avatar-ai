/** Portable encrypted-envelope primitives shared by local vault clients. */

export const PBKDF2_ITERATIONS = 600_000;
export const MAX_BACKUP_BYTES = 25 * 1024 * 1024;

const encoder = new TextEncoder();
const decoder = new TextDecoder();

function randomBytes(length) {
  return crypto.getRandomValues(new Uint8Array(length));
}

function toBase64(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary);
}

function fromBase64(value) {
  const binary = atob(value);
  return Uint8Array.from(binary, (character) => character.charCodeAt(0));
}

function hasOnlyKeys(value, allowedKeys) {
  return value !== null && typeof value === "object" && !Array.isArray(value)
    && Object.keys(value).every((key) => allowedKeys.includes(key));
}

function bytesEqual(left, right) {
  if (left.byteLength !== right.byteLength) return false;
  let difference = 0;
  for (let index = 0; index < left.byteLength; index += 1) difference |= left[index] ^ right[index];
  return difference === 0;
}

async function assertKeyPairMatches(privateKey, encodedPublicKey) {
  const publicKey = await crypto.subtle.importKey(
    "spki",
    encodedPublicKey,
    { name: "RSA-OAEP", hash: "SHA-256" },
    false,
    ["encrypt"],
  );
  const challenge = randomBytes(32);
  let challengeCiphertext;
  let recoveredChallenge;
  try {
    challengeCiphertext = new Uint8Array(await crypto.subtle.encrypt({ name: "RSA-OAEP" }, publicKey, challenge));
    recoveredChallenge = new Uint8Array(await crypto.subtle.decrypt({ name: "RSA-OAEP" }, privateKey, challengeCiphertext));
    if (!bytesEqual(challenge, recoveredChallenge)) throw new Error("The encrypted vault contains a mismatched public/private key pair.");
  } finally {
    challenge.fill(0);
    challengeCiphertext?.fill(0);
    recoveredChallenge?.fill(0);
  }
}

function isBase64(value, minBytes, maxBytes) {
  if (typeof value !== "string" || value.length % 4 !== 0 || !/^[A-Za-z0-9+/]*={0,2}$/.test(value)) return false;
  const byteLength = Math.floor(value.length * 3 / 4) - (value.endsWith("==") ? 2 : value.endsWith("=") ? 1 : 0);
  return byteLength >= minBytes && byteLength <= maxBytes;
}

function requireValidSnapshot(snapshot, validateSnapshot) {
  if (typeof validateSnapshot !== "function") throw new Error("A snapshot validator is required.");
  if (!validateSnapshot(snapshot)) throw new Error("Vault snapshot is invalid or contains unsupported fields.");
}

async function deriveWrappingKey(passphrase, salt) {
  if (typeof passphrase !== "string" || passphrase.length < 12) {
    throw new Error("A passphrase of at least 12 characters is required.");
  }
  const passphraseBytes = encoder.encode(passphrase);
  let material;
  try {
    material = await crypto.subtle.importKey("raw", passphraseBytes, "PBKDF2", false, ["deriveKey"]);
  } finally {
    passphraseBytes.fill(0);
  }
  return crypto.subtle.deriveKey(
    { name: "PBKDF2", salt, iterations: PBKDF2_ITERATIONS, hash: "SHA-256" },
    material,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}

async function encryptBytes(key, bytes, iv) {
  return new Uint8Array(await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, bytes));
}

async function decryptBytes(key, bytes, iv) {
  return new Uint8Array(await crypto.subtle.decrypt({ name: "AES-GCM", iv }, key, bytes));
}

async function encryptJson(key, value, iv) {
  const plaintext = encoder.encode(JSON.stringify(value));
  try {
    return await encryptBytes(key, plaintext, iv);
  } finally {
    plaintext.fill(0);
  }
}

/** Create an unlocked in-memory vault session and its portable encrypted envelope. */
export async function createVaultSession(passphrase, snapshot, validateSnapshot) {
  requireValidSnapshot(snapshot, validateSnapshot);
  const salt = randomBytes(16);
  const privateKeyIv = randomBytes(12);
  const snapshotIv = randomBytes(12);
  const rsa = await crypto.subtle.generateKey(
    { name: "RSA-OAEP", modulusLength: 3072, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
    true,
    ["encrypt", "decrypt"],
  );
  const publicKey = new Uint8Array(await crypto.subtle.exportKey("spki", rsa.publicKey));
  const privateKey = new Uint8Array(await crypto.subtle.exportKey("pkcs8", rsa.privateKey));
  let encryptedPrivateKey;
  try {
    const wrappingKey = await deriveWrappingKey(passphrase, salt);
    encryptedPrivateKey = await encryptBytes(wrappingKey, privateKey, privateKeyIv);
  } finally {
    privateKey.fill(0);
  }

  const dataKeyRaw = randomBytes(32);
  let wrappedDataKey;
  let dataKey;
  try {
    wrappedDataKey = new Uint8Array(await crypto.subtle.encrypt({ name: "RSA-OAEP" }, rsa.publicKey, dataKeyRaw));
    dataKey = await crypto.subtle.importKey("raw", dataKeyRaw, { name: "AES-GCM" }, false, ["encrypt", "decrypt"]);
  } finally {
    dataKeyRaw.fill(0);
  }
  const ciphertext = await encryptJson(dataKey, snapshot, snapshotIv);

  const envelope = {
    format: "memoria-vault-encrypted",
    schemaVersion: 1,
    kdf: { name: "PBKDF2", hash: "SHA-256", iterations: PBKDF2_ITERATIONS, salt: toBase64(salt) },
    keyWrapping: { name: "RSA-OAEP", hash: "SHA-256", publicKey: toBase64(publicKey), wrappedDataKey: toBase64(wrappedDataKey) },
    privateKeyEnvelope: { algorithm: "AES-GCM", iv: toBase64(privateKeyIv), ciphertext: toBase64(encryptedPrivateKey) },
    payload: { algorithm: "AES-GCM", iv: toBase64(snapshotIv), ciphertext: toBase64(ciphertext) },
  };
  return { envelope, snapshot, dataKey };
}

/** Create only the portable envelope when the caller does not need an open session. */
export async function createEnvelope(passphrase, snapshot, validateSnapshot) {
  return (await createVaultSession(passphrase, snapshot, validateSnapshot)).envelope;
}

/** Validate exact envelope shape and encoded sizes before expensive key operations. */
export function validateEnvelope(envelope, maxPayloadBytes = MAX_BACKUP_BYTES) {
  if (!Number.isSafeInteger(maxPayloadBytes) || maxPayloadBytes < 16 || maxPayloadBytes > MAX_BACKUP_BYTES) return false;
  return hasOnlyKeys(envelope, ["format", "schemaVersion", "kdf", "keyWrapping", "privateKeyEnvelope", "payload"])
    && hasOnlyKeys(envelope.kdf, ["name", "hash", "iterations", "salt"])
    && hasOnlyKeys(envelope.keyWrapping, ["name", "hash", "publicKey", "wrappedDataKey"])
    && hasOnlyKeys(envelope.privateKeyEnvelope, ["algorithm", "iv", "ciphertext"])
    && hasOnlyKeys(envelope.payload, ["algorithm", "iv", "ciphertext"])
    && envelope.format === "memoria-vault-encrypted"
    && envelope.schemaVersion === 1
    && envelope.kdf.name === "PBKDF2"
    && envelope.kdf.hash === "SHA-256"
    && envelope.kdf.iterations === PBKDF2_ITERATIONS
    && isBase64(envelope.kdf.salt, 16, 16)
    && envelope.keyWrapping.name === "RSA-OAEP"
    && envelope.keyWrapping.hash === "SHA-256"
    && isBase64(envelope.keyWrapping.publicKey, 300, 1024)
    && isBase64(envelope.keyWrapping.wrappedDataKey, 384, 384)
    && envelope.privateKeyEnvelope.algorithm === "AES-GCM"
    && isBase64(envelope.privateKeyEnvelope.iv, 12, 12)
    && isBase64(envelope.privateKeyEnvelope.ciphertext, 1000, 3000)
    && envelope.payload.algorithm === "AES-GCM"
    && isBase64(envelope.payload.iv, 12, 12)
    && isBase64(envelope.payload.ciphertext, 16, maxPayloadBytes);
}

/** Decrypt only after checking envelope and snapshot structure; caller owns snapshot validation. */
export async function unlockEnvelope(envelope, passphrase, validateSnapshot) {
  if (!validateEnvelope(envelope)) throw new Error("Encrypted vault format is not recognized.");
  if (typeof validateSnapshot !== "function") throw new Error("A snapshot validator is required.");
  const wrappingKey = await deriveWrappingKey(passphrase, fromBase64(envelope.kdf.salt));
  const privateBytes = await decryptBytes(wrappingKey, fromBase64(envelope.privateKeyEnvelope.ciphertext), fromBase64(envelope.privateKeyEnvelope.iv));
  let privateKey;
  try {
    privateKey = await crypto.subtle.importKey("pkcs8", privateBytes, { name: "RSA-OAEP", hash: "SHA-256" }, false, ["decrypt"]);
  } finally {
    privateBytes.fill(0);
  }
  await assertKeyPairMatches(privateKey, fromBase64(envelope.keyWrapping.publicKey));
  const dataKeyRaw = new Uint8Array(await crypto.subtle.decrypt({ name: "RSA-OAEP" }, privateKey, fromBase64(envelope.keyWrapping.wrappedDataKey)));
  let dataKey;
  try {
    dataKey = await crypto.subtle.importKey("raw", dataKeyRaw, { name: "AES-GCM" }, false, ["encrypt", "decrypt"]);
  } finally {
    dataKeyRaw.fill(0);
  }
  const plaintext = await decryptBytes(dataKey, fromBase64(envelope.payload.ciphertext), fromBase64(envelope.payload.iv));
  let snapshot;
  try {
    snapshot = JSON.parse(decoder.decode(plaintext));
  } finally {
    plaintext.fill(0);
  }
  requireValidSnapshot(snapshot, validateSnapshot);
  return { envelope, snapshot, dataKey };
}

/** Re-encrypt only the stored private-key envelope; the vault data key and payload stay unchanged. */
export async function changeEnvelopePassphrase(envelope, currentPassphrase, newPassphrase) {
  if (!validateEnvelope(envelope)) throw new Error("Encrypted vault format is not recognized.");
  if (typeof newPassphrase !== "string" || newPassphrase.length < 12) {
    throw new Error("A new passphrase of at least 12 characters is required.");
  }
  const currentWrappingKey = await deriveWrappingKey(currentPassphrase, fromBase64(envelope.kdf.salt));
  const privateBytes = await decryptBytes(
    currentWrappingKey,
    fromBase64(envelope.privateKeyEnvelope.ciphertext),
    fromBase64(envelope.privateKeyEnvelope.iv),
  );
  try {
    const privateKey = await crypto.subtle.importKey("pkcs8", privateBytes, { name: "RSA-OAEP", hash: "SHA-256" }, false, ["decrypt"]);
    await assertKeyPairMatches(privateKey, fromBase64(envelope.keyWrapping.publicKey));
    const salt = randomBytes(16);
    const iv = randomBytes(12);
    const newWrappingKey = await deriveWrappingKey(newPassphrase, salt);
    const ciphertext = await encryptBytes(newWrappingKey, privateBytes, iv);
    return {
      ...envelope,
      kdf: { ...envelope.kdf, salt: toBase64(salt) },
      privateKeyEnvelope: { algorithm: "AES-GCM", iv: toBase64(iv), ciphertext: toBase64(ciphertext) },
    };
  } finally {
    privateBytes.fill(0);
  }
}

/** Encrypt a new snapshot revision with the unlocked vault data key. */
export async function encryptSnapshotPayload(dataKey, snapshot, validateSnapshot) {
  requireValidSnapshot(snapshot, validateSnapshot);
  const iv = randomBytes(12);
  const ciphertext = await encryptJson(dataKey, snapshot, iv);
  return { algorithm: "AES-GCM", iv: toBase64(iv), ciphertext: toBase64(ciphertext) };
}
