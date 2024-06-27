package com.github.altfatterz.testcontainersdemo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactRepository extends JpaRepository<Fact, Long> {

    List<Fact> findFactsByValueContainingIgnoreCase(String value);

}
