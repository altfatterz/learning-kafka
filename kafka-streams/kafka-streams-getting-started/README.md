* Topics are created with the `docker-compose.yml`  

```bash
$ kafka-topics \
  --create \
  --bootstrap-server kafka:9092 \
  --replication-factor 1 \
  --partitions 1 \
  --topic sentences-topic
```

```bash
$ kafka-topics \
  --create \
  --bootstrap-server kafka:9092 \
  --replication-factor 1 \
  --partitions 1 \
  --topic lowercase-sentences-topic
```

```bash
$ kafka-topics \
  --create \
  --bootstrap-server kafka:9092 \
  --replication-factor 1 \
  --partitions 1 \
  --topic word-counts-topic
```

* Add data into the input topic

```bash
cat << EOF | kafka-console-producer \
--broker-list kafka:9092 \
--topic sentences-topic
"Kafka powers the Confluent Streaming Platform"
"Events are stored in Kafka"
"Confluent contributes to Kafka"
EOF
```

* Start the StatelessStreamProcessingExample

```bash
$ kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --from-beginning \
  --topic lowercase-sentences-topic
```

* Start the StatefulStreamProcessingExample

```bash
$ kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --from-beginning \
  --topic word-counts-topic
```

```bash
$ kafka-consumer-groups --bootstrap-server kafka:9092 --list
$ kafka-consumer-groups --bootstrap-server kafka:9092 --group lowercase-example --describe
```

