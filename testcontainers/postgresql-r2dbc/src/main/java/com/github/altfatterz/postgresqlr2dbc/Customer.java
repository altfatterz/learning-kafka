package com.github.altfatterz.postgresqlr2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Table(name = "customers")
public record Customer(@Id Long id, String firstname, String lastname) {

    boolean hasId() {
        return id != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Customer customer)) {
            return false;
        }
        return Objects.equals(firstname, customer.firstname) && Objects.equals(lastname, customer.lastname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstname, lastname);
    }
}
