create or replace TRIGGER SAK_D_TRG
AFTER DELETE ON SAK

FOR EACH ROW BEGIN
 INSERT INTO sak_gr.sak_gr
 (sak_gr_id, sak_id, tema, applikasjon, fagsaknr, opprettet_av, opprettet_tidspunkt, dato_overfort_grensesnitt, endring_type)
 VALUES (gsak_gr.SAK_SEQ.nextval, :old.id, :old.tema, :old.applikasjon, :old.fagsaknr, :old.opprettet_av, :old.opprettet_tidspunkt, CURRENT_TIMESTAMP, 'D');

 END;

