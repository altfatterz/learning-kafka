package com.github.altfatterz.testcontainersdemo;

import com.github.javafaker.Faker;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.swing.text.html.parser.Entity;
import java.util.Random;

@Component
public class InputGenerator {

    private Faker faker = new Faker();

    private FactRepository factRepository;
    private BuzzwordRepository buzzwordRepository;
    private HeroRepository heroRepository;
    private CatchPhraseRepository catchPhraseRepository;

    private Random random = new Random();

    public InputGenerator(FactRepository factRepository, BuzzwordRepository buzzwordRepository,
                          HeroRepository heroRepository, CatchPhraseRepository catchPhraseRepository) {
        this.factRepository = factRepository;
        this.buzzwordRepository = buzzwordRepository;
        this.heroRepository = heroRepository;
        this.catchPhraseRepository = catchPhraseRepository;
    }

    @Scheduled(fixedRate = 1000)
    public void insert() {
        String fact = faker.chuckNorris().fact();
        factRepository.save(new Fact(fact));

        String buzzword = faker.company().buzzword();
        buzzwordRepository.save(new Buzzword(buzzword));

        String catchPhrase = faker.company().catchPhrase();
        catchPhraseRepository.save(new CatchPhrase(catchPhrase));

        String hero = faker.ancient().hero();
        heroRepository.save(new Hero(hero));

        long count = factRepository.count();
    }

    @Scheduled(fixedRate = 2000)
    public void delete() {
        factRepository.deleteById(random.nextLong(1, factRepository.count()));
        buzzwordRepository.deleteById(random.nextLong(1, buzzwordRepository.count()));
        catchPhraseRepository.deleteById(random.nextLong(1, catchPhraseRepository.count()));
        heroRepository.deleteById(random.nextLong(1, heroRepository.count()));
    }

    @Scheduled(fixedRate = 2000)
    public void update() {
        factRepository.save(new Fact(random.nextLong(1, factRepository.count()), "update"));
        buzzwordRepository.save(new Buzzword(random.nextLong(1, buzzwordRepository.count()), "update"));
        catchPhraseRepository.save(new CatchPhrase(random.nextLong(1, catchPhraseRepository.count()), "update"));
        heroRepository.save(new Hero(random.nextLong(1, heroRepository.count()), "update"));
    }

}
