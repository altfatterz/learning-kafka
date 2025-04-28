#### Kafka Basics

#### Start kafka with control center 

```bash
$ docker compose up -d
```

#### Create a topic

```bash
$ docker exec -it broker bash
$ kafka-topics --bootstrap-server broker:29092 --delete --topic test-topic
$ kafka-topics --bootstrap-server broker:9092 --create --topic test-topic --partitions 3 --replication-factor 1
$ kafka-topics --bootstrap-server broker:29092 --describe --topic test-topic
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

#### Start the consumers

```bash
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaConsumerDemo config/local-consumer-1.properties
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaConsumerDemo config/local-consumer-2.properties
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaConsumerDemo config/local-consumer-3.properties
```

#### Start the producer

```bash
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaProducerDemo config/local-producer.properties
```

#### Check consumer group instances - each instance is processing a partition

```bash
$ kafka-consumer-groups --bootstrap-server broker:9092 --group kafka-basic-local-consumer-group --describe

GROUP                            TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                           HOST            CLIENT-ID
kafka-basic-local-consumer-group test-topic      0          488             497             9               local-consumer-1-f7da4f40-d258-47eb-805a-22d803de7495 /192.168.65.1   local-consumer-1
kafka-basic-local-consumer-group test-topic      1          454             458             4               local-consumer-2-1b26024e-5979-477d-a065-ece0e9179587 /192.168.65.1   local-consumer-2
kafka-basic-local-consumer-group test-topic      2          452             464             12              local-consumer-3-8ded3725-cdfe-4217-b498-e47017f787dd /192.168.65.1   local-consumer-3
```

### Start the consumer with `read-from-timestamp` property

```bash
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaConsumerDemo config/local-consumer-timestamp.properties
```

Check the logs:

```bash
13:14:22.505 [main] INFO  c.g.a.CustomConsumerRebalanceListener - onPartitionsAssigned called with: [test-topic-0, test-topic-1, test-topic-2]
13:14:22.505 [main] INFO  c.g.a.CustomConsumerRebalanceListener - timestampsToSearch: {test-topic-2=1745752418872, test-topic-0=1745752418872, test-topic-1=1745752418872}
13:14:22.508 [main] INFO  c.g.a.CustomConsumerRebalanceListener - Seeking partition 0 to offset 631
13:14:22.508 [main] INFO  o.a.k.c.c.i.ClassicKafkaConsumer - [Consumer clientId=consumer-local-consumer-timestamp-1, groupId=local-consumer-timestamp] Seeking to offset 631 for partition test-topic-0
13:14:22.508 [main] INFO  c.g.a.CustomConsumerRebalanceListener - Seeking partition 1 to offset 603
13:14:22.508 [main] INFO  o.a.k.c.c.i.ClassicKafkaConsumer - [Consumer clientId=consumer-local-consumer-timestamp-1, groupId=local-consumer-timestamp] Seeking to offset 603 for partition test-topic-1
13:14:22.508 [main] INFO  c.g.a.CustomConsumerRebalanceListener - Seeking partition 2 to offset 638
13:14:22.508 [main] INFO  o.a.k.c.c.i.ClassicKafkaConsumer - [Consumer clientId=consumer-local-consumer-timestamp-1, groupId=local-consumer-timestamp] Seeking to offset 638 for partition test-topic-2
```

### Other ways to reset offsets

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

1. Install Confluent CLI: https://docs.co   nfluent.io/confluent-cli/current/install.html
2. Connect the CLI to your Confluent Cloud cluster: https://docs.confluent.io/confluent-cli/current/connect.html


# Version

```bash

confluent version

Version:     v4.26.0
Git Ref:     d2facf65
Build Date:  2025-04-21T23:27:56Z
Go Version:  go1.22.7 (darwin/arm64)
Development: false

confluent update

```

# Confluent Shell

```bash
$ confluent shell
```

# Create a topic

```bash
$ confluent kafka topic create test-topic --cluster lkc-ro6ok7
$ confluent kafka topic list
```

# Modify configurations `cloud-consumer.properties / cloud-producer.properties`

Start the consumer

```bash
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaConsumerDemo config/cloud-consumer.properties
```

Start the producer

```bash
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaProducerDemo config/cloud-producer.properties
```

Consume messages with Confluent CLI

```bash
$ confluent kafka topic consume test-topic
```

You might need to set the api-key

```bash
$ confluent api-key use <KEY-NAME>
$ confluent api-key store <KEY-NAME> <SECRET>
$ confluent api-key list

  Current |       Key        |   Description   |  Owner   |     Owner Email      | Resource Type |  Resource  |       Created
----------+------------------+-----------------+----------+----------------------+---------------+------------+-----------------------
          | PFHFOT43NUYK47W3 | kafka-basic-key | u-422714 | altfatterz@gmail.com | kafka         | lkc-ro6ok7 | 2025-04-27T11:34:31Z
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
$ confluent kafka cluster delete lkc-ro6ok7
```

Resources:

https://developer.confluent.io/tutorials/creating-first-apache-kafka-producer-application/confluent.html


