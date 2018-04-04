DROP SEQUENCE sak_seq;

DECLARE
    max_id NUMBER;
BEGIN
    SELECT max(id)
    INTO max_id
    FROM sak;
    EXECUTE IMMEDIATE 'create sequence sak_seq start with ' || max_id;
END;
