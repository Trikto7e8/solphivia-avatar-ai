"""Text-only demo gateway. Run behind HTTPS with a persistent SQLite volume."""
import argparse
import asyncio
import hashlib
import http.client
import json
import os
import re
import secrets
import sqlite3
import time
import uuid
from dataclasses import dataclass
from contextlib import contextmanager
from pathlib import Path

MAX_BODY = 8192
MAX_MESSAGE_BYTES = 4096
MAX_REPLY_CHARS = 4000
MAX_OUTPUT_TOKENS = 512
INSTRUCTIONS = (
    "Sei Solphivia IA, una compagna digitale gentile. Rispondi in italiano chiaro "
    "in massimo 120 parole. Questa e' una demo di testo. Non puoi accedere a file, "
    "memorie, telefono, account o conversazioni precedenti. Non dichiarare azioni eseguite."
)


class Rejected(Exception):
    def __init__(self, status, code):
        self.status, self.code = status, code


@dataclass(frozen=True)
class Config:
    database: str
    api_key: str
    model: str = "gpt-5-mini"
    global_requests: int = 100
    global_tokens: int = 200_000

    @classmethod
    def environment(cls):
        config = cls(
            os.environ.get("DEMO_DATABASE", "/data/demo.sqlite3"),
            os.environ.get("OPENAI_API_KEY", ""),
            os.environ.get("OPENAI_MODEL", "gpt-5-mini"),
            int(os.environ.get("DEMO_GLOBAL_REQUESTS", "100")),
            int(os.environ.get("DEMO_GLOBAL_TOKENS", "200000")),
        )
        if not config.api_key or min(config.global_requests, config.global_tokens) <= 0:
            raise ValueError("Configure OPENAI_API_KEY and positive demo limits on the server")
        return config


class Ledger:
    def __init__(self, database):
        self.database = database
        Path(database).parent.mkdir(parents=True, exist_ok=True)
        with self.connect() as db:
            db.executescript("""
                CREATE TABLE IF NOT EXISTS testers (
                    id TEXT PRIMARY KEY, request_limit INTEGER NOT NULL CHECK(request_limit>0));
                CREATE TABLE IF NOT EXISTS invites (
                    digest TEXT PRIMARY KEY, tester TEXT NOT NULL REFERENCES testers(id),
                    expires INTEGER NOT NULL, revoked INTEGER NOT NULL DEFAULT 0);
                CREATE TABLE IF NOT EXISTS requests (
                    id TEXT PRIMARY KEY, tester TEXT NOT NULL REFERENCES testers(id),
                    reserved INTEGER NOT NULL, created INTEGER NOT NULL, state TEXT NOT NULL,
                    input_tokens INTEGER, output_tokens INTEGER);
            """)

    @contextmanager
    def connect(self):
        db = sqlite3.connect(self.database, timeout=10)
        try:
            db.execute("PRAGMA foreign_keys=ON")
            with db:
                yield db
        finally:
            db.close()

    @staticmethod
    def digest(code):
        return hashlib.sha256(code.encode("utf-8")).hexdigest()

    def issue(self, tester, limit, days):
        if not re.fullmatch(r"[a-zA-Z0-9_-]{1,64}", tester) or limit <= 0 or days <= 0:
            raise ValueError("Use a pseudonymous tester ID, positive limit and expiry")
        code = secrets.token_urlsafe(32)
        with self.connect() as db:
            db.execute("INSERT INTO testers VALUES (?,?) ON CONFLICT(id) DO UPDATE SET request_limit=excluded.request_limit", (tester, limit))
            db.execute("INSERT INTO invites(digest,tester,expires) VALUES (?,?,?)", (self.digest(code), tester, int(time.time()) + days * 86400))
        return code

    def revoke(self, tester):
        with self.connect() as db:
            db.execute("UPDATE invites SET revoked=1 WHERE tester=?", (tester,))

    def reserve(self, code, request_id, tokens, config):
        now = int(time.time())
        with self.connect() as db:
            # Serialize admission across threads/processes sharing this database.
            db.execute("BEGIN IMMEDIATE")
            row = db.execute("SELECT i.tester,t.request_limit FROM invites i JOIN testers t ON t.id=i.tester WHERE digest=? AND revoked=0 AND expires>?", (self.digest(code), now)).fetchone()
            if row is None:
                raise Rejected(401, "invalid_invite")
            tester, limit = row
            if db.execute("SELECT 1 FROM requests WHERE id=?", (request_id,)).fetchone():
                raise Rejected(409, "duplicate_request")
            used, reserved = db.execute("SELECT COUNT(*),COALESCE(SUM(reserved),0) FROM requests").fetchone()
            if used >= config.global_requests or reserved + tokens > config.global_tokens:
                raise Rejected(429, "global_limit")
            if db.execute("SELECT COUNT(*) FROM requests WHERE tester=?", (tester,)).fetchone()[0] >= limit:
                raise Rejected(429, "tester_limit")
            if db.execute("SELECT 1 FROM requests WHERE tester=? AND state='pending' AND created>?", (tester, now - 90)).fetchone():
                raise Rejected(429, "request_in_progress")
            db.execute("INSERT INTO requests(id,tester,reserved,created,state) VALUES (?,?,?,?,'pending')", (request_id, tester, tokens, now))

    def finish(self, request_id, state, usage=None):
        usage = usage or {}
        with self.connect() as db:
            db.execute("UPDATE requests SET state=?,input_tokens=?,output_tokens=? WHERE id=?", (state, usage.get("input_tokens"), usage.get("output_tokens"), request_id))
        # Reservations are never refunded. Unknown upstream outcomes remain charged.


def openai_response(config, message):
    payload = json.dumps({
        "model": config.model, "instructions": INSTRUCTIONS, "input": message,
        "max_output_tokens": MAX_OUTPUT_TOKENS, "store": False,
        "reasoning": {"effort": "minimal"},
    }).encode("utf-8")
    connection = http.client.HTTPSConnection("api.openai.com", timeout=30)
    try:
        connection.request("POST", "/v1/responses", payload, {
            "Authorization": "Bearer " + config.api_key,
            "Content-Type": "application/json",
        })
        response = connection.getresponse()
        data = response.read(65537)
        if response.status != 200 or len(data) > 65536:
            raise Rejected(502, "provider_unavailable")
        result = json.loads(data)
        parts = []
        for item in result.get("output", []):
            if item.get("type") != "message" or item.get("role") != "assistant":
                continue
            for content in item.get("content", []):
                if content.get("type") == "output_text":
                    parts.append(content.get("text", ""))
                elif content.get("type") == "refusal":
                    parts.append(content.get("refusal", ""))
        reply = "\n".join(parts).strip()
        if not reply or len(reply) > MAX_REPLY_CHARS:
            raise Rejected(502, "invalid_provider_response")
        return reply, result.get("usage", {})
    finally:
        connection.close()


class DemoApp:
    def __init__(self, config, provider=openai_response):
        self.config, self.provider = config, provider
        self.ledger = Ledger(config.database)

    def chat(self, headers, data):
        if set(data) != {"message", "request_id"}:
            raise Rejected(400, "invalid_request")
        message, request_id = data["message"], data["request_id"]
        if not isinstance(message, str) or not message.strip() or len(message.encode("utf-8")) > MAX_MESSAGE_BYTES:
            raise Rejected(400, "message_size")
        if not isinstance(request_id, str):
            raise Rejected(400, "invalid_request_id")
        try:
            if str(uuid.UUID(request_id)) != request_id:
                raise ValueError()
        except ValueError:
            raise Rejected(400, "invalid_request_id") from None
        auth = headers.get(b"authorization", b"").decode("ascii", errors="replace")
        if not re.fullmatch(r"Bearer [a-zA-Z0-9_-]{32,128}", auth):
            raise Rejected(401, "invalid_invite")
        # UTF-8 byte count plus fixed overhead bounds input tokens conservatively.
        reservation = len(message.encode("utf-8")) + len(INSTRUCTIONS.encode()) + 1024 + MAX_OUTPUT_TOKENS
        self.ledger.reserve(auth[7:], request_id, reservation, self.config)
        try:
            reply, usage = self.provider(self.config, message)
            if not isinstance(reply, str) or not reply.strip() or len(reply) > MAX_REPLY_CHARS:
                raise Rejected(502, "invalid_provider_response")
            self.ledger.finish(request_id, "done", usage)
            return {"reply": reply, "request_id": request_id}
        except Exception:
            self.ledger.finish(request_id, "failed")
            raise Rejected(502, "provider_unavailable") from None

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            return
        status, response = 200, {"status": "ok"}
        try:
            if scope["path"] == "/health" and scope["method"] == "GET":
                pass
            elif scope["path"] != "/v1/chat" or scope["method"] != "POST":
                raise Rejected(404, "not_found")
            else:
                headers = dict(scope["headers"])
                if headers.get(b"content-type", b"").split(b";")[0].strip().lower() != b"application/json":
                    raise Rejected(415, "json_required")
                body = bytearray()
                more = True
                async with asyncio.timeout(10):
                    while more:
                        event = await receive()
                        if event["type"] == "http.disconnect":
                            return
                        body.extend(event.get("body", b""))
                        if len(body) > MAX_BODY:
                            raise Rejected(413, "request_size")
                        more = event.get("more_body", False)
                data = json.loads(body)
                if not isinstance(data, dict):
                    raise Rejected(400, "invalid_request")
                response = await asyncio.to_thread(self.chat, headers, data)
        except Rejected as error:
            status, response = error.status, {"error": error.code}
        except (ValueError, UnicodeError):
            status, response = 400, {"error": "invalid_json"}
        except asyncio.TimeoutError:
            status, response = 408, {"error": "request_timeout"}
        except Exception:
            status, response = 503, {"error": "service_unavailable"}
        payload = json.dumps(response, ensure_ascii=False).encode("utf-8")
        await send({"type": "http.response.start", "status": status, "headers": [
            (b"content-type", b"application/json; charset=utf-8"),
            (b"content-length", str(len(payload)).encode()),
            (b"cache-control", b"no-store"),
        ]})
        await send({"type": "http.response.body", "body": payload})


def create_app():
    return DemoApp(Config.environment())


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Manage demo invitations locally on the server")
    parser.add_argument("action", choices=["issue", "revoke", "usage"])
    parser.add_argument("--tester")
    parser.add_argument("--limit", type=int, default=20)
    parser.add_argument("--days", type=int, default=7)
    args = parser.parse_args()
    ledger = Ledger(os.environ.get("DEMO_DATABASE", "/data/demo.sqlite3"))
    if args.action == "issue":
        print(ledger.issue(args.tester or "", args.limit, args.days))
    elif args.action == "revoke":
        ledger.revoke(args.tester)
        print("Invitations revoked")
    else:
        with ledger.connect() as db:
            print(json.dumps(db.execute("SELECT tester,COUNT(*),SUM(reserved),SUM(input_tokens),SUM(output_tokens) FROM requests GROUP BY tester").fetchall()))
