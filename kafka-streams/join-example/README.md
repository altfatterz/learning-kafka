Create the topics:

```bash
kafka-topics \
--create \
--bootstrap-server kafka:9092 \
--replication-factor 1 \
--partitions 1 \
--topic left-topic
```

```bash
kafka-topics \
--create \
--bootstrap-server kafka:9092 \
--replication-factor 1 \
--partitions 1 \
--topic right-topic
```

```bash
kafka-topics \
--create \
--bootstrap-server kafka:9092 \
--replication-factor 1 \
--partitions 1 \
--topic joined-topic
```

Write some values into the topics and see the join topic output

```bash
kafkacat \
-b kafka:9092 \
-t left-topic \
-P -K: -Z
```

1: foo
2: bar

```
kafkacat \
-b kafka:9092 \
-t right-topic \
-P -K: -Z
```

1: foo2
2: bar2

```
kafkacat \
-b kafka:9092 \
-t joined-topic \
-C -K\\t
```

