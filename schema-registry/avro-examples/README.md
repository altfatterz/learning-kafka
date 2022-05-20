#### Avro Examples

Running Locally

```bash
$ docker compose up -d
```

```bash
CONTAINER ID   IMAGE                                                    COMMAND                  CREATED              STATUS                      PORTS                                        NAMES
bf0a641c0c7d   confluentinc/cp-enterprise-kafka:7.1.1-1-ubi8            "/etc/confluent/dock…"   About a minute ago   Up About a minute           9092/tcp, 0.0.0.0:19092->19092/tcp           kafka
98d680d648ba   confluentinc/cp-schema-registry:7.1.1-1-ubi8             "/etc/confluent/dock…"   About a minute ago   Up About a minute           0.0.0.0:8081->8081/tcp                       schema-registry
ef17fd5563bb   confluentinc/cp-enterprise-kafka:7.1.1-1-ubi8            "bash -c 'echo Waiti…"   About a minute ago   Exited (0) 33 seconds ago                                                create-topics
62b24e0adde1   confluentinc/cp-zookeeper:7.1.1-1-ubi8                   "/etc/confluent/dock…"   About a minute ago   Up About a minute           2888/tcp, 0.0.0.0:2181->2181/tcp, 3888/tcp   zookeeper
f7bbbb867695   confluentinc/cp-enterprise-control-center:7.1.1-1-ubi8   "/etc/confluent/dock…"   About a minute ago   Up About a minute           0.0.0.0:9021->9021/tcp                       control-center
b0664f414562   cnfltraining/training-tools:6.0                          "/bin/sh"                About a minute ago   Up About a minute                                                        tools
```

In Control-Center create the schema from the `resources/avro/schema.avsc` file.
If you create a message from Control-Center the consumer cannot parse it, returns: "unknown magic byte"

```bash
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroConsumerDemo config/local-consumer.properties
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroProducerDemo config/local-producer.properties
```

Confluent Cloud 

```bash
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroConsumerDemo config/cloud-consumer.properties
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroProducerDemo config/cloud-producer.properties
```

```bash
$ docker exec -it kafka bash
$ kafka-consumer-groups --bootstrap-server kafka:9092 --list
$ kafka-consumer-groups --bootstrap-server kafka:9092 --describe --group kafka-avro-local-consumer
$ kafka-consumer-groups --bootstrap-server kafka:9092 --group kafka-avro-local-consumer --reset-offsets --topic avro-demo:0 --to-offset 0
$ kafka-consumer-groups --bootstrap-server kafka:9092 --group kafka-avro-local-consumer --reset-offsets --topic avro-demo:0 --to-offset 0 --execute
$ kafka-consumer-groups --bootstrap-server kafka:9092 --delete --group kafka-avro-local-consumer
 

```


https://zoltanaltfatter.com/2020/01/02/schema-evolution-with-confluent-registry/





### Resources:

https://docs.confluent.io/current/installation/docker/image-reference.html#image-reference
https://github.com/simplesteph/kafka-stack-docker-compose/