#### JSON Schema Examples

# Running Locally

```bash
$ docker compose up -d
```

```bash
CONTAINER ID   IMAGE                                                    COMMAND                  CREATED              STATUS                      PORTS                                        NAMES
bf0a641c0c7d   confluentinc/cp-enterprise-kafka:7.3.1            "/etc/confluent/dock…"   About a minute ago   Up About a minute           9092/tcp, 0.0.0.0:19092->19092/tcp           kafka
98d680d648ba   confluentinc/cp-schema-registry:7.3.1             "/etc/confluent/dock…"   About a minute ago   Up About a minute           0.0.0.0:8081->8081/tcp                       schema-registry
ef17fd5563bb   confluentinc/cp-enterprise-kafka:7.3.1            "bash -c 'echo Waiti…"   About a minute ago   Exited (0) 33 seconds ago                                                create-topics
62b24e0adde1   confluentinc/cp-zookeeper:7.3.1                   "/etc/confluent/dock…"   About a minute ago   Up About a minute           2888/tcp, 0.0.0.0:2181->2181/tcp, 3888/tcp   zookeeper
f7bbbb867695   confluentinc/cp-enterprise-control-center:7.3.1   "/etc/confluent/dock…"   About a minute ago   Up About a minute           0.0.0.0:9021->9021/tcp                       control-center
b0664f414562   cnfltraining/training-tools:6.0                          "/bin/sh"                About a minute ago   Up About a minute                                                        tools
```

Start the producer, notice that it will connect to the Schema Registry and will create the schema.

```bash
$ java -cp target/json-schema-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaJsonSchemaProducerDemo config/local-producer.properties
```

Start the consumer:

```bash
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaJsonSchemaConsumerDemo config/local-consumer.properties
````




Resources:

* https://docs.confluent.io/platform/current/schema-registry/serdes-develop/serdes-json.html

