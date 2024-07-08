package com.github.altfatterz.testcontainersdemo;

import jakarta.persistence.*;

@Entity
@Table(name = "buzzwords")
public class Buzzword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String value;

    public Buzzword() {
    }

    public Buzzword(Long id, String value) {
        this.id = id;
        this.value = value;
    }

    public Buzzword(String value) {
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

}