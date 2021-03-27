```bash
kafka-topics --bootstrap-server kafka:9092 --create --topic left-topic --partitions 1 --replication-factor 1; 
kafka-topics --bootstrap-server kafka:9092 --create --topic right-topic --partitions 1 --replication-factor 1;
kafka-topics --bootstrap-server kafka:9092 --create --topic joined-topic --partitions 1 --replication-factor 1;
````

```bash
kafka-topics --bootstrap-server kafka:9092 --delete --topic left-topic;
kafka-topics --bootstrap-server kafka:9092 --delete --topic right-topic;
kafka-topics --bootstrap-server kafka:9092 --delete --topic joined-topic;
```


Write some values into the topics and see the join topic output

```bash
kafkacat \
-b kafka:9092 \
-t left-topic \
-P -K: -Z
```

x:foo
x:bar
y:aaa

```
kafkacat \
-b kafka:9092 \
-t right-topic \
-P -K: -Z
```

x:baz

```
kafkacat \
-b kafka:9092 \
-t joined-topic \
-C -K\\t
% Reached end of topic joined-topic [0] at offset 0
x	[foo, baz]
x	[bar, baz]
Reached end of topic joined-topic [0] at offset 2

```



In the logs:

```bash
10:30:37.712 [join-example-b5b7a634-0050-4dd9-b775-fb573e88e573-StreamThread-1] INFO  com.example.JoinExample - key: x, value: [foo, baz]
10:30:37.715 [join-example-b5b7a634-0050-4dd9-b775-fb573e88e573-StreamThread-1] INFO  com.example.JoinExample - key: x, value: [bar, baz]
```