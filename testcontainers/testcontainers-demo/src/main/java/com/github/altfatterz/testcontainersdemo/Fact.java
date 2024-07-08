package com.github.altfatterz.testcontainersdemo;

import jakarta.persistence.*;

@Entity
@Table(name = "facts")
public class Fact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String value;

    public Fact() {
    }

    public Fact(Long id, String value) {
        this.id = id;
        this.value = value;
    }

    public Fact(String value) {
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

}
