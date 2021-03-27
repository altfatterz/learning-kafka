```bash
$ kafka-topics \
  --create \
  --bootstrap-server kafka:9092 \
  --replication-factor 1 \
  --partitions 1 \
  --topic lines-topic
```

```bash
$ kafka-topics \
  --create \
  --bootstrap-server kafka:9092 \
  --replication-factor 1 \
  --partitions 1 \
  --topic lines-lower-topic
```

```bash
cat << EOF | kafka-console-producer \
--broker-list kafka:9092 \
--property "parse.key=true" \
--property "key.separator=:" \
--topic lines-topic
1:"Kafka powers the Confluent Streaming Platform"
2:"Events are stored in Kafka"
3:"Confluent contributes to Kafka"
EOF
```

```bash
$ kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --from-beginning \
  --topic lines-lower-topic
```

```bash
$ kafka-consumer-grpups --bootstrap-server kafka:9092 --list
$ kafka-consumer-groups --bootstrap-server kafka:9092 --group lowercase-example --describe
```