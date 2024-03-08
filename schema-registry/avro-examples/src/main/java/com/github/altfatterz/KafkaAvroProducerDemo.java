package com.github.altfatterz;

import com.github.altfatterz.avro.Account;
import com.github.altfatterz.avro.AccountType;
import com.github.altfatterz.avro.NewCustomerCreatedEvent;
import com.github.javafaker.Faker;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

public class KafkaAvroProducerDemo {

    static final Logger logger = LoggerFactory.getLogger(KafkaAvroProducerDemo.class);

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length != 1) {
            logger.info("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        // Load producer configuration settings from a local file
        final Properties props = Util.loadConfig(args[0]);

        final String topic = props.getProperty("topic");

        // producer
        Producer<String, NewCustomerCreatedEvent> producer = new KafkaProducer<>(props);

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing producer.");
            producer.close();
        }));

        Faker faker = new Faker();

        while (true) {
            NewCustomerCreatedEvent event = newCustomer(faker);

            ProducerRecord<String, NewCustomerCreatedEvent> record = new ProducerRecord<>(topic, event);
            producer.send(record);

            Thread.sleep(100);
        }

    }

    private static NewCustomerCreatedEvent newCustomer(Faker faker) {
        return NewCustomerCreatedEvent.newBuilder()
                .setFirstName(faker.name().firstName())
                .setLastName(faker.name().lastName())
                .setAccounts(Arrays.asList(
                        Account.newBuilder()
                                .setIban(faker.finance().iban())
                                .setType(AccountType.CHECKING)
                                .build(),
                        Account.newBuilder()
                                .setIban(faker.finance().iban())
                                .setType(AccountType.SAVING)
                                .build()
                ))
                .setSettings(new HashMap<>() {{
                    put("e-billing-enabled", faker.bool().bool());
                    put("push-notification-enabled", faker.bool().bool());
                }})
                .setSignupTimestamp(Instant.now())
                .build();
    }

}
