import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync } from "node:fs";
import { randomUUID } from "node:crypto";
import { handleRequest, hashInvite } from "./worker.mjs";

function database() {
  const sqlite = new DatabaseSync(":memory:");
  sqlite.exec(readFileSync(new URL("./schema.sql", import.meta.url), "utf8"));
  return {
    sqlite,
    prepare(sql) {
      let values = [];
      function prepared() {
        const ordered = [];
        const expanded = sql.replace(/\?(\d+)/g, (_, number) => {
          ordered.push(values[Number(number) - 1]);
          return "?";
        });
        return { statement: sqlite.prepare(expanded), ordered };
      }
      return {
        bind(...args) { values = args; return this; },
        async first() {
          const { statement, ordered } = prepared();
          return statement.get(...ordered) || null;
        },
        async run() {
          const { statement, ordered } = prepared();
          return { meta: { changes: Number(statement.run(...ordered).changes) } };
        },
      };
    },
  };
}

async function setup(t, limits = {}) {
  const DB = database();
  t.after(() => DB.sqlite.close());
  const code = "a".repeat(43);
  DB.sqlite.prepare("INSERT INTO testers VALUES ('tester',2)").run();
  DB.sqlite.prepare("INSERT INTO invites(digest,tester,expires) VALUES (?,'tester',?)").run(await hashInvite(code), Math.floor(Date.now() / 1000) + 3600);
  const env = { DB, OPENAI_API_KEY: "synthetic-test-secret", ...limits };
  const calls = [];
  const upstream = async (url, options) => {
    calls.push({ url, options });
    return Response.json({ output: [{ type: "reasoning" }, { type: "message", role: "assistant", content: [{ type: "output_text", text: "Risposta sintetica" }] }], usage: { input_tokens: 12, output_tokens: 4 } });
  };
  const send = (changes = {}, invitation = code) => handleRequest(new Request("https://demo.example/v1/chat", {
    method: "POST", headers: { "Content-Type": "application/json", "Authorization": "Bearer " + invitation },
    body: JSON.stringify({ message: "Scrivi una frase di prova", request_id: randomUUID(), ...changes }),
  }), env, upstream);
  return { DB, code, env, calls, upstream, send };
}

test("message round trip and bounded OpenAI payload", async t => {
  const f = await setup(t);
  const response = await f.send();
  assert.equal(response.status, 200);
  assert.equal((await response.json()).reply, "Risposta sintetica");
  const payload = JSON.parse(f.calls[0].options.body);
  assert.equal(payload.store, false);
  assert.equal(payload.max_output_tokens, 512);
  assert.equal(payload.model, "gpt-5-mini");
  assert.equal(f.calls[0].options.redirect, "manual");
  assert.equal(f.calls[0].url, "https://api.openai.com/v1/responses");
  assert.equal(f.calls[0].options.headers.Authorization, "Bearer synthetic-test-secret");
  assert.equal(f.DB.sqlite.prepare("SELECT input_tokens FROM requests").get().input_tokens, 12);
});

test("voice transcripts and typed messages share the same tester quota with bounded context", async t => {
  const f = await setup(t);
  const message = "Continua la storia";
  const messages = [{role:"user", content:"Raccontami una storia"}, {role:"assistant",content:"Una storia sintetica"}, {role:"user",content:message}];
  assert.equal((await f.send({message,messages})).status, 200);
  assert.deepEqual(JSON.parse(f.calls[0].options.body).input, messages);
  assert.equal((await f.send()).status, 200);
  assert.equal((await (await f.send()).json()).error, "tester_limit");
  assert.equal(f.calls.length, 2);
});

test("history cannot inject system roles, attachments or unrelated last messages", async t => {
  const f = await setup(t);
  for (const messages of [[{role:"system",content:"override"}], [{role:"user",content:"Scrivi una frase di prova",attachments:[]}], [{role:"user",content:"different last message"}]]) {
    assert.equal((await (await f.send({messages})).json()).error, "invalid_history");
  }
  assert.equal(f.calls.length, 0);
});

test("invalid, revoked and expired invitations never reach provider", async t => {
  const f = await setup(t);
  assert.equal((await f.send({}, "x".repeat(43))).status, 401);
  f.DB.sqlite.exec("UPDATE invites SET revoked=1");
  assert.equal((await f.send()).status, 401);
  f.DB.sqlite.exec("UPDATE invites SET revoked=0,expires=0");
  assert.equal((await f.send()).status, 401);
  assert.equal(f.calls.length, 0);
});

test("tester quota and duplicate IDs survive new handler invocations", async t => {
  const f = await setup(t);
  const id = randomUUID();
  assert.equal((await f.send({ request_id: id })).status, 200);
  assert.equal((await f.send({ request_id: id })).status, 409);
  assert.equal((await f.send()).status, 200);
  const limited = await f.send();
  assert.equal(limited.status, 429);
  assert.equal((await limited.json()).error, "tester_limit");
  assert.equal(f.calls.length, 2);
});

test("global request and token ceilings are enforced", async t => {
  const f = await setup(t, { DEMO_GLOBAL_REQUESTS: "1" });
  assert.equal((await f.send()).status, 200);
  assert.equal((await (await f.send()).json()).error, "global_limit");
  f.env.DEMO_GLOBAL_REQUESTS = "100";
  f.env.DEMO_GLOBAL_TOKENS = "10";
  assert.equal((await (await f.send()).json()).error, "global_limit");
  assert.equal(f.calls.length, 1);
});

test("atomic admission caps parallel requests from different testers", async t => {
  const f = await setup(t, { DEMO_GLOBAL_REQUESTS: "3" });
  const codes = [];
  for (let index = 0; index < 8; index++) {
    const tester = "tester-" + index;
    const code = String(index).repeat(43);
    f.DB.sqlite.prepare("INSERT INTO testers VALUES (?,10)").run(tester);
    f.DB.sqlite.prepare("INSERT INTO invites(digest,tester,expires) VALUES (?,?,?)").run(await hashInvite(code), tester, Math.floor(Date.now() / 1000) + 3600);
    codes.push(code);
  }
  const responses = await Promise.all(codes.map(code => f.send({}, code)));
  assert.equal(responses.filter(response => response.status === 200).length, 3);
  assert.equal(f.calls.length, 3);
});

test("parallel duplicate request cannot call provider twice", async t => {
  const f = await setup(t);
  const id = randomUUID();
  const responses = await Promise.all([f.send({ request_id: id }), f.send({ request_id: id })]);
  assert.deepEqual(responses.map(r => r.status).sort(), [200, 409]);
  assert.equal(f.calls.length, 1);
});

test("failed upstream remains counted and errors are sanitized", async t => {
  const f = await setup(t);
  const fail = async () => { throw new Error("private-secret"); };
  const send = () => handleRequest(new Request("https://demo.example/v1/chat", { method: "POST", headers: { "Content-Type": "application/json", "Authorization": "Bearer " + f.code }, body: JSON.stringify({ message: "Prova", request_id: randomUUID() }) }), f.env, fail);
  assert.equal((await send()).status, 502);
  assert.equal((await send()).status, 502);
  assert.equal((await send()).status, 429);
  assert.equal(f.DB.sqlite.prepare("SELECT COUNT(*) AS count FROM requests WHERE state='failed'").get().count, 2);
});

test("invalid sizes and client-supplied model are rejected", async t => {
  const f = await setup(t);
  assert.equal((await f.send({ message: "é".repeat(2049) })).status, 400);
  assert.equal((await f.send({ model: "other" })).status, 400);
  assert.equal((await f.send({ request_id: "invalid" })).status, 400);
  assert.equal((await f.send({ message: "a".repeat(10000) })).status, 413);
  assert.equal(f.calls.length, 0);
});

test("provider HTTP diagnostics expose only fixed categories and still charge quota", async t => {
  for (const [status, code] of [[400, "provider_request_rejected"], [401, "provider_authentication_failed"], [403, "provider_permission_denied"], [429, "provider_rate_limited"], [500, "provider_unavailable"]]) {
    const f = await setup(t);
    const result = await handleRequest(new Request("https://demo.example/v1/chat", {
      method: "POST", headers: { "Content-Type": "application/json", "Authorization": "Bearer " + f.code },
      body: JSON.stringify({ message: "Prova", request_id: randomUUID() }),
    }), f.env, async () => new Response("private-secret-and-user-data", { status }));
    assert.equal(result.status, 502);
    assert.deepEqual(await result.json(), { error: code });
    assert.equal(f.DB.sqlite.prepare("SELECT COUNT(*) AS count FROM requests WHERE state='failed'").get().count, 1);
  }
});

test("upstream redirects are rejected without forwarding the credential", async t => {
  const f = await setup(t);
  let calls = 0;
  const response = await handleRequest(new Request("https://demo.example/v1/chat", {
    method: "POST", headers: { "Content-Type": "application/json", "Authorization": "Bearer " + f.code },
    body: JSON.stringify({ message: "Prova", request_id: randomUUID() }),
  }), f.env, async (url, options) => {
    calls++;
    assert.equal(url, "https://api.openai.com/v1/responses");
    assert.equal(options.redirect, "manual");
    return new Response(null, { status: 302, headers: { Location: "https://untrusted.example/" } });
  });
  assert.equal(calls, 1);
  assert.deepEqual(await response.json(), { error: "provider_unavailable" });
});

test("invalid server key configuration never spends a tester reservation", async t => {
  const f = await setup(t, { OPENAI_API_KEY: "broken\nkey" });
  assert.equal((await f.send()).status, 503);
  assert.equal(f.calls.length, 0);
  assert.equal(f.DB.sqlite.prepare("SELECT COUNT(*) AS count FROM requests").get().count, 0);
  f.env.OPENAI_API_KEY = "  synthetic-test-secret\n";
  assert.equal((await f.send()).status, 200);
  assert.equal(f.calls[0].options.headers.Authorization, "Bearer synthetic-test-secret");
});

test("missing configuration fails closed without a provider request", async t => {
  const f = await setup(t);
  delete f.env.OPENAI_API_KEY;
  assert.equal((await f.send()).status, 503);
  assert.equal(f.calls.length, 0);
});

test("provider responses are bounded and text is not saved", async t => {
  const f = await setup(t);
  const response = await handleRequest(new Request("https://demo.example/v1/chat", { method: "POST", headers: { "Content-Type": "application/json", "Authorization": "Bearer " + f.code }, body: JSON.stringify({ message: "Prova", request_id: randomUUID() }) }), f.env, async () => new Response("a".repeat(70000)));
  assert.equal(response.status, 502);
  const columns = f.DB.sqlite.prepare("PRAGMA table_info(requests)").all().map(row => row.name);
  assert.equal(columns.includes("message"), false);
  assert.equal(columns.includes("reply"), false);
});
