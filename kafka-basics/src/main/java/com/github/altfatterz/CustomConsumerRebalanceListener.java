package com.github.altfatterz;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CustomConsumerRebalanceListener implements ConsumerRebalanceListener {

    private static final Logger logger = LoggerFactory.getLogger(CustomConsumerRebalanceListener.class);

    private final long startTimestamp;
    private final Consumer<String, String> consumer;

    public CustomConsumerRebalanceListener(Consumer<String, String> consumer, long startTimestamp) {
        this.consumer = consumer;
        this.startTimestamp = startTimestamp;
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        logger.info("onPartitionsAssigned called with: {}", partitions);

        // Build a map key=partition value=startTimestamp
        final Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
        for (TopicPartition partition : partitions) {
            timestampsToSearch.put(partition, startTimestamp);
        }
        logger.info("timestampsToSearch: {}", timestampsToSearch);

        // Request the offsets for the start timestamp
        final Map<TopicPartition, OffsetAndTimestamp> startOffsets = consumer.offsetsForTimes(timestampsToSearch);
        // Seek each partition to the new offset
        for (Map.Entry<TopicPartition, OffsetAndTimestamp> entry : startOffsets.entrySet()) {
            if (entry.getValue() != null) {
                // Print the new offset for each partition
                logger.info("Seeking partition {} to offset {}", entry.getKey().partition(), entry.getValue().offset());
                consumer.seek(entry.getKey(), entry.getValue().offset());
            }
        }
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        // nothing to do
    }

}
