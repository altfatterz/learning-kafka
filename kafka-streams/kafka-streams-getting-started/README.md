* Topics are created with the `docker-compose.yml`  

```bash
$ kafka-topics bootstrap-server kafka:9092 --topic sentences-topic --create --partitions 1 --replication-factor 1 
$ kafka-topics bootstrap-server kafka:9092 --topic lowercase-sentences-topic --create --partitions 1 --replication-factor 1 
$ kafka-topics bootstrap-server kafka:9092 --topic word-counts-topic --create --partitions 1 --replication-factor 1 
```

* Add data into the input topic

```bash
cat << EOF | kafka-console-producer --broker-list kafka:9092 --topic sentences-topic
"Kafka powers the Confluent Streaming Platform"
"Events are stored in Kafka"
"Confluent contributes to Kafka"
EOF
```

* Start the StatelessStreamProcessingExample

```bash
$ kafka-console-consumer --bootstrap-server kafka:9092 --topic lowercase-sentences-topic --from-beginning 
```

* Start the StatefulStreamProcessingExample

```bash
$ kafka-console-consumer --bootstrap-server kafka:9092 --topic word-counts-topic --from-beginning \
--property print.key=true --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

```bash
$ kafka-consumer-groups --bootstrap-server kafka:9092 --list
$ kafka-consumer-groups --bootstrap-server kafka:9092 --group stateless-kafka-streams-example --describe
```

