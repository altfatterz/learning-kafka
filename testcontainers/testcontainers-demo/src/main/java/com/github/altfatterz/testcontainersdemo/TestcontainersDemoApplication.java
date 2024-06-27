package com.github.altfatterz.testcontainersdemo;

import jakarta.persistence.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SpringBootApplication
public class TestcontainersDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestcontainersDemoApplication.class, args);
    }

}

@RestController
class FactRestController {

    private FactRepository factRepository;

    public FactRestController(FactRepository factRepository) {
        this.factRepository = factRepository;
    }

    @PostMapping("/facts")
    void addFact(@RequestBody String fact) {
        factRepository.save(new Fact(fact));
    }

    @GetMapping("/facts")
    List<Fact> getFacts() {
        return factRepository.findAll();
    }
}

@Repository
interface FactRepository extends JpaRepository<Fact, Long> {
}

@Entity
@Table(name = "facts")
class Fact {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String value;

    public Fact() {}

    public Fact(String value) {
        this.value = value;
    }

    public Long getId() { return id; }
    public String getValue() { return value; }

}





