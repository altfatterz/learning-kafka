
### Startup single node cluster

```bash
$ docker compose up -d
```

Wait until the cluster is available in the Confluent Control Center: http://localhost:9021/

### Create the input and output topics:

```bash
$ docker exec -i broker kafka-topics --bootstrap-server broker:9092 --create --topic movie-ticket-sales \
--partitions 1 --replication-factor 1

$ docker exec -i broker kafka-topics --bootstrap-server broker:9092 --create --topic movie-tickets-sold \
--partitions 1 --replication-factor 1

$ docker exec -i broker kafka-topics --bootstrap-server broker:9092 --list 
```

### Start the service 

`TicketStalesApp` - via IntelliJ, check the configuration values in the logs for: 

- StreamsConfig values:
- KafkaAvroSerializerConfig values:
- KafkaAvroDeserializerConfig values:
- Topologies:
- Kafka version: 3.7.0

### Produce events to the input topic

```bash
$ cd ticket-sales
$ docker exec -i schema-registry kafka-avro-console-producer --topic movie-ticket-sales \
--bootstrap-server broker:9092 \
--property value.schema="$(< src/main/resources/avro/ticket-sale.avsc)"
```

Waiting for your input:

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


### Check the output topic

```bash
$ docker exec -it broker kafka-console-consumer --topic movie-tickets-sold \
  --bootstrap-server broker:9092 \
  --from-beginning \
  --property print.key=true
```

```bash
Die Hard	1 tickets sold
Die Hard	2 tickets sold
The Godfather	1 tickets sold
Die Hard	3 tickets sold
The Godfather	2 tickets sold
The Big Lebowski	1 tickets sold
The Big Lebowski	2 tickets sold
The Godfather	3 tickets sold
The Godfather	4 tickets sold
```


### Notes

- https://developer.confluent.io/courses/kafka-streams/testing/
- Memory management: https://docs.confluent.io/platform/current/streams/developer-guide/memory-mgmt.html
- 

`KafkaStreams` actually provides a `TopologyTestDriver` that can be used to test the Topologies built via the 
`StreamsBuilder` without the need to have an embedded kafka.



### Resources:

- https://kafka-tutorials.confluent.io/create-stateful-aggregation-count/kstreams.html
