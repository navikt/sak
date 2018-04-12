create TRIGGER SAK_IU_TRG
AFTER INSERT OR UPDATE ON SAK

FOR EACH ROW
DECLARE
 endringstype CHAR(1);
 BEGIN
 IF updating THEN endringstype := 'U'; END IF;
 IF inserting THEN endringstype :='I'; END IF;

 INSERT INTO sak_gr.sak_gr
 (sak_gr_id, sak_id, tema, applikasjon, fagsaknr, opprettet_av, opprettet_tidspunkt, dato_overfort_grensesnitt, endring_type)
 VALUES (sak_gr.SAK_SEQ.nextval, :new.id, :new.tema, :new.applikasjon, :new.fagsaknr, :new.opprettet_av, :new.opprettet_tidspunkt, CURRENT_TIMESTAMP, endringstype);
END;
