#### Avro Examples

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
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroProducerDemo config/local-producer.properties
```

Start the consumer:

```bash
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroConsumerDemo config/local-consumer.properties
````


Produce the following message in the Control Center:

```json
{"first_name":"John","last_name":"Doe","accounts":[{"iban":"CH93 0076 2011 6238 5295 7","type":"CHECKING"},{"iban":"CH93 0076 2011 6238 5295 8","type":"SAVING"}],"settings":{"e-billing-enabled":true,"push-notification-enabled":false},"signup_timestamp":"2022-05-21T10:41:24.117Z","phone_number":null}
```

In the consumer you will see an error message:

```bash
org.apache.kafka.common.errors.SerializationException: Unknown magic byte!
```

Turns out the produced message is not serialised via Avro, is a simple JSON message and serialised via StringSerialiser.
Try to read it out using:

```bash
$ kafka-console-consumer --bootstrap-server kafka:9092 --topic avro-demo --partition 0 --offset <check-which-offset>
```

To read via Avro use:

```bash
$ docker exec -it schema-registry bash
$ kafka-avro-console-consumer --bootstrap-server kafka:9092 --topic avro-demo --from-beginning
```

# avro-tools
$ java -jar avro-tools-1.11.3.jar
```bash
----------------
Available tools:
    canonical  Converts an Avro Schema to its canonical form
          cat  Extracts samples from files
      compile  Generates Java code for the given schema.
       concat  Concatenates avro files without re-compressing.
        count  Counts the records in avro files or folders
  fingerprint  Returns the fingerprint for the schemas.
   fragtojson  Renders a binary-encoded Avro datum as JSON.
     fromjson  Reads JSON records and writes an Avro data file.
     fromtext  Imports a text file into an avro data file.
      getmeta  Prints out the metadata of an Avro data file.
    getschema  Prints out schema of an Avro data file.
          idl  Generates a JSON schema from an Avro IDL file
 idl2schemata  Extract JSON schemata of the types from an Avro IDL file
       induce  Induce schema/protocol from Java class/interface via reflection.
   jsontofrag  Renders a JSON-encoded Avro datum as binary.
       random  Creates a file with randomly generated instances of a schema.
      recodec  Alters the codec of a data file.
       repair  Recovers data from a corrupt Avro Data file
  rpcprotocol  Output the protocol of a RPC service
   rpcreceive  Opens an RPC Server and listens for one message.
      rpcsend  Sends a single RPC message.
       tether  Run a tethered mapreduce job.
       tojson  Dumps an Avro data file as JSON, record per line or pretty.
       totext  Converts an Avro data file to a text file.
     totrevni  Converts an Avro data file to a Trevni file.
  trevni_meta  Dumps a Trevni file's metadata as JSON.
trevni_random  Create a Trevni file filled with random instances of a schema.
trevni_tojson  Dumps a Trevni file as JSON.
```

```bash
$ java -jar avro-tools-1.11.3.jar fromjson --schema-file ./src/main/resources/avro/schema.avsc customer.json > customer.avro
```

Get back the schema:

```bash
$ java -jar avro-tools-1.11.3.jar getschema customer.avro
```

# Confluent Cloud 

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