package com.github.altfatterz;

import com.github.altfatterz.avro.Account;
import com.github.altfatterz.avro.AccountType;
import com.github.altfatterz.avro.Customer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

public class KafkaAvroProducerDemo {

    static final Logger logger = LoggerFactory.getLogger(KafkaAvroProducerDemo.class);

//    static final String BOOTSTRAP_SERVERS = "localhost:9092";
//    static final String TOPIC = "customer-topic";

    public static void main(String[] args) throws IOException {

        if (args.length != 1) {
            System.out.println("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        // Load producer configuration settings from a local file
        final Properties props = Util.loadConfig(args[0]);

        final String topic = props.getProperty("topic");

//        // common producer properties
//        Properties properties = new Properties();
//        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
//        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//
//        // avro specific configuration
//        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
//        properties.setProperty(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        // producer
        Producer<String, Customer> producer = new KafkaProducer<>(props);

        Customer customer = newCustomer();

        logger.info("Customer: {}", customer);

        // create a producer record
        ProducerRecord<String, Customer> record = new ProducerRecord<>(topic, customer);

        logger.info("send message asynchronously....");
        producer.send(record);

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing producer.");
            producer.close();
        }));
    }

    private static Customer newCustomer() {
        return Customer.newBuilder()
                .setFirstName("John")
                .setLastName("Doe")
                .setAccounts(Arrays.asList(
                        Account.newBuilder()
                                .setIban("CH93 0076 2011 6238 5295 7")
                                .setType(AccountType.CHECKING)
                                .build(),
                        Account.newBuilder()
                                .setIban("CH93 0076 2011 6238 5295 8")
                                .setType(AccountType.SAVING)
                                .build()
                ))
                .setSettings(new HashMap<String, Boolean>() {{
                    put("e-billing-enabled", true);
                    put("push-notification-enabled", false);
                }})
                .setSignupTimestamp(Instant.now())
                .build();
    }

}
