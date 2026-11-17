CREATE TABLE IF NOT EXISTS testers (
  id TEXT PRIMARY KEY,
  request_limit INTEGER NOT NULL CHECK(request_limit > 0)
);
CREATE TABLE IF NOT EXISTS invites (
  digest TEXT PRIMARY KEY,
  tester TEXT NOT NULL REFERENCES testers(id),
  expires INTEGER NOT NULL,
  revoked INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS requests (
  id TEXT PRIMARY KEY,
  tester TEXT NOT NULL REFERENCES testers(id),
  reserved INTEGER NOT NULL,
  created INTEGER NOT NULL,
  state TEXT NOT NULL,
  input_tokens INTEGER,
  output_tokens INTEGER
);
CREATE INDEX IF NOT EXISTS requests_tester ON requests(tester);
