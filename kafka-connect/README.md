Introducing `Confluent Hub`:https://www.confluent.io/blog/introducing-confluent-hub/

`Confluent Hub`: https://www.confluent.io/hub/

Kafka Connect official documentation:
https://docs.confluent.io/current/connect/index.html

The Simplest Useful Kafka Connect Data Pipeline In The World:
1. https://www.confluent.io/blog/simplest-useful-kafka-connect-data-pipeline-world-thereabouts-part-1/
2. https://www.confluent.io/blog/the-simplest-useful-kafka-connect-data-pipeline-in-the-world-or-thereabouts-part-2/
3. https://www.confluent.io/blog/simplest-useful-kafka-connect-data-pipeline-world-thereabouts-part-3/



Kafka Connect Datagen Connector https://github.com/confluentinc/kafka-connect-datagen

Create Topics

```bash
$ docker exec -it tools bash
kafka-topics --bootstrap-server kafka:9092 --create --topic pageviews --partitions 3 --replication-factor 1
```

Install & Restart
```bash
$ docker-compose exec -u root connect confluent-hub install confluentinc/kafka-connect-datagen:0.4.0
$ docker-compose restart connect
```

```bash
curl -X POST \
-H "Content-Type: application/json" \
--data '{
"name": "DatagenConnector-pageviews",
"config": {
"connector.class": "io.confluent.kafka.connect.datagen.DatagenConnector",
"key.converter": "org.apache.kafka.connect.storage.StringConverter",
"kafka.topic": "pageviews",
"quickstart": "pageviews",
"max.interval": 1000,
"iterations": 10000000,
"tasks.max": "1"}
}' http://connect:8083/connectors | jq
```


```bash
$ docker exec -it tools bash
kafka-avro-console-consumer \
--bootstrap-server kafka:9092 \
--property schema.registry.url=http://schema-registry:8081 \
--topic pageviews \
--property print.key=true \
--key-deserializer=org.apache.kafka.common.serialization.StringDeserializer \
--from-beginning
```