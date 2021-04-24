The topics are already created with the `docker-compose` configuration.


To create the topics:

```bash
kafka-topics --bootstrap-server kafka:9092 --create --topic movie-ticket-sales --partitions 1 --replication-factor 1; 
kafka-topics --bootstrap-server kafka:9092 --create --topic movie-tickets-sold -partitions 1 --replication-factor 1;
```

To delete the topics:

```bash
$ kafka-topics --bootstrap-server kafka:9092 --delete --topic movie-ticket-sales;
$ kafka-topics --bootstrap-server kafka:9092 --delete --topic movie-tickets-sold;
```

```bash
$ kafka-topics --bootstrap-server kafka:9092 --list
```

Producer:
```bash
docker exec -i schema-registry /usr/bin/kafka-avro-console-producer --topic movie-ticket-sales --bootstrap-server kafka:9092 --property value.schema="$(< src/main/resources/avro/ticket-sale.avsc)"

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

Consumer:


```bash
$ docker exec -it tools bash
$ kafka-console-consumer --topic movie-tickets-sold --bootstrap-server kafka:9092 --from-beginning --property print.key=true --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```




