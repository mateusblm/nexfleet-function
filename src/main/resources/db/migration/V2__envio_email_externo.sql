ALTER TABLE notificacoes.email_capturado
  DROP CONSTRAINT email_capturado_status_check;

ALTER TABLE notificacoes.email_capturado
  ADD COLUMN destinatario VARCHAR(320),
  ADD COLUMN provedor VARCHAR(60),
  ADD COLUMN provedor_mensagem_id VARCHAR(200),
  ADD COLUMN enviado_em TIMESTAMPTZ,
  ADD COLUMN erro_ultimo VARCHAR(500);

ALTER TABLE notificacoes.email_capturado
  ADD CONSTRAINT email_capturado_status_check
  CHECK (status IN ('CAPTURADO_LOCALMENTE', 'PENDENTE', 'ENVIADO'));

CREATE INDEX ix_email_capturado_status
  ON notificacoes.email_capturado (status, solicitado_em);
