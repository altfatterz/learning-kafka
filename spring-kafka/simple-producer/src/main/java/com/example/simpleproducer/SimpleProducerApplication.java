package com.example.simpleproducer;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

@SpringBootApplication
public class SimpleProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleProducerApplication.class, args);
    }

}

@RestController
class Producer {

    private static final Logger logger = LoggerFactory.getLogger(Producer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public Producer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/messages")
    public void sendMessage(@RequestBody String message) {
        String payload = message.trim();

        logger.info("Sending payload {}", payload);

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(Config.TOPIC, payload);

        future.whenComplete((result, throwable) -> {
            if (result != null) {
                RecordMetadata recordMetadata = result.getRecordMetadata();
                logger.info("success, topic: {}, partition: {}, offset: {}",
                        recordMetadata.topic(),
                        recordMetadata.partition(),
                        recordMetadata.offset());
            } else {
                logger.info("error occurred:" + throwable);
            }
        });
    }

    @PostMapping("/many-messages")
    public void multipleMessages(@RequestBody Integer nr) {

        logger.info("Sending {} nr. of messages", nr);

        IntStream.range(0, nr).forEach(value -> {

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(Config.TOPIC, "message_" + value);

            future.whenComplete((result, throwable) -> {
                if (result != null) {
                    RecordMetadata recordMetadata = result.getRecordMetadata();
                    logger.info("success, topic: {}, partition: {}, offset: {}",
                            recordMetadata.topic(),
                            recordMetadata.partition(),
                            recordMetadata.offset());
                } else {
                    logger.info("error occurred:" + throwable);
                }
            });

        });
    }

    @PostMapping("/partition")
    public void sendToPartition(@RequestBody int partition) throws ExecutionException, InterruptedException {

        logger.info("Sending message to partition {}", partition);

        SendResult<String, String> result = kafkaTemplate.sendDefault(partition, null, "Hello World").get();
        RecordMetadata recordMetadata = result.getRecordMetadata();

        logger.info("success, topic: {}, partition: {}, offset: {}",
                recordMetadata.topic(),
                recordMetadata.partition(),
                recordMetadata.offset());
    }
}