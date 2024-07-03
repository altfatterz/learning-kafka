package com.github.altfatterz.joinexamples;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConfigurationProperties("topics.stream-to-stream")
public class StreamToStreamJoinConfig {

    private String input1;
    private String input2;
    private String output;
    private Integer windowSizeInSeconds;

    // Spring Boot Apache Kafka Support
    // https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-kafka

    @Bean
    public NewTopic streamToStreamInput1() {
        return TopicBuilder.name(input1).build();
    }

    @Bean
    public NewTopic streamToStreamInput2() {
        return TopicBuilder.name(input2).build();
    }

    @Bean
    public NewTopic streamToStreamOutput() {
        return TopicBuilder.name(output).build();
    }

    public String getInput1() {
        return input1;
    }

    public void setInput1(String input1) {
        this.input1 = input1;
    }

    public String getInput2() {
        return input2;
    }

    public void setInput2(String input2) {
        this.input2 = input2;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Integer getWindowSizeInSeconds() {
        return windowSizeInSeconds;
    }

    public void setWindowSizeInSeconds(Integer windowSizeInSeconds) {
        this.windowSizeInSeconds = windowSizeInSeconds;
    }
}