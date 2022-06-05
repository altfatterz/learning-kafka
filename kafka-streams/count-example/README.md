The topics are already created with the `docker-compose` configuration.

```bash
$ docker exec kafka kafka-topics --bootstrap-server kafka:9092 --list
```

* Producer to `movie-ticket-sales` topic
```bash
$ cd count-example
$ docker exec -i schema-registry /usr/bin/kafka-avro-console-producer --topic movie-ticket-sales --bootstrap-server kafka:9092 --property value.schema="$(< src/main/resources/avro/ticket-sale.avsc)"

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

* Consumer from `movie-tickets-sold` topic 
```bash
$ cd count-example
$ docker exec tools kafka-console-consumer --topic movie-tickets-sold --bootstrap-server kafka:9092 --from-beginning --property print.key=true --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

You should not see anything yet, since we didn't start the stream processing application. After starting it, the output:

```bash
$ mvn clean install
$ java -cp target/count-example-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.CountExampleApplication config/local.properties
```

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



