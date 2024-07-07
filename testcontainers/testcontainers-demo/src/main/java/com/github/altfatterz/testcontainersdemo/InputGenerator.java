package com.github.altfatterz.testcontainersdemo;

import com.github.javafaker.Faker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InputGenerator {

    private Faker faker = new Faker();

    private FactRepository factRepository;
    private BuzzwordRepository buzzwordRepository;
    private HeroRepository heroRepository;
    private CatchPhraseRepository catchPhraseRepository;

    public InputGenerator(FactRepository factRepository, BuzzwordRepository buzzwordRepository,
                          HeroRepository heroRepository, CatchPhraseRepository catchPhraseRepository) {
        this.factRepository = factRepository;
        this.buzzwordRepository = buzzwordRepository;
        this.heroRepository = heroRepository;
        this.catchPhraseRepository = catchPhraseRepository;
    }

    @Scheduled(fixedRate = 1000)
    public void generateInput() {
        String fact = faker.chuckNorris().fact();
        factRepository.save(new Fact(fact));

        String buzzword = faker.company().buzzword();
        buzzwordRepository.save(new Buzzword(buzzword));

        String catchPhrase = faker.company().catchPhrase();
        catchPhraseRepository.save(new CatchPhrase(catchPhrase));

        String hero = faker.ancient().hero();
        heroRepository.save(new Hero(hero));
    }

}
