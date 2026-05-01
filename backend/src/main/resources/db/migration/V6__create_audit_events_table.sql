CREATE TABLE audit_events (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sub         VARCHAR(255) NOT NULL,
  event_type  VARCHAR(50)  NOT NULL,
  before_val  TEXT,
  after_val   TEXT,
  occurred_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
