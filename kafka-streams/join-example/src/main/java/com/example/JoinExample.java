package com.example;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;


public class JoinExample {

	private final static String APPLICATION_ID = "join-example";

	private final static Logger logger = LoggerFactory.getLogger(JoinExample.class);

	public static void main(String[] args) {
		logger.info("*** Starting {} Application ***", APPLICATION_ID);

		Properties config = getConfig();
		Topology topology = getTopology();
		KafkaStreams streams =  startApp(config, topology);

		setupShutdownHook(streams);
	}

	private static Topology getTopology(){
		StreamsBuilder builder = new StreamsBuilder();

		final Serde<String> stringSerde = Serdes.String();

		KStream<String, String> leftStream = builder.stream("left-topic",
				Consumed.with(stringSerde, stringSerde));
		KStream<String, String> rightStream = builder.stream("right-topic",
				Consumed.with(stringSerde, stringSerde));

		leftStream
//				.join(rightStream,
//				.leftJoin(rightStream,
				.outerJoin(rightStream,
						(leftValue, rightValue) -> "[" + leftValue + ", " + rightValue + "]",
						JoinWindows.of(Duration.ofMinutes(5)),
						Joined.with(stringSerde, stringSerde, stringSerde)
				)
				.to("joined-topic", Produced.with(stringSerde, stringSerde));

		Topology topology = builder.build();
		logger.info(topology.describe().toString());

		return topology;
	}

	private static Properties getConfig(){
		Properties settings = new Properties();
		settings.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
		settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
		return settings;
	}

	private static KafkaStreams startApp(Properties config, Topology topology){
		KafkaStreams streams = new KafkaStreams(topology, config);
		streams.start();
		return streams;
	}

	private static void setupShutdownHook(KafkaStreams streams){
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("### Stopping {} application ###", APPLICATION_ID);
			streams.close();
		}));
	}

}
