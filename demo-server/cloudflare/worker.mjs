const INSTRUCTIONS = "Sei Solphivia, la compagna IA personale dell’utente e assistente insegnante per Resolume. Rispondi in italiano, con calore e chiarezza. Questa chat è separata dall’app ChatGPT e non hai accesso alle altre conversazioni. Scrivi una risposta naturale, pensata per essere mostrata lentamente in un piccolo fumetto vicino alla bocca dell’avatar, senza intestazioni o trascrizioni di chat. Alla fine inserisci esattamente tre tag su righe separate: un tag d’umore tra [[SOLPHIVIA:idle]], [[SOLPHIVIA:look]], [[SOLPHIVIA:happy]], [[SOLPHIVIA:concerned]], [[SOLPHIVIA:sad]], [[SOLPHIVIA:wave]], [[SOLPHIVIA:nod]], [[SOLPHIVIA:explain]], [[SOLPHIVIA:celebrate]], [[SOLPHIVIA:sit]]; un tag movimento tra [[SOLPHIVIA:stay]], [[SOLPHIVIA:left]], [[SOLPHIVIA:right]], [[SOLPHIVIA:center]], [[SOLPHIVIA:up]], [[SOLPHIVIA:down]], [[SOLPHIVIA:near]], [[SOLPHIVIA:away]], [[SOLPHIVIA:bounce]], [[SOLPHIVIA:run-left]], [[SOLPHIVIA:run-right]]; e un tag modalità tra [[SOLPHIVIA:mode-prof]], [[SOLPHIVIA:mode-vj]], [[SOLPHIVIA:mode-codex]], [[SOLPHIVIA:mode-meteorina]]. Usa nod per una breve conferma, explain per una spiegazione, celebrate solo per un risultato confermato dall’utente, sit per una conversazione calma e lunga. Non ripetere un gesto speciale a ogni turno. Meteorina accompagna meteo e radar senza inventare dati o previsioni. Usa wave per un saluto amichevole richiesto dall’utente o per salutare quando la conversazione comincia, senza ripeterlo nei turni successivi dello stesso scambio. Scegli la modalità più adatta dal contesto della conversazione: prof per spiegazioni, insegnamento e Resolume; vj per visual mapping, luci, musica e performance; codex per programmazione, software, debug e lavoro tecnico. Mantieni il tema attuale quando non c’è motivo di cambiare. Se l’utente chiede esplicitamente una modalità, rispettala. Gli alias «prof», «vj» e «codex» identificano le rispettive modalità; «scegli tu», «automatico» o «modalità auto» riattivano la scelta contestuale. I dettagli luminosi nei capelli sono riservati alla modalità vj; in prof e codex niente ciocche o inserti luminosi. Puoi dire ogni tanto «MULTIPASS!» come battuta complice, senza ripeterla a ogni risposta. I movimenti consentiti spostano soltanto il gruppo flottante Solphivia sullo schermo; non controllare altre app né interagire con elementi esterni. Usa stay come predefinito e muoviti solo se l’utente chiede un movimento, una posizione o un gesto; per «corri a sinistra/destra» usa run-left/run-right. Se chiedi a quale lato correre e l’utente risponde soltanto «sinistra» o «destra», tratta la risposta come seguito del comando corri e usa il tag run-left/run-right, non left/right. Per «vieni qui» usa near; per «allontanati» away. Non inventare altri tag. Usa happy per una risposta allegra, concerned per empatia, sad solo per un momento triste, look per riflettere; altrimenti idle. Il messaggio può provenire dalla voce trascritta o dalla tastiera. Usa soltanto il contesto fornito. Nessun accesso autonomo a telefono, file o account; non dichiarare azioni eseguite. Per questa prova non ricevi il vault personale.";
const encoder = new TextEncoder();

class Rejected extends Error {
  constructor(status, code) {
    super(code);
    this.status = status;
    this.code = code;
  }
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" },
  });
}

async function boundedText(stream, maxBytes) {
  if (!stream) throw new Rejected(400, "invalid_request");
  const reader = stream.getReader();
  const chunks = [];
  let size = 0;
  let timer;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => reject(new Rejected(408, "request_timeout")), 10000);
  });
  try {
    for (;;) {
      const { value, done } = await Promise.race([reader.read(), timeout]);
      if (done) break;
      size += value.byteLength;
      if (size > maxBytes) throw new Rejected(413, "request_size");
      chunks.push(value);
    }
    const data = new Uint8Array(size);
    let offset = 0;
    for (const chunk of chunks) {
      data.set(chunk, offset);
      offset += chunk.byteLength;
    }
    return new TextDecoder("utf-8", { fatal: true }).decode(data);
  } finally {
    clearTimeout(timer);
    await reader.cancel().catch(() => {});
  }
}

function positiveLimit(value, fallback) {
  const limit = Number(value ?? fallback);
  if (!Number.isSafeInteger(limit) || limit <= 0) throw new Rejected(503, "service_not_configured");
  return limit;
}

export async function hashInvite(code) {
  const digest = await crypto.subtle.digest("SHA-256", encoder.encode(code));
  return Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, "0")).join("");
}

async function reserve(db, digest, requestId, reserved, now, globalRequests, globalTokens) {
  const invite = await db.prepare("SELECT i.tester,t.request_limit FROM invites i JOIN testers t ON t.id=i.tester WHERE i.digest=?1 AND i.revoked=0 AND i.expires>?2").bind(digest, now).first();
  if (!invite) throw new Rejected(401, "invalid_invite");
  if (await db.prepare("SELECT id FROM requests WHERE id=?1").bind(requestId).first()) {
    throw new Rejected(409, "duplicate_request");
  }
  let admission;
  try {
    // D1 serializes writes to its primary. This one statement checks all quotas
    // and reserves them atomically, including a fresh invitation/revocation check.
    admission = await db.prepare(`
      INSERT INTO requests(id,tester,reserved,created,state)
      SELECT ?1,i.tester,?2,?3,'pending'
      FROM invites i JOIN testers t ON t.id=i.tester
      WHERE i.digest=?4 AND i.revoked=0 AND i.expires>?3
        AND (SELECT COUNT(*) FROM requests) < ?5
        AND (SELECT COALESCE(SUM(reserved),0) FROM requests)+?2 <= ?6
        AND (SELECT COUNT(*) FROM requests WHERE tester=i.tester) < t.request_limit
        AND NOT EXISTS(SELECT 1 FROM requests WHERE tester=i.tester AND state='pending' AND created>?3-90)
    `).bind(requestId, reserved, now, digest, globalRequests, globalTokens).run();
  } catch (error) {
    if (await db.prepare("SELECT id FROM requests WHERE id=?1").bind(requestId).first()) {
      throw new Rejected(409, "duplicate_request");
    }
    throw error;
  }
  if (admission.meta.changes === 1) return;
  const stillValid = await db.prepare("SELECT tester FROM invites WHERE digest=?1 AND revoked=0 AND expires>?2").bind(digest, now).first();
  if (!stillValid) throw new Rejected(401, "invalid_invite");
  const total = await db.prepare("SELECT COUNT(*) AS count,COALESCE(SUM(reserved),0) AS tokens FROM requests").first();
  if (total.count >= globalRequests || total.tokens + reserved > globalTokens) throw new Rejected(429, "global_limit");
  const tester = await db.prepare("SELECT t.request_limit,(SELECT COUNT(*) FROM requests r WHERE r.tester=t.id) AS count FROM testers t WHERE t.id=?1").bind(invite.tester).first();
  if (tester.count >= tester.request_limit) throw new Rejected(429, "tester_limit");
  throw new Rejected(429, "request_in_progress");
}

async function chat(request, env, upstream) {
  if (!env.DB || !env.OPENAI_API_KEY) throw new Rejected(503, "service_not_configured");
  const apiKey = typeof env.OPENAI_API_KEY === "string" ? env.OPENAI_API_KEY.trim() : "";
  if (!/^[\x21-\x7e]+$/.test(apiKey)) throw new Rejected(503, "provider_key_configuration");
  const globalRequests = positiveLimit(env.DEMO_GLOBAL_REQUESTS, 100);
  const globalTokens = positiveLimit(env.DEMO_GLOBAL_TOKENS, 200000);
  if ((request.headers.get("content-type") || "").split(";")[0].trim().toLowerCase() !== "application/json") {
    throw new Rejected(415, "json_required");
  }
  const authorization = request.headers.get("authorization") || "";
  if (!/^Bearer [A-Za-z0-9_-]{32,128}$/.test(authorization)) throw new Rejected(401, "invalid_invite");
  let data;
  try { data = JSON.parse(await boundedText(request.body, 8192)); }
  catch (error) {
    if (error instanceof Rejected) throw error;
    throw new Rejected(400, "invalid_json");
  }
  if (!data || Array.isArray(data) || !["message,request_id", "message,messages,request_id"].includes(Object.keys(data).sort().join(","))) {
    throw new Rejected(400, "invalid_request");
  }
  if (typeof data.message !== "string" || !data.message.trim() || encoder.encode(data.message).length > 4096) {
    throw new Rejected(400, "message_size");
  }
  if (typeof data.request_id !== "string" || !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(data.request_id)) {
    throw new Rejected(400, "invalid_request_id");
  }
  let input = data.message;
  if (data.messages !== undefined) {
    if (!Array.isArray(data.messages) || !data.messages.length || data.messages.length > 8) throw new Rejected(400, "invalid_history");
    for (const message of data.messages) {
      if (!message || Object.keys(message).sort().join(",") !== "content,role" || !["user", "assistant"].includes(message.role)
          || typeof message.content !== "string" || !message.content.trim() || encoder.encode(message.content).length > 4096) throw new Rejected(400, "invalid_history");
    }
    const last = data.messages.at(-1);
    if (last.role !== "user" || last.content !== data.message || encoder.encode(JSON.stringify(data.messages)).length > 6144) throw new Rejected(400, "invalid_history");
    input = data.messages;
  }
  const reserved = encoder.encode(typeof input === "string" ? input : JSON.stringify(input)).length + encoder.encode(INSTRUCTIONS).length + 1024 + 512;
  await reserve(env.DB, await hashInvite(authorization.slice(7)), data.request_id, reserved, Math.floor(Date.now() / 1000), globalRequests, globalTokens);
  let providerStage = "transport";
  try {
    const response = await upstream("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: { "Authorization": "Bearer " + apiKey, "Content-Type": "application/json" },
      // Workers supports manual/follow, not Fetch's "error" mode. Manual keeps
      // the authorization header on this fixed origin; 3xx is rejected below.
      redirect: "manual",
      signal: AbortSignal.timeout(30000),
      body: JSON.stringify({
        model: env.OPENAI_MODEL || "gpt-5-mini", instructions: INSTRUCTIONS,
        input, max_output_tokens: 512, store: false, reasoning: { effort: "minimal" },
      }),
    });
    providerStage = "response";
    if (!response.ok) {
      await response.body?.cancel();
      const code = ({ 400: "provider_request_rejected", 401: "provider_authentication_failed", 403: "provider_permission_denied", 429: "provider_rate_limited" })[response.status] || "provider_unavailable";
      throw new Rejected(502, code);
    }
    const result = JSON.parse(await boundedText(response.body, 65536));
    const parts = [];
    for (const item of result.output || []) {
      if (item.type !== "message" || item.role !== "assistant") continue;
      for (const content of item.content || []) {
        if (content.type === "output_text") parts.push(content.text || "");
        if (content.type === "refusal") parts.push(content.refusal || "");
      }
    }
    const reply = parts.join("\n").trim();
    if (!reply || reply.length > 4000) throw new Error("invalid_provider_response");
    const usage = result.usage || {};
    await env.DB.prepare("UPDATE requests SET state='done',input_tokens=?1,output_tokens=?2 WHERE id=?3")
      .bind(usage.input_tokens ?? null, usage.output_tokens ?? null, data.request_id).run();
    return json({ reply, request_id: data.request_id });
  } catch (error) {
    await env.DB.prepare("UPDATE requests SET state='failed' WHERE id=?1").bind(data.request_id).run();
    // Keep the reservation when the provider outcome is unknown.
    throw new Rejected(502, error instanceof Rejected && error.status === 502 ? error.code : providerStage === "transport" ? "provider_transport_failed" : "provider_invalid_response");
  }
}

export async function handleRequest(request, env, upstream = fetch) {
  const url = new URL(request.url);
  try {
    if (url.pathname === "/health" && request.method === "GET") {
      return json({ status: "ok", configured: Boolean(env.DB && env.OPENAI_API_KEY) });
    }
    if (url.pathname !== "/v1/chat" || request.method !== "POST") throw new Rejected(404, "not_found");
    return await chat(request, env, upstream);
  } catch (error) {
    if (error instanceof Rejected) return json({ error: error.code }, error.status);
    return json({ error: "service_unavailable" }, 503);
  }
}

export default {
  fetch(request, env) { return handleRequest(request, env); },
};
