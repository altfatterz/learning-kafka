#### Kafka Basics

https://zoltanaltfatter.com/2019/11/23/kafka-basics

Running locally:

```bash
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaConsumerDemo config/local-consumer.properties
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaProducerDemo config/local-producer.properties
```

Running with Confluent Cloud Settings:

```bash
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaConsumerDemo config/cloud-consumer.properties
$ java -cp target/kafka-basics-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaProducerDemo config/cloud-producer.properties
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




```bash
$ confluent kafka topic produce test-topic --api-key --api-secret
$ confluent kafka topic 
```




Resources:

https://developer.confluent.io/get-started/java#build-producer


Schema registry:

https://psrc-o268o.eu-central-1.aws.confluent.cloud
https://psrc-o268o.eu-central-1.aws.confluent.cloud

https://psrc-d9vg7.europe-west3.gcp.confluent.cloud


