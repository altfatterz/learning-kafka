package com.github.altfatterz.postgresqlr2dbc;

import org.springframework.boot.SpringApplication;

public class TestPostgresqlR2dbcApplication {

    public static void main(String[] args) {
        SpringApplication.from(PostgresqlR2dbcApplication::main)
                .with(TestPostgresqlR2dbcApplicationConfig.class).run(args);
    }

}
