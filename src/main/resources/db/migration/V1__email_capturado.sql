CREATE SCHEMA IF NOT EXISTS notificacoes;

CREATE TABLE notificacoes.email_capturado (
  evento_id UUID PRIMARY KEY,
  abastecimento_id UUID NOT NULL,
  assunto VARCHAR(160) NOT NULL,
  corpo TEXT NOT NULL,
  status VARCHAR(30) NOT NULL CHECK (status = 'CAPTURADO_LOCALMENTE'),
  solicitado_em TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_email_capturado_solicitado_em
  ON notificacoes.email_capturado (solicitado_em DESC);
