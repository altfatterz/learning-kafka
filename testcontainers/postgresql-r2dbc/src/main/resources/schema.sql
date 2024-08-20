create table if not exists facts
(
    id    serial primary key,
    value  varchar(1000)  not null
);