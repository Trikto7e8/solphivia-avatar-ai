import asyncio
from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
import tempfile
import threading
import unittest
from unittest.mock import patch
import uuid

from server import Config, DemoApp, Ledger, Rejected, MAX_MESSAGE_BYTES


class DemoTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.config = Config(str(Path(self.temp.name) / "demo.sqlite3"), "synthetic-server-secret", global_requests=3)
        self.calls = []

        def provider(config, text):
            self.calls.append(text)
            return "Risposta di prova", {"input_tokens": 12, "output_tokens": 4}

        self.app = DemoApp(self.config, provider)
        self.code = self.app.ledger.issue("tester-one", 2, 7)

    def tearDown(self):
        self.temp.cleanup()

    def request(self, code=None, **overrides):
        body = {"message": "Rispondi alla prova", "request_id": str(uuid.uuid4())}
        body.update(overrides)
        return self.app.chat({b"authorization": ("Bearer " + (code or self.code)).encode()}, body)

    def rejected(self, expected, operation):
        with self.assertRaises(Rejected) as result:
            operation()
        self.assertEqual(expected, result.exception.code)

    def test_answer_and_usage_without_accounts(self):
        self.assertEqual("Risposta di prova", self.request()["reply"])
        with self.app.ledger.connect() as db:
            row = db.execute("SELECT input_tokens,output_tokens,state FROM requests").fetchone()
        self.assertEqual((12, 4, "done"), row)

    def test_invalid_revoked_expired_never_call_provider(self):
        self.rejected("invalid_invite", lambda: self.request("x" * 43))
        self.app.ledger.revoke("tester-one")
        self.rejected("invalid_invite", self.request)
        expired = self.app.ledger.issue("expired", 2, 7)
        with self.app.ledger.connect() as db:
            db.execute("UPDATE invites SET expires=0 WHERE tester='expired'")
        self.rejected("invalid_invite", lambda: self.request(expired))
        self.assertFalse(self.calls)

    def test_tester_and_global_limits_survive_restart_and_rotation(self):
        self.request()
        rotated = self.app.ledger.issue("tester-one", 2, 7)
        self.request(rotated)
        self.app = DemoApp(self.config, self.app.provider)
        self.rejected("tester_limit", lambda: self.request(rotated))
        other = self.app.ledger.issue("tester-two", 5, 7)
        self.request(other)
        self.rejected("global_limit", lambda: self.request(other))
        self.assertEqual(3, len(self.calls))

    def test_duplicate_request_is_not_billed_again(self):
        request_id = str(uuid.uuid4())
        self.request(request_id=request_id)
        self.rejected("duplicate_request", lambda: self.request(request_id=request_id))
        self.assertEqual(1, len(self.calls))

    def test_size_and_extra_fields_rejected_before_upstream(self):
        self.rejected("message_size", lambda: self.request(message="a" * (MAX_MESSAGE_BYTES + 1)))
        self.rejected("message_size", lambda: self.request(message="é" * 3000))
        self.rejected("invalid_request", lambda: self.request(model="anything"))
        self.rejected("invalid_request_id", lambda: self.request(request_id="invalid"))
        self.assertFalse(self.calls)

    def test_token_budget_is_enforced_before_upstream(self):
        self.app.config = Config(self.config.database, "synthetic", global_tokens=10)
        self.rejected("global_limit", self.request)
        self.assertFalse(self.calls)

    def test_failed_upstream_remains_counted_and_no_secrets_leak(self):
        def failure(*args):
            raise RuntimeError("private-provider-secret")
        self.app.provider = failure
        self.rejected("provider_unavailable", self.request)
        self.rejected("provider_unavailable", self.request)
        self.rejected("tester_limit", self.request)

    def test_concurrent_admission_never_exceeds_global_limit(self):
        gate = threading.Barrier(8)
        codes = [self.app.ledger.issue("tester-" + str(i), 10, 7) for i in range(8)]

        def run(code):
            gate.wait()
            try:
                self.request(code)
                return True
            except Rejected:
                return False

        with ThreadPoolExecutor(max_workers=8) as pool:
            self.assertEqual(3, sum(pool.map(run, codes)))
        self.assertEqual(3, len(self.calls))

    def test_requests_and_replies_are_not_saved_in_ledger(self):
        self.request()
        with self.app.ledger.connect() as db:
            schema = db.execute("PRAGMA table_info(requests)").fetchall()
        self.assertEqual({"id", "tester", "reserved", "created", "state", "input_tokens", "output_tokens"}, {row[1] for row in schema})

    def test_openai_payload_is_bounded_and_uses_project_key_only_on_server(self):
        from server import openai_response, MAX_OUTPUT_TOKENS
        with patch("server.http.client.HTTPSConnection") as connection:
            response = connection.return_value.getresponse.return_value
            response.status = 200
            response.read.return_value = json.dumps({"output": [{"type": "reasoning"}, {"type": "message", "role": "assistant", "content": [{"type": "output_text", "text": "Ciao"}]}], "usage": {}}).encode()
            self.assertEqual("Ciao", openai_response(self.config, "Prova")[0])
            args = connection.return_value.request.call_args.args
            payload = json.loads(args[2])
            self.assertFalse(payload["store"])
            self.assertEqual(MAX_OUTPUT_TOKENS, payload["max_output_tokens"])
            self.assertEqual("Bearer synthetic-server-secret", args[3]["Authorization"])
            self.assertNotIn("tools", payload)

    def asgi(self, chunks, content_type=b"application/json"):
        events = iter(chunks)
        sent = []

        async def receive():
            return next(events)

        async def send(event):
            sent.append(event)

        asyncio.run(self.app({"type": "http", "path": "/v1/chat", "method": "POST", "headers": [(b"content-type", content_type), (b"authorization", ("Bearer " + self.code).encode())]}, receive, send))
        return sent

    def test_http_valid_request_and_malformed_json(self):
        body = json.dumps({"message": "Prova", "request_id": str(uuid.uuid4())}).encode()
        events = self.asgi([{"type": "http.request", "body": body}])
        self.assertEqual(200, events[0]["status"])
        self.assertEqual("Risposta di prova", json.loads(events[1]["body"])["reply"])
        self.assertEqual(400, self.asgi([{"type": "http.request", "body": b"{"}])[0]["status"])

    def test_chunked_oversized_body_and_wrong_content_type(self):
        events = [{"type": "http.request", "body": b"a" * 5000, "more_body": True}, {"type": "http.request", "body": b"b" * 5000}]
        self.assertEqual(413, self.asgi(events)[0]["status"])
        self.assertEqual(415, self.asgi([], b"text/plain")[0]["status"])
        self.assertFalse(self.calls)


if __name__ == "__main__":
    unittest.main()
