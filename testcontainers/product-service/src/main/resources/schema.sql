create table products
(
    id    serial primary key,
    code  varchar(255)  not null,
    name  varchar(255)  not null,
    price numeric(5, 2) not null,
    UNIQUE (code)
);