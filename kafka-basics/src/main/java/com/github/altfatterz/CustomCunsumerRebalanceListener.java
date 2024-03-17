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

public class CustomCunsumerRebalanceListener implements ConsumerRebalanceListener {

    private static final Logger logger = LoggerFactory.getLogger(CustomCunsumerRebalanceListener.class);

    private long startTimestamp;
    private Consumer<String, String> consumer;

    public CustomCunsumerRebalanceListener(Consumer consumer, long startTimestamp) {
        this.consumer = consumer;
        this.startTimestamp = startTimestamp;
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {

        // Build a map key=partition value=startTimestamp
        final Map<TopicPartition, Long> timestampsToSearch = new HashMap<>();
        for (TopicPartition partition : partitions) {
            timestampsToSearch.put(partition, startTimestamp);
        }

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
