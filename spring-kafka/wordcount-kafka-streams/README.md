1. Create the topics:

```bash
$ kafka-topics --create --topic word-count-input \
   --bootstrap-server localhost:19092 --partitions 1 --replication-factor 1

$ kafka-topics --create --topic word-count-output \
   --bootstrap-server localhost:19092 --partitions 1 --replication-factor 1
```

2. List topics:

```bash
$ kafka-topics --bootstrap-server localhost:19092 --list

__consumer_offsets
word-count-input
word-count-output
```

3. Start consumer:

```bash
$ kafka-console-consumer --topic word-count-output --from-beginning \
   --bootstrap-server localhost:19092 \
   --property print.key=true \
   --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

4. Start producer:

```bash
$ kafka-console-producer --bootstrap-server localhost:19092 --topic word-count-input
```

5. Start Kafka Streams app:

```bash
$ java -jar target/wordcount-kafka-streams.jar
```

6. List again the topics:

```bash
$ kafka-topics --bootstrap-server localhost:19092 --list

__confluent.support.metrics
__consumer_offsets
word-count-input
word-count-output
wordcount-KSTREAM-AGGREGATE-STATE-STORE-0000000003-changelog
wordcount-KSTREAM-AGGREGATE-STATE-STORE-0000000003-repartition
```

7. Produce some sentences:



DSL API:
https://kafka.apache.org/20/documentation/streams/developer-guide/dsl-api.html

