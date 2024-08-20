package com.github.altfatterz.postgresqlr2dbc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;

@SpringBootApplication
public class PostgresqlR2dbcApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostgresqlR2dbcApplication.class, args);
    }

}
