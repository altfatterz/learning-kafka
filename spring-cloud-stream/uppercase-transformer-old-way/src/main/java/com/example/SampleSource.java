package com.example;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.concurrent.atomic.AtomicBoolean;

@EnableBinding(SampleSource.Source.class)
public class SampleSource {

    private AtomicBoolean semaphore = new AtomicBoolean(true);

    @Bean
    @InboundChannelAdapter(value = SampleSource.Source.SAMPLE, poller = @Poller(fixedDelay = "1000", maxMessagesPerPoll = "1"))
    public MessageSource<String> sendTestData() {
        return () -> {
            String payload = this.semaphore.getAndSet(!this.semaphore.get()) ? "foo" : "bar";
            return MessageBuilder.withPayload(payload).build();
        };
    }

    interface Source {
        String SAMPLE = "sample-source";

        @Output(SAMPLE)
        MessageChannel sampleSource();
    }
}
