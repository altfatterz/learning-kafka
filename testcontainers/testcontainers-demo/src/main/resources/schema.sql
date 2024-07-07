create table if not exists facts
(
    id    serial primary key,
    value  varchar(1000)  not null
);

create table if not exists buzzwords
(
    id    serial primary key,
    value  varchar(1000)  not null
);

create table if not exists catchphrases
(
    id    serial primary key,
    value  varchar(1000)  not null
);

create table if not exists heros
(
    id    serial primary key,
    value  varchar(1000)  not null
);


