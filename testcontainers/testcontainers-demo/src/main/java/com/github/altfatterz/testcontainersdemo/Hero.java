package com.github.altfatterz.testcontainersdemo;

import jakarta.persistence.*;

@Entity
@Table(name = "heros")
public class Hero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String value;

    public Hero() {
    }

    public Hero(String value) {
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

}