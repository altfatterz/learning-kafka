package com.github.altfatterz.testcontainersdemo;

import jakarta.persistence.*;

@Entity
@Table(name = "catchphrases")
public class CatchPhrase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String value;

    public CatchPhrase() {
    }

    public CatchPhrase(Long id, String value) {
        this.id = id;
        this.value = value;
    }

    public CatchPhrase(String value) {
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

}