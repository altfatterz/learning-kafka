#### Kafka Basics

#### Start kafka with control center 

```bash
$ docker compose up -d
```

#### Create a topic

```bash
$ docker exec -it broker bash
$ kafka-topics --bootstrap-server broker:9092 --delete --topic test-topic
$ kafka-topics --bootstrap-server broker:9092 --create --topic test-topic --partitions 3 --replication-factor 1
$ exit
```

### Access control center

```bash
$ open https://localhost:9021
```

#### Build the consumer and producer

```bash
$ mvn clean package
```

#### Start the consumer

```bash
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaConsumerDemo config/local-consumer.properties
```

#### Start the producer

```bash
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaProducerDemo config/local-producer.properties
```

### Start the consumer with `read-from-timestamp` property

```bash
$ docker exec -it broker bash
$ kafka-consumer-groups --bootstrap-server broker:9092 --list
$ kafka-consumer-groups --bootstrap-server broker:9092 --group kafka-basic-local-consumer-group --describe

GROUP                            TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
kafka-basic-local-consumer-group test-topic      0          863             863             0               -               -               -
kafka-basic-local-consumer-group test-topic      1          797             797             0               -               -               -
kafka-basic-local-consumer-group test-topic      2          804             804             0               -               -               -

$ kafka-consumer-groups --bootstrap-server broker:9092 --group kafka-basic-local-consumer-group --delete
Deletion of requested consumer groups ('kafka-basic-local-consumer-group') was successful.
```

Restart the consumer and check the logs you should see the "Seek to " log entry

```bash
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaConsumerDemo config/local-consumer.properties
```

Check the logs:

```bash
09:15:47.811 [main] INFO  c.g.altfatterz.KafkaConsumerDemo - Seeking partition 0 to offset 100
09:15:47.811 [main] INFO  o.a.k.clients.consumer.KafkaConsumer - [Consumer clientId=consumer-kafka-basic-local-consumer-group-1, groupId=kafka-basic-local-consumer-group] Seeking to offset 100 for partition test-topic-0
09:15:47.812 [main] INFO  c.g.altfatterz.KafkaConsumerDemo - Seeking partition 1 to offset 84
09:15:47.812 [main] INFO  o.a.k.clients.consumer.KafkaConsumer - [Consumer clientId=consumer-kafka-basic-local-consumer-group-1, groupId=kafka-basic-local-consumer-group] Seeking to offset 84 for partition test-topic-1
09:15:47.812 [main] INFO  c.g.altfatterz.KafkaConsumerDemo - Seeking partition 2 to offset 87
09:15:47.812 [main] INFO  o.a.k.clients.consumer.KafkaConsumer - [Consumer clientId=consumer-kafka-basic-local-consumer-group-1, groupId=kafka-basic-local-consumer-group] Seeking to offset 87 for partition test-topic-2```
```

### Ways to reset offsets 
```bash
$ kafka-consumer-groups --bootstrap-server broker:9092 --group kafka-basic-local-consumer-group --topic test-topic --reset-offsets --shift-by 1 --dry-run
$ kafka-consumer-groups --bootstrap-server broker:9092 --group kafka-basic-local-consumer-group --topic test-topic --reset-offsets --to-earliest --dry-run
$ kafka-consumer-groups --bootstrap-server broker:9092 --group kafka-basic-local-consumer-group --topic test-topic --reset-offsets --to-latest --dry-run
$ kafka-consumer-groups --bootstrap-server broker:9092 --group kafka-basic-local-consumer-group --topic test-topic --reset-offsets --to-offset 100 --dry-run
$ kafka-consumer-groups --bootstrap-server broker:9092 --group kafka-basic-local-consumer-group --topic test-topic --reset-offsets --to-datetime '2024-03-17T12:28:15.000' --dry-run

GROUP                          TOPIC                          PARTITION  NEW-OFFSET
kafka-basic-local-consumer-group test-topic                     0          4
kafka-basic-local-consumer-group test-topic                     1          8
kafka-basic-local-consumer-group test-topic                     2          5

$ kafka-consumer-groups --bootstrap-server broker:9092 --group kafka-basic-local-consumer-group --topic test-topic --reset-offsets --to-datetime '2024-03-17T12:28:16.000' --dry-run

GROUP                          TOPIC                          PARTITION  NEW-OFFSET
kafka-basic-local-consumer-group test-topic                     0          7
kafka-basic-local-consumer-group test-topic                     1          10
kafka-basic-local-consumer-group test-topic                     2          5
```

the timestamp in Kafka is stored in UTC format. 

# Running with Confluent Cloud

1. Install Confluent CLI: https://docs.confluent.io/confluent-cli/current/install.html
2. Connect the CLI to your Confluent Cloud cluster: https://docs.confluent.io/confluent-cli/current/connect.html


```bash
$ confluent kafka topic create test-topic --cluster lkc-r05v70
```

```bash
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaConsumerDemo config/cloud-consumer.properties
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaProducerDemo config/cloud-producer.properties
```

Consume messages with Confluent CLI

```bash
$ confluent kafka topic consume test-topic
```

Confluent Client produce:

```bash
$ confluent iam user list
     ID    |        Email         | First Name | Last Name | Status | Authentication Method
-----------+----------------------+------------+-----------+--------+------------------------
  u-422714 | altfatterz@gmail.com | Zoltan     | Altfatter | Active | Username/Password
```

Manage api keys (both for kafka and schema-registry) 
```bash
$ confluent api-key list
```

Delete Kafka cluster:

```bash
$ confluent kafka cluster delete lkc-r05v70
```

Resources:

https://developer.confluent.io/tutorials/creating-first-apache-kafka-producer-application/confluent.html


