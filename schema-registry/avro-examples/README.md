#### Avro Examples

Running Locally

```bash
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroConsumerDemo config/local-consumer.properties

$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroProducerDemo config/local-producer.properties
```

Confluent Cloud 

```bash
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroConsumerDemo config/cloud-consumer.properties

java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroProducerDemo config/cloud-producer.properties
```


https://zoltanaltfatter.com/2020/01/02/schema-evolution-with-confluent-registry/

```bash
$ docker-compose up -d
```

```bash
docker ps -a

CONTAINER ID   IMAGE                    COMMAND                  CREATED         STATUS         PORTS                                                                                                                                                  NAMES
f803ba38f0ba   lensesio/fast-data-dev   "/usr/bin/dumb-init …"   2 minutes ago   Up 2 minutes   0.0.0.0:2181->2181/tcp, 0.0.0.0:3030->3030/tcp, 0.0.0.0:8081-8083->8081-8083/tcp, 0.0.0.0:9092->9092/tcp, 0.0.0.0:9581-9585->9581-9585/tcp, 3031/tcp   avro-examples_kafka-cluster_1
```

Open in browser

http://localhost:3030/


```bash
$ docker exec -it fast-data-dev bash
```

#### Insomnia

Great HTTP client:
https://insomnia.rest/

```bash
$ brew install insomnia
```


### Resources:

https://docs.confluent.io/current/installation/docker/image-reference.html#image-reference
https://github.com/simplesteph/kafka-stack-docker-compose/