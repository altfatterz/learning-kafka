package com.github.altfatterz.testcontainersdemo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FactRestController {

    private FactRepository factRepository;

    public FactRestController(FactRepository factRepository) {
        this.factRepository = factRepository;
    }

    @PostMapping("/facts")
    public void addFact(@RequestBody String fact) {
        factRepository.save(new Fact(fact));
    }

    @GetMapping("/facts")
    public List<Fact> getFacts() {
        return factRepository.findAll();
    }

    @PostMapping("/facts/search")
    public List<Fact> searchFacts(@RequestBody String term) {
        return factRepository.findFactsByValueContainingIgnoreCase(term);
    }
}
