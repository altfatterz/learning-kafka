Start up the cluster:

```bash
$ docker compose up -d
```

Create the topics:

```bash
$ docker compose exec -it tools bash
$ kafka-topics --bootstrap-server broker:9092 --delete --topic sentences-topic
$ kafka-topics --bootstrap-server broker:9092 --delete --topic lowercase-sentences-topic
$ kafka-topics --bootstrap-server broker:9092 --delete --topic word-counts-topic 
$ kafka-topics --bootstrap-server broker:9092 --topic sentences-topic --create --partitions 3 --replication-factor 1 
$ kafka-topics --bootstrap-server broker:9092 --topic lowercase-sentences-topic --create --partitions 1 --replication-factor 1 
$ kafka-topics --bootstrap-server broker:9092 --topic word-counts-topic --create --partitions 1 --replication-factor 1 
```

* Add data into the input topic

```bash
$ docker compose exec -it tools bash
$ kafka-console-producer --bootstrap-server broker:9092 --topic sentences-topic
```

* Stream the output content:

```bash
$ docker compose exec -it tools bash
$ kafka-console-consumer --bootstrap-server broker:9092 --topic lowercase-sentences-topic --from-beginning --property print.key=true 
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
	Assigned partitions:                       [sentences-topic-0, sentences-topic-1, sentences-topic-2]
	Current owned partitions:                  []
	Added partitions (assigned - owned):       [sentences-topic-0, sentences-topic-1, sentences-topic-2]
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

* Start the StatefulStreamProcessingExample

```bash
$ kafka-console-consumer --bootstrap-server broker:9092 --topic word-counts-topic --from-beginning \
--property print.key=true --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

```bash
$ kafka-consumer-groups --bootstrap-server broker:9092 --list
$ kafka-consumer-groups --bootstrap-server broker:9092 --group stateless-kafka-streams-example --describe
$ kafka-consumer-groups --bootstrap-server broker:9092 --group stateful-kafka-streams-example --describe
```

