------------------------------------------------------------------------------------------------------------------------

- `Task assignment` 
  - related to the consumer group and rebalancing protocol
  - `StreamThread` -  
  - `Task` - correspond to topic partitions (can be `active` or `standby`)
  - if you assign a task to a node which didn't work on it yet, need to restore the state from `changelog` topic (long delay)
    --> `standby` tasks - only maintain state and they don't do any processing

------------------------------------------------------------------------------------------------------------------------

`KStream`

```java
Stream<String, String> stream = builder.stream(inputTopic, Consumed.with(Serdes.String(), Serdes.String())); 
```

`KTable` (Update Stream)
- can only be subscribed to a single topic unlike `KStream`
- represents the latest value of each record
- backed by a state store (can lookup the latest value) - RocksDB
- buffer the updates, only when flushed the updates get forwarded further down the processor topology
  - (30 seconds by default - how quickly you want to see updates from this `KTable`)
- KTable sees one partition of that topic at a time

```java
KTable<String, String> ktable = builder.table(inputTopic, Materialized.with(Serdes.String(), Serdes.String()));
```

`GlobalKTable`

```java
GlobalKTable<String, String> globalKTable = builder.globalTable(inputTopic, Materialized.with(Serdes.String(), Serdes.String()));
```

- The main difference between a `KTable` and a `GlobalKTable`
  - `KTable` shards data between Kafka Streams instances,
  - `GlobalKTable` extends a full copy of the data to each instance.
- You typically use a `GlobalKTable` with lookup data, smaller in size and don't change over time that much

------------------------------------------------------------------------------------------------------------------------

Serdes (Serialization / Deserialization)

------------------------------------------------------------------------------------------------------------------------

`Joins`   
 - join events with the `same key`
 - stream-stream join -> new stream 
   - windowed join - records that arrive are joined with other records of the same key within a defined window of time
   - all the is stored in local state store to keep track what data has arrived during this time
   - keys cannot be changed, can be computed a new value
   - join types:
     - inner - produce only a record if both sides had a record within the defined window
     - outer - opposite, both sides produce an output record 
       - left-value + right-value
       - left-value + NULL
       - NULL + right-value
     - left-outer - only the left value will produce a record (you can decide which stream is the left)
       - left-value + right-value
       - left-value + NULL
 - stream-table join -> new stream
   - not windowed, when you get new stream event will be join with the latest value from the table -> produces the output record
   - KStream-KTable join
   - KStream-GlobalKTable join
   - join types:
     - inner - only produce a record if both sides are available
     - left-outer join - left is the stream (the stream drives the join)
   - GlobalKTables all information is bootstrapped in ahead of time 
     - read all events from topic as soon as they occur into the GlobalKTable 
   - KTables events and join are timestamp driven 
     - events in KTable with a higher timestamp than events in KStream are not going to be joined with those earlier events
 - table-table join -> new `table`
   - not windowed

`ValueJoiner`
`ValueJoinerWithKey`

```bash
leftStream.join(rightStream, valueJoiner, JoinWindows.of(Duration.ofSeconds(10)))
- window duration - amount of time that two events can differ between the left side and the right side 
```

------------------------------------------------------------------------------------------------------------------------

Otter Romp: Shallow Dives into Kafka Streams (A. Sophie Blee-Goldman, Responsive) https://www.youtube.com/watch?v=uzr-MDrDqJg

------------------------------------------------------------------------------------------------------------------------

### Stateless Demo 

```bash
$ docker compose up -d
```

Create the topics:

```bash
docker compose exec -it broker bash
  
kafka-topics --bootstrap-server broker:9092 --delete --topic stateless-demo-input-topic
kafka-topics --bootstrap-server broker:9092 --delete --topic stateless-demo-output-topic
kafka-topics --bootstrap-server broker:9092 --delete --topic stateful-demo-input-topic
kafka-topics --bootstrap-server broker:9092 --delete --topic stateful-demo-output-topic
kafka-topics --bootstrap-server broker:9092 --delete --topic ktable-input-topic
kafka-topics --bootstrap-server broker:9092 --delete --topic ktable-output-topic 

kafka-topics --bootstrap-server broker:9092 --topic stateless-demo-input-topic --create --partitions 3 --replication-factor 1
kafka-topics --bootstrap-server broker:9092 --topic stateless-demo-output-topic --create --partitions 3 --replication-factor 1
kafka-topics --bootstrap-server broker:9092 --topic stateful-demo-input-topic --create --partitions 3 --replication-factor 1 
kafka-topics --bootstrap-server broker:9092 --topic stateful-demo-output-topic --create --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server broker:9092 --topic ktable-input-topic --create --partitions 3 --replication-factor 1
kafka-topics --bootstrap-server broker:9092 --topic ktable-output-topic --create --partitions 1 --replication-factor 1
```

* Add data into the input topic

```bash
docker compose exec -it broker bash
kafka-console-producer --bootstrap-server broker:9092 --topic stateless-demo-input-topic
```

* Stream the output content:

```bash
docker compose exec -it tools bash
kafka-console-consumer --bootstrap-server broker:9092 --topic stateless-demo-output-topic --from-beginning --property print.key=true 
```

* Start the StatelessStreamProcessingExample

Check the logs:
```bash
13:50:09.640 [main] INFO  c.e.StatelessStreamProcessingExample - Topologies:
   Sub-topology: 0
    Source: KSTREAM-SOURCE-0000000000 (topics: [stateless-demo-input-topic])
      --> KSTREAM-PEEK-0000000001
    Processor: KSTREAM-PEEK-0000000001 (stores: [])
      --> KSTREAM-MAPVALUES-0000000002
      <-- KSTREAM-SOURCE-0000000000
    Processor: KSTREAM-MAPVALUES-0000000002 (stores: [])
      --> KSTREAM-FILTER-0000000003
      <-- KSTREAM-PEEK-0000000001
    Processor: KSTREAM-FILTER-0000000003 (stores: [])
      --> KSTREAM-PEEK-0000000004
      <-- KSTREAM-MAPVALUES-0000000002
    Processor: KSTREAM-PEEK-0000000004 (stores: [])
      --> KSTREAM-SINK-0000000005
      <-- KSTREAM-FILTER-0000000003
    Sink: KSTREAM-SINK-0000000005 (topic: stateless-demo-output-topic)
      <-- KSTREAM-PEEK-0000000004

...
13:50:53.214 [stateless-kafka-streams-example-d04a1821-021a-44e2-af6f-b429a5bd731f-StreamThread-1] INFO  o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=stateless-kafka-streams-example-d04a1821-021a-44e2-af6f-b429a5bd731f-StreamThread-1-consumer, groupId=stateless-kafka-streams-example] Updating assignment with
	Assigned partitions:                       [stateless-demo-input-topic-0, stateless-demo-input-topic-1, stateless-demo-input-topic-2]
	Current owned partitions:                  []
	Added partitions (assigned - owned):       [stateless-demo-input-topic-0, stateless-demo-input-topic-1, stateless-demo-input-topic-2]
	Revoked partitions (owned - assigned):     []
...
13:50:53.216 [stateless-kafka-streams-example-d04a1821-021a-44e2-af6f-b429a5bd731f-StreamThread-1] INFO  o.a.k.s.p.internals.TaskManager - stream-thread [stateless-kafka-streams-example-d04a1821-021a-44e2-af6f-b429a5bd731f-StreamThread-1] Handle new assignment with:
	New active tasks: [0_2, 0_1, 0_0]
	New standby tasks: []
	Existing active tasks: []
	Existing standby tasks: []
```

Start another instance and check consumer groups:

```bash
docker compose exec -it broker bash
kafka-consumer-groups --bootstrap-server broker:9092 --group stateless-kafka-streams-example --describe

GROUP                           TOPIC                      PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                                                                                                       HOST            CLIENT-ID
stateless-kafka-streams-example stateless-demo-input-topic 2          -               0               -               stateless-kafka-streams-example-d04a1821-021a-44e2-af6f-b429a5bd731f-StreamThread-1-consumer-511a4173-2bfa-418a-89e8-0be8f3b2bd76 /192.168.65.1   stateless-kafka-streams-example-d04a1821-021a-44e2-af6f-b429a5bd731f-StreamThread-1-consumer
stateless-kafka-streams-example stateless-demo-input-topic 1          -               0               -               stateless-kafka-streams-example-d04a1821-021a-44e2-af6f-b429a5bd731f-StreamThread-1-consumer-511a4173-2bfa-418a-89e8-0be8f3b2bd76 /192.168.65.1   stateless-kafka-streams-example-d04a1821-021a-44e2-af6f-b429a5bd731f-StreamThread-1-consumer
stateless-kafka-streams-example stateless-demo-input-topic 0          -               0               -               stateless-kafka-streams-example-d04a1821-021a-44e2-af6f-b429a5bd731f-StreamThread-1-consumer-511a4173-2bfa-418a-89e8-0be8f3b2bd76 /192.168.65.1   stateless-kafka-streams-example-d04a1821-021a-44e2-af6f-b429a5bd731f-StreamThread-1-consumer
```

Stop the instances, modify this line and check consumer groups

```bash
// settings.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2);
```

```bash
GROUP                           TOPIC                      PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                                                                                                       HOST            CLIENT-ID
stateless-kafka-streams-example stateless-demo-input-topic 2          -               0               -               stateless-kafka-streams-example-b3595680-40c6-47f0-913c-4c9b175da129-StreamThread-1-consumer-91d9bd97-c323-4a9b-8675-8acc054f447a /192.168.65.1   stateless-kafka-streams-example-b3595680-40c6-47f0-913c-4c9b175da129-StreamThread-1-consumer
stateless-kafka-streams-example stateless-demo-input-topic 0          -               0               -               stateless-kafka-streams-example-b3595680-40c6-47f0-913c-4c9b175da129-StreamThread-1-consumer-91d9bd97-c323-4a9b-8675-8acc054f447a /192.168.65.1   stateless-kafka-streams-example-b3595680-40c6-47f0-913c-4c9b175da129-StreamThread-1-consumer
stateless-kafka-streams-example stateless-demo-input-topic 1          -               0               -               stateless-kafka-streams-example-b3595680-40c6-47f0-913c-4c9b175da129-StreamThread-2-consumer-feef0874-b118-400e-b922-9e1337002f5f /192.168.65.1   stateless-kafka-streams-example-b3595680-40c6-47f0-913c-4c9b175da129-StreamThread-2-consumer
```

You can see the difference between `scale up` (running with more StreamThreads) vs `scale out` (running multiple instances) 

------------------------------------------------------------------------------------------------------------------------

### Stateful Demo

```bash
docker compose exec -it broker bash
kafka-console-producer --bootstrap-server broker:9092 --topic stateful-demo-input-topic
```

Check output

```bash
docker compose exec -it broker bash
kafka-console-consumer --bootstrap-server broker:9092 --topic stateful-demo-output-topic  --from-beginning \
--property print.key=true --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

* Start the StatefulStreamProcessingExample

Check the logs:

```bash
14:13:25.912 [main] INFO  c.e.StatefulStreamProcessingExample - Topologies:
   Sub-topology: 0
    Source: KSTREAM-SOURCE-0000000000 (topics: [stateful-demo-input-topic])
      --> KSTREAM-FLATMAPVALUES-0000000001
    Processor: KSTREAM-FLATMAPVALUES-0000000001 (stores: [])
      --> KSTREAM-PEEK-0000000002
      <-- KSTREAM-SOURCE-0000000000
    Processor: KSTREAM-PEEK-0000000002 (stores: [])
      --> KSTREAM-KEY-SELECT-0000000003
      <-- KSTREAM-FLATMAPVALUES-0000000001
    Processor: KSTREAM-KEY-SELECT-0000000003 (stores: [])
      --> WordCount-repartition-filter
      <-- KSTREAM-PEEK-0000000002
    Processor: WordCount-repartition-filter (stores: [])
      --> WordCount-repartition-sink
      <-- KSTREAM-KEY-SELECT-0000000003
    Sink: WordCount-repartition-sink (topic: WordCount-repartition)
      <-- WordCount-repartition-filter

  Sub-topology: 1
    Source: WordCount-repartition-source (topics: [WordCount-repartition])
      --> KSTREAM-AGGREGATE-0000000004
    Processor: KSTREAM-AGGREGATE-0000000004 (stores: [WordCount])
      --> KTABLE-TOSTREAM-0000000008
      <-- WordCount-repartition-source
    Processor: KTABLE-TOSTREAM-0000000008 (stores: [])
      --> KSTREAM-PEEK-0000000009
      <-- KSTREAM-AGGREGATE-0000000004
    Processor: KSTREAM-PEEK-0000000009 (stores: [])
      --> KSTREAM-SINK-0000000010
      <-- KTABLE-TOSTREAM-0000000008
    Sink: KSTREAM-SINK-0000000010 (topic: stateful-demo-output-topic)
      <-- KSTREAM-PEEK-0000000009
...

14:13:26.514 [stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1] INFO  o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer, groupId=stateful-kafka-streams-example] Updating assignment with
	Assigned partitions:                       [stateful-demo-input-topic-0, stateful-demo-input-topic-1, stateful-demo-input-topic-2, stateful-kafka-streams-example-WordCount-repartition-0, stateful-kafka-streams-example-WordCount-repartition-1, stateful-kafka-streams-example-WordCount-repartition-2]
	Current owned partitions:                  []
	Added partitions (assigned - owned):       [stateful-demo-input-topic-0, stateful-demo-input-topic-1, stateful-demo-input-topic-2, stateful-kafka-streams-example-WordCount-repartition-0, stateful-kafka-streams-example-WordCount-repartition-1, stateful-kafka-streams-example-WordCount-repartition-2]
	Revoked partitions (owned - assigned):     []
	
14:13:26.515 [stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1] INFO  o.a.k.s.p.internals.TaskManager - stream-thread [stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1] Handle new assignment with:
	New active tasks: [1_0, 0_2, 1_2, 0_1, 1_1, 0_0]
	New standby tasks: []
	Existing active tasks: []
	Existing standby tasks: []	
...
	      
      
      
```

Check the consumer group:
```bash
docker compose exec -it broker bash
kafka-consumer-groups --bootstrap-server broker:9092 --group stateful-kafka-streams-example --describe

GROUP                          TOPIC                                                PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                                                                                                      HOST            CLIENT-ID
stateful-kafka-streams-example stateful-demo-input-topic                            1          -               0               -               stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer-4d55d304-254f-472f-afa4-2003f0472d07 /192.168.65.1   stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer
stateful-kafka-streams-example stateful-demo-input-topic                            2          -               0               -               stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer-4d55d304-254f-472f-afa4-2003f0472d07 /192.168.65.1   stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer
stateful-kafka-streams-example stateful-kafka-streams-example-WordCount-repartition 2          -               0               -               stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer-4d55d304-254f-472f-afa4-2003f0472d07 /192.168.65.1   stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer
stateful-kafka-streams-example stateful-kafka-streams-example-WordCount-repartition 0          -               0               -               stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer-4d55d304-254f-472f-afa4-2003f0472d07 /192.168.65.1   stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer
stateful-kafka-streams-example stateful-demo-input-topic                            0          -               0               -               stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer-4d55d304-254f-472f-afa4-2003f0472d07 /192.168.65.1   stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer
stateful-kafka-streams-example stateful-kafka-streams-example-WordCount-repartition 1          -               0               -               stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer-4d55d304-254f-472f-afa4-2003f0472d07 /192.168.65.1   stateful-kafka-streams-example-1e52e566-415b-4cd2-a6a4-0e7f0d08cfd1-StreamThread-1-consumer
```

Check that the `changelog` and `repartition` topics were created

```bash
kafka-topics --bootstrap-server broker:9092 --list | grep stateful

stateful-demo-input-topic
stateful-demo-output-topic
stateful-kafka-streams-example-WordCount-changelog
stateful-kafka-streams-example-WordCount-repartition
```

Check on the host where the `stateful-kafka-streams-example` process is running

```bash
$ ls -l /tmp/kafka-streams/stateful-kafka-streams-example
drwxr-xr-x@ 2 altfatterz  wheel   64 May  1 14:13 0_0
drwxr-xr-x@ 2 altfatterz  wheel   64 May  1 14:13 0_1
drwxr-xr-x@ 2 altfatterz  wheel   64 May  1 14:13 0_2
drwxr-xr-x@ 4 altfatterz  wheel  128 May  1 14:13 1_0
drwxr-xr-x@ 4 altfatterz  wheel  128 May  1 14:13 1_1
drwxr-xr-x@ 4 altfatterz  wheel  128 May  1 14:13 1_2
```



### KTableExample

Producer:

```bash
docker compose exec -it broker bash
kafka-console-producer --bootstrap-server broker:9092 --topic ktable-input-topic --property parse.key=true --property key.separator=:

1:id-1000
2:id-1012
3:id-1232
2:id-2511
3:id-111

```

* Consumer:

```bash
docker compose exec -it broker bash
kafka-console-consumer --bootstrap-server broker:9092 --topic ktable-output-topic --from-beginning --property print.key=true 
```

Start `KTableExample`

```bash
14:33:02.857 [main] INFO  c.e.StatelessStreamProcessingExample - Topologies:
   Sub-topology: 0
    Source: KSTREAM-SOURCE-0000000000 (topics: [ktable-input-topic])
      --> KTABLE-SOURCE-0000000001
    Processor: KTABLE-SOURCE-0000000001 (stores: [ktable-store])
      --> KTABLE-FILTER-0000000002
      <-- KSTREAM-SOURCE-0000000000
    Processor: KTABLE-FILTER-0000000002 (stores: [])
      --> KTABLE-MAPVALUES-0000000003
      <-- KTABLE-SOURCE-0000000001
    Processor: KTABLE-MAPVALUES-0000000003 (stores: [])
      --> KTABLE-FILTER-0000000004
      <-- KTABLE-FILTER-0000000002
    Processor: KTABLE-FILTER-0000000004 (stores: [])
      --> KTABLE-TOSTREAM-0000000005
      <-- KTABLE-MAPVALUES-0000000003
    Processor: KTABLE-TOSTREAM-0000000005 (stores: [])
      --> KSTREAM-PEEK-0000000006
      <-- KTABLE-FILTER-0000000004
    Processor: KSTREAM-PEEK-0000000006 (stores: [])
      --> KSTREAM-SINK-0000000007
      <-- KTABLE-TOSTREAM-0000000005
    Sink: KSTREAM-SINK-0000000007 (topic: ktable-output-topic)
      <-- KSTREAM-PEEK-0000000006
      
14:33:03.303 [ktable-example-7fba0928-260a-4973-b637-4c6abf99a317-StreamThread-1] INFO  o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=ktable-example-7fba0928-260a-4973-b637-4c6abf99a317-StreamThread-1-consumer, groupId=ktable-example] Updating assignment with
	Assigned partitions:                       [ktable-input-topic-0, ktable-input-topic-1, ktable-input-topic-2]
	Current owned partitions:                  []
	Added partitions (assigned - owned):       [ktable-input-topic-0, ktable-input-topic-1, ktable-input-topic-2]
	Revoked partitions (owned - assigned):     []
	
14:33:03.305 [ktable-example-7fba0928-260a-4973-b637-4c6abf99a317-StreamThread-1] INFO  o.a.k.s.p.internals.TaskManager - stream-thread [ktable-example-7fba0928-260a-4973-b637-4c6abf99a317-StreamThread-1] Handle new assignment with:
	New active tasks: [0_2, 0_1, 0_0]
	New standby tasks: []
	Existing active tasks: []
	Existing standby tasks: []
		      
```

```bash
kafka-consumer-groups --bootstrap-server broker:9092 --group ktable-example --describe

GROUP           TOPIC              PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                                                                                      HOST            CLIENT-ID
ktable-example  ktable-input-topic 2          -               0               -               ktable-example-7fba0928-260a-4973-b637-4c6abf99a317-StreamThread-1-consumer-804fe70d-fba9-42fb-998f-305854a14d4f /192.168.65.1   ktable-example-7fba0928-260a-4973-b637-4c6abf99a317-StreamThread-1-consumer
ktable-example  ktable-input-topic 1          -               0               -               ktable-example-7fba0928-260a-4973-b637-4c6abf99a317-StreamThread-1-consumer-804fe70d-fba9-42fb-998f-305854a14d4f /192.168.65.1   ktable-example-7fba0928-260a-4973-b637-4c6abf99a317-StreamThread-1-consumer
ktable-example  ktable-input-topic 0          -               0               -               ktable-example-7fba0928-260a-4973-b637-4c6abf99a317-StreamThread-1-consumer-804fe70d-fba9-42fb-998f-305854a14d4f /192.168.65.1   ktable-example-7fba0928-260a-4973-b637-4c6abf99a317-StreamThread-1-consumer
```

