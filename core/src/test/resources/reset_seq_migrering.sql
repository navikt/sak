DROP SEQUENCE seq_sak;

DECLARE
    max_id NUMBER;
BEGIN
    SELECT max(id)
    INTO max_id
    FROM sak;
    EXECUTE IMMEDIATE 'create sequence seq_sak start with ' || max_id;
END;
