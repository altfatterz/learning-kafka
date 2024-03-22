Start up the cluster:

```bash
$ docker compose up -d
```

Create the topics:

```bash
$ docker compose exec -it tools bash
$ kafka-topics --bootstrap-server broker:9092 --delete --topic stateless-demo-input-topic
$ kafka-topics --bootstrap-server broker:9092 --delete --topic stateless-demo-output-topic
$ kafka-topics --bootstrap-server broker:9092 --delete --topic stateful-demo-input-topic
$ kafka-topics --bootstrap-server broker:9092 --delete --topic stateful-demo-output-topic 
$ kafka-topics --bootstrap-server broker:9092 --topic stateless-demo-input-topic --create --partitions 3 --replication-factor 1
$ kafka-topics --bootstrap-server broker:9092 --topic stateless-demo-output-topic --create --partitions 1 --replication-factor 1
$ kafka-topics --bootstrap-server broker:9092 --topic stateful-demo-input-topic --create --partitions 3 --replication-factor 1 
$ kafka-topics --bootstrap-server broker:9092 --topic stateful-demo-output-topic --create --partitions 1 --replication-factor 1 
```

* Add data into the input topic

```bash
$ docker compose exec -it tools bash
$ kafka-console-producer --bootstrap-server broker:9092 --topic stateless-demo-input-topic
```

* Stream the output content:

```bash
$ docker compose exec -it tools bash
$ kafka-console-consumer --bootstrap-server broker:9092 --topic stateless-demo-output-topic --from-beginning --property print.key=true 
```

* Start the StatelessStreamProcessingExample

Check the logs:
```bash
   Sub-topology: 0
    Source: KSTREAM-SOURCE-0000000000 (topics: [sentences-topic])
      --> KSTREAM-MAPVALUES-0000000001
    Processor: KSTREAM-MAPVALUES-0000000001 (stores: [])
      --> KSTREAM-SINK-0000000002
      <-- KSTREAM-SOURCE-0000000000
    Sink: KSTREAM-SINK-0000000002 (topic: lowercase-sentences-topic)
      <-- KSTREAM-MAPVALUES-0000000001
...
	Assigned partitions:                       [stateless-demo-input-topic-0, stateless-demo-input-topic-1,stateless-demo-input-topic-2]
	Current owned partitions:                  []
	Added partitions (assigned - owned):       [stateless-demo-input-topic-0, stateless-demo-input-topic-1, stateless-demo-input-topic-2]
	Revoked partitions (owned - assigned):     []
...
	New active tasks: [0_2, 0_1, 0_0]
	New standby tasks: []
	Existing active tasks: []
	Existing standby tasks: []
```

Start another instance and check consumer groups:

```bash
$ docker compose exec -it tools bash
$ kafka-consumer-groups --bootstrap-server broker:9092 --group stateless-kafka-streams-example --describe

GROUP                           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                                                                                                       HOST            CLIENT-ID
stateless-kafka-streams-example sentences-topic 0          1               1               0               stateless-kafka-streams-example-d041c09d-a358-422a-9895-052fcadb17af-StreamThread-1-consumer-118564b8-68d8-41db-af87-c316959ef7d1 /192.168.65.1   stateless-kafka-streams-example-d041c09d-a358-422a-9895-052fcadb17af-StreamThread-1-consumer
stateless-kafka-streams-example sentences-topic 2          -               0               -               stateless-kafka-streams-example-d041c09d-a358-422a-9895-052fcadb17af-StreamThread-1-consumer-118564b8-68d8-41db-af87-c316959ef7d1 /192.168.65.1   stateless-kafka-streams-example-d041c09d-a358-422a-9895-052fcadb17af-StreamThread-1-consumer
stateless-kafka-streams-example sentences-topic 1          -               0               -               stateless-kafka-streams-example-66358719-fe06-48a8-be53-4d9b0977e176-StreamThread-1-consumer-94c7c917-0138-4803-b0f9-7087ba9024eb /192.168.65.1   stateless-kafka-streams-example-66358719-fe06-48a8-be53-4d9b0977e176-StreamThread-1-consumer
```

Stop the instances, modify this line and check consumer groups

```bash
// settings.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
```

```bash

GROUP                           TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                                                                                                       HOST            CLIENT-ID
stateless-kafka-streams-example sentences-topic 0          1               1               0               stateless-kafka-streams-example-103d456a-cd0c-49d5-b851-9208c5805f03-StreamThread-1-consumer-6b4b65e7-6ed9-4fc2-a276-031fa0db353b /192.168.65.1   stateless-kafka-streams-example-103d456a-cd0c-49d5-b851-9208c5805f03-StreamThread-1-consumer
stateless-kafka-streams-example sentences-topic 2          -               0               -               stateless-kafka-streams-example-103d456a-cd0c-49d5-b851-9208c5805f03-StreamThread-1-consumer-6b4b65e7-6ed9-4fc2-a276-031fa0db353b /192.168.65.1   stateless-kafka-streams-example-103d456a-cd0c-49d5-b851-9208c5805f03-StreamThread-1-consumer
stateless-kafka-streams-example sentences-topic 1          -               0               -               stateless-kafka-streams-example-103d456a-cd0c-49d5-b851-9208c5805f03-StreamThread-2-consumer-bc3bc66c-5950-4c76-9bd1-e0ce3e0db20a /192.168.65.1   stateless-kafka-streams-example-103d456a-cd0c-49d5-b851-9208c5805f03-StreamThread-2-consumer
```

You can see the difference between `scale up` vs `scale out`

------------------------------------------------------------------------------------------------------------------------

Provide input

```bash
$ docker compose exec -it tools bash
$ kafka-console-producer --bootstrap-server broker:9092 --topic stateful-demo-input-topic
```

Check output

```bash
$ kafka-console-consumer --bootstrap-server broker:9092 --topic stateful-demo-output-topic  --from-beginning \
--property print.key=true --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

* Start the StatefulStreamProcessingExample

Check the consumer group:
```bash
$ kafka-consumer-groups --bootstrap-server broker:9092 --group stateful-kafka-streams-example --describe
```

Check that the `changelog` and `repartition` topics were created

```bash
$ kafka-topics --bootstrap-server broker:9092 --list | grep stateful
```

```bash
$ ls -l /tmp/kafka-streams/stateful-kafka-streams-example
```


------------------------------------------------------------------------------------------------------------------------

`KStream`

```java
Stream<String, String> firstStream = 
    builder.stream(inputTopic, Consumed.with(Serdes.String(), Serdes.String())); 
```

`KTable`

```java
KTable<String, String> firstKTable = 
    builder.table(inputTopic, Materialized.with(Serdes.String(), Serdes.String()));
```

`GlobalKTable`

```java
GlobalKTable<String, String> globalKTable = 
    builder.globalTable(inputTopic, Materialized.with(Serdes.String(), Serdes.String()));
```

- The main difference between a KTable and a GlobalKTable is that a KTable shards data between Kafka Streams instances, while a GlobalKTable extends a full copy of the data to each instance. 
- You typically use a GlobalKTable with lookup data.



