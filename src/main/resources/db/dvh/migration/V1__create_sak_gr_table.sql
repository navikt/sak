CREATE TABLE sak_gr (
    sak_gr_id                  NUMBER(38)    NOT NULL,
    sak_id                     NUMBER(10),
    aktoerid                   VARCHAR2(40),
    orgnr                      VARCHAR2(9),
    tema                       VARCHAR2(40),
    applikasjon                VARCHAR2(40),
    fagsaknr                   VARCHAR2(40),
    opprettet_av               VARCHAR2(40),
    opprettet_tidspunkt        TIMESTAMP,
    dato_overfort_grensesnitt  TIMESTAMP     NOT NULL,
    endring_type               CHAR(1)       NOT NULL,

    CONSTRAINT pk_sak_gr PRIMARY KEY (sak_gr_id)
);

CREATE SEQUENCE SAK_SEQ;

