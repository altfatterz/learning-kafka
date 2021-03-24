See the created topics in the `docker-compose.yaml`

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

