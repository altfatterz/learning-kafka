package com.github.altfatterz.testcontainersdemo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatchPhraseRepository extends JpaRepository<CatchPhrase, Long> {
}
