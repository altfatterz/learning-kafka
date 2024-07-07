package com.github.altfatterz.testcontainersdemo;

import com.github.javafaker.Faker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InputGenerator {

    private Faker faker = new Faker();

    private FactRepository factRepository;
    private BuzzwordRepository buzzwordRepository;

    public InputGenerator(FactRepository factRepository, BuzzwordRepository buzzwordRepository) {
        this.factRepository = factRepository;
        this.buzzwordRepository = buzzwordRepository;
    }

    @Scheduled(fixedRate = 1000)
    public void generateInput() {
        String fact = faker.chuckNorris().fact();
        factRepository.save(new Fact(fact));

        String buzzword = faker.company().buzzword();
        buzzwordRepository.save(new Buzzword(buzzword));
    }

}
