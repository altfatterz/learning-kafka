Create Kafka topics:

```bash
kafka-topics \
--create \
--bootstrap-server kafka:9092 \
--replication-factor 1 \
--partitions 1 \
--topic temperatures-topic
```

```bash
kafka-topics \
--create \
--bootstrap-server kafka:9092 \
--replication-factor 1 \
--partitions 1 \
--topic high-temperatures-topic
```


View the created topics

```bash
kafka-topics \
--bootstrap-server kafka:9092 \
--list
```

Populate input Kafka topic:

```bash
cat << EOF | kafka-console-producer \
--broker-list kafka:9092 \
--property "parse.key=true" \
--property "key.separator=:" \
--topic temperatures-topic
"S1":{"station":"S1", "temperature": 10.2, "timestamp": 1}
"S1":{"station":"S1", "temperature": 11.2, "timestamp": 2}
"S1":{"station":"S1", "temperature": 11.1, "timestamp": 3}
"S1":{"station":"S1", "temperature": 12.5, "timestamp": 4}
"S2":{"station":"S2", "temperature": 15.2, "timestamp": 1}
"S2":{"station":"S2", "temperature": 21.7, "timestamp": 2}
"S2":{"station":"S2", "temperature": 25.1, "timestamp": 3}
"S2":{"station":"S2", "temperature": 27.8, "timestamp": 4}
EOF
```

Read the output topic:

```bash
kafka-console-consumer \
--bootstrap-server kafka:9092 \
--from-beginning \
--topic high-temperatures-topic
```

