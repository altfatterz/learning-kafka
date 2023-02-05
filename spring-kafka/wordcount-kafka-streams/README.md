1. Setup the infrastructure using `spring-kafka/docker-compose.yml` file

2. Start the `WordCountApp` application. The application creates automatically the followings: 
- the input topic
- the output topic
- a producer generating input
- a stream processing the input and generating output

3. List topics:

```bash
$ kafka-topics --bootstrap-server localhost:19092 --list

__consumer_offsets
word-count-input
word-count-output
wordcount-KSTREAM-AGGREGATE-STATE-STORE-0000000003-changelog
wordcount-KSTREAM-AGGREGATE-STATE-STORE-0000000003-repartition
```

4. Start consumer:

```bash
$ kafka-console-consumer --topic word-count-output --from-beginning \
   --bootstrap-server localhost:19092 \
   --property print.key=true \
   --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

5. Metrics exposed via actuator / prometheus registry. The `kafka.stream.state` might appear a few seconds later.

```bash
$ http :8080/actuator/metrics
...
    "kafka.stream.state.all.rate",
    
$ http :8080/actuator/prometheus
... 
# HELP kafka_stream_state_all_rate The average number of calls to all per second
# TYPE kafka_stream_state_all_rate gauge
kafka_stream_state_all_rate{kafka_version="3.3.2",rocksdb_state_id="KSTREAM-AGGREGATE-STATE-STORE-0000000003",spring_id="defaultKafkaStreamsBuilder",task_id="1_0",thread_id="wordcount-23f9c82c-af39-4b8b-9042-e5afd8ff07c9-StreamThread-1",} 0.0
    
```

