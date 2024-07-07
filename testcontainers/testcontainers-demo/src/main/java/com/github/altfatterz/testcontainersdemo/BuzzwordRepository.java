package com.github.altfatterz.testcontainersdemo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuzzwordRepository extends JpaRepository<Buzzword, Long> {
}
