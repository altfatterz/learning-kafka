#### Protobuf Examples

# Running Locally

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

Build the example:

```bash
$ cd /producer-examples
$ mvn clean install
```

Start the producer, notice that it will connect to the Schema Registry and will create the schema.

```bash
$ cd producer-protobuf
$ java -cp target/producer-protobuf-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaProtobufProducerDemo config/local-producer.properties
```

Start the consumer:

```bash
$ cd consumer-protobuf
$ java -cp target/consumer-protobuf-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaProtobufConsumerDemo config/local-consumer.properties
```

# Confluent Cloud

```bash
$ cd producer-protobuf
$ java -cp target/producer-protobuf-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaProtobufProducerDemo config/cloud-producer.properties
```

Start the consumer:

```bash
$ cd consumer-protobuf
$ java -cp target/consumer-protobuf-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaProtobufConsumerDemo config/cloud-consumer.properties
```

```bash
$ docker exec -it kafka bash
$ kafka-consumer-groups --bootstrap-server kafka:9092 --list
$ kafka-consumer-groups --bootstrap-server kafka:9092 --describe --group kafka-avro-local-consumer
$ kafka-consumer-groups --bootstrap-server kafka:9092 --group kafka-avro-local-consumer --reset-offsets --topic avro-demo:0 --to-offset 0
$ kafka-consumer-groups --bootstrap-server kafka:9092 --group kafka-avro-local-consumer --reset-offsets --topic avro-demo:0 --to-offset 0 --execute
$ kafka-consumer-groups --bootstrap-server kafka:9092 --delete --group kafka-avro-local-consumer
```

# Schema Registry

```bash
http :8081/subjects
[
    "avro-demo-value",
    "protobuf-demo-value"
]
```

```bash
http :8081/subjects/protobuf-demo-value/versions
[
  1
]
```

```bash
http :8081/subjects/protobuf-demo-value/versions/1
{
    "id": 2,
    "schema": "syntax = \"proto3\";\n\noption java_package = \"com.example.model.Customer\";\n\nmessage Customer {\n  int32 id = 1;\n  string firstName = 2;\n  string lastName = 3;\n  string email = 4;\n  repeated .Customer.PhoneNumber phones = 6;\n\n  message PhoneNumber {\n    string number = 1;\n    .Customer.PhoneType type = 2;\n  }\n  enum PhoneType {\n    MOBILE = 0;\n    HOME = 1;\n    WORK = 2;\n  }\n}\n",
    "schemaType": "PROTOBUF",
    "subject": "protobuf-demo-value",
    "version": 1
}
```

```bash
$ http delete :8081/subjects/protobuf-demo-value/versions/1
```

Registered schema types:

```bash
$ http :8081/schemas/types
[
    "JSON",
    "PROTOBUF",
    "AVRO"
]
```

# kafka-protobuf-console-producer

Create a `t1-p` topic
```bash
$ docker exec kafka kafka-topics --bootstrap-server kafka:9092 --create --topic t1-p --partitions 1 --replication-factor 1
```

```bash
$ docker exec -it schema-registry bash
```

The command line Protobuf producer will convert the JSON object to a Protobuf message (using the schema specified in <value.schema>) 
and then use an underlying serializer to serialize the message to the Kafka topic t1-p.

```bash
$ kafka-protobuf-console-producer --bootstrap-server kafka:9092 --topic t1-p \
--property schema.registry.url=http://schema-registry:8081 \
--property value.schema='syntax = "proto3"; message MyRecord { string f1 = 1; }'
```

#kafka-protobuf-console-consumer

```bash
$ kafka-protobuf-console-consumer --bootstrap-server kafka:9092 --topic t1-p --from-beginning \
--property schema.registry.url=http://schema-registry:8081 
```

```bash
http :8081/subjects/t1-p-value/versions/latest
{
    "id": 3,
    "schema": "syntax = \"proto3\";\n\nmessage MyRecord {\n  string f1 = 1;\n}\n",
    "schemaType": "PROTOBUF",
    "subject": "t1-p-value",
    "version": 1
}
```

```bash
http :8081/subjects/t1-p-value/versions/1/schema
syntax = "proto3";

message MyRecord {
  string f1 = 1;
}
```





More resources:

https://lenses.io/blog/2022/03/protobuf-to-kafka/