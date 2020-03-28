package com.example.simpleconsumer;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class Config {

    public static final String TOPIC = "messages";

    public static final String TOPIC_DLT = "messages.DLT";

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    // Spring Boot Apache Kafka Support
    // https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-kafka

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name(TOPIC)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicDLT() {
        return TopicBuilder.name(TOPIC_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    // By default, records that fail are simply logged and we move on to the next one.
    // Here we the SeekToCurrentErrorHandler which performs seek operations on the consumer to reset the offsets
    // so that the discarded records are fetched again on the next poll.
    // By default SeekToCurrentErrorHandler tries 10 times and logs the failed record.
    // Here override the 3 max attempts with 2 second interval and using a recoverer sending it to a dead letter topic.
    @Bean
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory,
            KafkaTemplate<?, ?> kafkaTemplate) {

        // KafkaMessageListenerContainer receives all message from all topics or partitions on a single thread.
        // ConcurrentKafkaListenerContainer delegates to one or more KafkaMessageListenerContainer instances to
        // provide multi-threaded consumption

        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setErrorHandler(new SeekToCurrentErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(2000, 3)
        ));
        return factory;
    }


    //  It will be invoked before calling the listener allowing inspection or modification of the record.
    //  If the interceptor returns null, the listener is not called.
    @Bean
    public RecordInterceptor recordInterceptor() {
        return consumerRecord -> {
            logger.info("Key: {}, Value: {}", consumerRecord.key(), consumerRecord.value());
            logger.info("Topic: {}, Partition: {}, Offset: {}", consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
            return consumerRecord;
        };
    }

}
