-- Notificacoes no Azure SQL / SQL Server (schema notificacoes, criado pelo bootstrap do admin).
-- Recomeco limpo em T-SQL: substitui as migracoes V1..V2 do PostgreSQL.

CREATE TABLE notificacoes.email_capturado (
  evento_id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_email_capturado PRIMARY KEY,
  abastecimento_id UNIQUEIDENTIFIER NOT NULL,
  assunto NVARCHAR(160) NOT NULL,
  corpo NVARCHAR(MAX) NOT NULL,
  status NVARCHAR(30) NOT NULL
    CONSTRAINT ck_email_capturado_status
    CHECK (status IN ('CAPTURADO_LOCALMENTE', 'PENDENTE', 'ENVIADO')),
  solicitado_em DATETIMEOFFSET(6) NOT NULL,
  destinatario NVARCHAR(320) NULL,
  provedor NVARCHAR(60) NULL,
  provedor_mensagem_id NVARCHAR(200) NULL,
  enviado_em DATETIMEOFFSET(6) NULL,
  erro_ultimo NVARCHAR(500) NULL
);
GO

CREATE INDEX ix_email_capturado_solicitado_em
  ON notificacoes.email_capturado (solicitado_em DESC);
GO

CREATE INDEX ix_email_capturado_status
  ON notificacoes.email_capturado (status, solicitado_em);
GO
