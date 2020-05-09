package com.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.SubscribableChannel;

@EnableBinding(SampleSink.Sink.class)
public class SampleSink {

    private final Log logger = LogFactory.getLog(getClass());

    @StreamListener(SampleSink.Sink.SAMPLE)
    public void receive(String payload) {
        logger.info("Data received: " + payload);
    }

    interface Sink {
        String SAMPLE = "sample-sink";

        @Input(SAMPLE)
        SubscribableChannel sampleSink();
    }
}
