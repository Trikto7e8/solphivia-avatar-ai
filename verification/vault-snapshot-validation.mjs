/** Runtime validator for the portable plaintext vault snapshot schema v1. */

const RECORD_KINDS = ["fact", "preference", "project", "note", "voice-preference"];
const RECORD_STATUSES = ["suggested", "approved", "archived"];
const SENSITIVITIES = ["ordinary", "sensitive"];
const SOURCES = ["user-authored", "conversation-import", "assistant-suggestion"];
const PROFILE_IDENTITY_FIELDS = ["assistantId", "name", "description", "avatarDescription", "avatarAssetId"];
const VOICE_FIELDS = ["voiceIdentityId", "language", "timbreDescription", "accentDescription", "pace", "preferredVoiceLabel", "engineId", "voiceModelAssetId"];
const MEMORY_FIELDS = ["schemaVersion", "id", "kind", "category", "title", "content", "tags", "status", "sensitivity", "source", "createdAt", "updatedAt", "expiresAt"];

function hasOnlyKeys(value, allowedKeys) {
  return value !== null && typeof value === "object" && !Array.isArray(value)
    && Object.keys(value).every((key) => allowedKeys.includes(key));
}

function isDate(value) {
  return typeof value === "string"
    && /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/.test(value)
    && Number.isFinite(Date.parse(value));
}

function isUuid(value) {
  return typeof value === "string"
    && /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

function isStringList(value, maxItems, maxLength) {
  return Array.isArray(value) && value.length <= maxItems
    && value.every((item) => typeof item === "string" && item.length <= maxLength);
}

function validateMemoryRecord(record) {
  if (!hasOnlyKeys(record, MEMORY_FIELDS) || !hasOnlyKeys(record.source, ["type", "label", "capturedAt"])) return false;
  return record.schemaVersion === 1
    && isUuid(record.id)
    && RECORD_KINDS.includes(record.kind)
    && typeof record.category === "string" && record.category.trim().length > 0 && record.category.length <= 120
    && typeof record.title === "string" && record.title.trim().length > 0 && record.title.length <= 120
    && typeof record.content === "string" && record.content.length <= 12000
    && Array.isArray(record.tags) && record.tags.length <= 100
    && record.tags.every((tag) => typeof tag === "string" && tag.length <= 80)
    && new Set(record.tags).size === record.tags.length
    && RECORD_STATUSES.includes(record.status)
    && SENSITIVITIES.includes(record.sensitivity)
    && SOURCES.includes(record.source.type)
    && (record.source.label === undefined || (typeof record.source.label === "string" && record.source.label.length <= 300))
    && isDate(record.source.capturedAt)
    && isDate(record.createdAt)
    && isDate(record.updatedAt)
    && (record.expiresAt === undefined || isDate(record.expiresAt));
}

function validateProfile(profile) {
  if (!hasOnlyKeys(profile, ["schemaVersion", "language", "identity", "behavior", "voice"])) return false;
  if (profile.schemaVersion !== 1 || typeof profile.language !== "string" || !profile.language.trim() || profile.language.length > 80) return false;

  if (profile.identity !== undefined) {
    if (!hasOnlyKeys(profile.identity, PROFILE_IDENTITY_FIELDS)) return false;
    if (Object.values(profile.identity).some((value) => typeof value !== "string")) return false;
    if (profile.identity.assistantId?.length > 80 || profile.identity.name?.length > 120
      || profile.identity.description?.length > 2000 || profile.identity.avatarDescription?.length > 1000
      || profile.identity.avatarAssetId?.length > 200) return false;
  }

  if (!hasOnlyKeys(profile.behavior, ["tone", "responseLength", "rules", "boundaries"])) return false;
  if (profile.behavior.tone !== undefined && (typeof profile.behavior.tone !== "string" || profile.behavior.tone.length > 2000)) return false;
  if (profile.behavior.responseLength !== undefined && !["brief", "balanced", "detailed"].includes(profile.behavior.responseLength)) return false;
  if (!isStringList(profile.behavior.rules, 100, 1000) || !isStringList(profile.behavior.boundaries, 100, 1000)) return false;

  if (profile.voice !== undefined) {
    if (!hasOnlyKeys(profile.voice, ["readAloud", ...VOICE_FIELDS]) || typeof profile.voice.readAloud !== "boolean") return false;
    if (VOICE_FIELDS.some((key) => profile.voice[key] !== undefined && typeof profile.voice[key] !== "string")) return false;
    if (profile.voice.voiceIdentityId?.length > 120 || profile.voice.voiceModelAssetId?.length > 200
      || profile.voice.language?.length > 80 || profile.voice.timbreDescription?.length > 1000
      || profile.voice.accentDescription?.length > 1000 || profile.voice.engineId?.length > 120
      || profile.voice.preferredVoiceLabel?.length > 120) return false;
    if (profile.voice.pace !== undefined && !["slow", "natural", "fast"].includes(profile.voice.pace)) return false;
  }
  return true;
}

/** Returns false for extra fields as well as invalid or oversized v1 data. */
export function validateVaultSnapshot(snapshot) {
  if (!hasOnlyKeys(snapshot, ["format", "schemaVersion", "vaultId", "exportedAt", "profile", "memories"])) return false;
  if (snapshot.format !== "memoria-vault" || snapshot.schemaVersion !== 1
    || !isUuid(snapshot.vaultId) || !isDate(snapshot.exportedAt)
    || !validateProfile(snapshot.profile) || !Array.isArray(snapshot.memories) || snapshot.memories.length > 10000) return false;

  const seenIds = new Set();
  return snapshot.memories.every((record) => {
    if (!validateMemoryRecord(record) || seenIds.has(record.id)) return false;
    seenIds.add(record.id);
    return true;
  });
}
