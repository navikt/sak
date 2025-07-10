create table t_k_fagomrade
(
    k_fagomrade     varchar2(20)  not null
        constraint ikfagsystemu
            primary key,
    dekode          varchar2(200) not null,
    dato_fom        date          not null,
    dato_tom        date,
    er_gyldig       char          not null,
    dato_opprettet  timestamp(6)  not null,
    opprettet_av    varchar2(20)  not null,
    dato_endret     timestamp(6)  not null,
    endret_av       varchar2(20)  not null,
    beskrivelse     varchar2(200),
    avlever_med_dok char
);