package com.github.altfatterz;

import com.github.altfatterz.types.Dimensions;
import com.github.altfatterz.types.Product;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class KafkaJsonSchemaProducerDemo {

    static final Logger logger = LoggerFactory.getLogger(KafkaJsonSchemaProducerDemo.class);

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length != 1) {
            logger.info("Please provide the configuration file path as a command line argument");
            System.exit(1);
        }

        // Load producer configuration settings from a local file
        final Properties props = Util.loadConfig(args[0]);

        final String topic = props.getProperty("topic");

        // producer
        Producer<String, Product> producer = new KafkaProducer<>(props);

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Closing producer.");
            producer.close();
        }));

        Product product = newProduct();

        logger.info("address: {}", product);

        // create a producer record
        ProducerRecord<String, Product> record = new ProducerRecord<>(topic, "productKey1", product);

        logger.info("send message asynchronously....");
        producer.send(record);

        Thread.sleep(5000);
    }

    private static Product newProduct() {
        Product product = new Product();
        product.setProductId(1);
        product.setProductName("bicycle");
        product.setPrice(25.50);
        Dimensions dimensions = new Dimensions();
        dimensions.setHeight(58.51);
        dimensions.setWidth(20.15);
        dimensions.setLength(150.40);
        product.setDimensions(dimensions);
        return product;
    }


}
