Based on the example:

https://kafka-tutorials.confluent.io/create-stateful-aggregation-count/kstreams.html

```bash
docker exec -i schema-registry kafka-avro-console-producer --topic movie-ticket-sales \
  --bootstrap-server kafka:9092 \
  --property value.schema="$(< src/main/resources/avro/ticket-sale.avsc)"
```

```bash
{"title":"Die Hard","sale_ts":"2019-07-18T10:00:00Z","ticket_total_value":12}
{"title":"Die Hard","sale_ts":"2019-07-18T10:01:00Z","ticket_total_value":12}
{"title":"The Godfather","sale_ts":"2019-07-18T10:01:31Z","ticket_total_value":12}
{"title":"Die Hard","sale_ts":"2019-07-18T10:01:36Z","ticket_total_value":24}
{"title":"The Godfather","sale_ts":"2019-07-18T10:02:00Z","ticket_total_value":18}
{"title":"The Big Lebowski","sale_ts":"2019-07-18T11:03:21Z","ticket_total_value":12}
{"title":"The Big Lebowski","sale_ts":"2019-07-18T11:03:50Z","ticket_total_value":12}
{"title":"The Godfather","sale_ts":"2019-07-18T11:40:00Z","ticket_total_value":36}
{"title":"The Godfather","sale_ts":"2019-07-18T11:40:09Z","ticket_total_value":18}
```

Start the console consumer

```bash
docker exec -it broker kafka-console-consumer --topic movie-tickets-sold \
  --bootstrap-server broker:19092 \
  --from-beginning \
  --property print.key=true \
  --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

View topics 

```bash
docker exec -it broker kafka-topics --bootstrap-server broker:9092 --list
```

and you will see in the consumer:

```bash
Die Hard	1
Die Hard	2
The Godfather	1
Die Hard	3
The Godfather	2
The Big Lebowski	1
The Big Lebowski	2
The Godfather	3
The Godfather	4
```


`KafkaStreams` actually provides a `TopologyTestDriver` that can be used to test the Topologies built via the 
`StreamsBuilder` without the need to have an embedded kafka.



