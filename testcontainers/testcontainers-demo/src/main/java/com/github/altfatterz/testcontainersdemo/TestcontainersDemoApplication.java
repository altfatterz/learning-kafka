package com.github.altfatterz.testcontainersdemo;

import com.github.javafaker.ChuckNorris;
import com.github.javafaker.Faker;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SpringBootApplication
public class TestcontainersDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestcontainersDemoApplication.class, args);
	}

}



@Repository
interface FactRepository extends JpaRepository<Fact, Long> {}

@Entity
record Fact(@Id Long id, String value) {}





