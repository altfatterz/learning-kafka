package com.github.altfatterz.joinexamples;

import org.apache.kafka.streams.kstream.ValueJoinerWithKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StreamToStreamValueJoiner implements ValueJoinerWithKey<String, String, String, String> {

    private static final Logger logger = LoggerFactory.getLogger(StreamToStreamValueJoiner.class);

    @Override
    public String apply(String readOnlyKey, String impressionValue, String clickValue) {
        logger.info("inside value joiner with key: " + readOnlyKey + "clickValue:" + clickValue + "impressionValue:" + impressionValue);
        return clickValue == null ? impressionValue + "/not-clicked-yet" : impressionValue + "/" + clickValue;
    }

}
