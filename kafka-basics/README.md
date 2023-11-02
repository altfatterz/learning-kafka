#### Kafka Basics

#### Start kafka with control center 

```bash
$ docker compose up -d
```

#### Create a topic

```bash
$ docker exec -it broker bash
$ kafka-topics --bootstrap-server broker:9092 --create --topic test-topic --partitions 3 --replication-factor 1
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

### Running with Confluent Cloud

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


