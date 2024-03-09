#### Avro Examples

# Running Locally

```bash
$ docker compose up -d
```

Create the topic

```bash
$ kafka-topics --bootstrap-server localhost:29092 --create --topic avro-demo --partitions 1 --replication-factor 1
```

Register the new schema 
```bash
jq '. | {schema: tojson}' src/main/resources/avro/schema.avsc | http post :8081/subjects/avro-demo-value/versions  
```

Get back the latest version
```bash
http :8081/subjects/avro-demo-value/versions/latest
```

View subjects / schemas

```bash
$ http :8081/subjects
$ http :8081/schemas
```

If needed perform a soft delete or hard delete (appending `?permanent=true`) of all versions of the schema.

```bash
$ http delete :8081/subjects/avro-demo-value
$ http delete :8081/subjects/avro-demo-value?permanent=true
```

Check syntax errors:
```bash
$ mvn io.confluent:kafka-schema-registry-maven-plugin:validate
```

Test backward compatibility by adding only a field without a default value (`schema2a.avsc`)

```bash
$ mvn io.confluent:kafka-schema-registry-maven-plugin:test-compatibility
```

You will get a failure like:

```bash [{errorType:'READER_FIELD_MISSING_DEFAULT_VALUE', description:'The field 'middle_name' at path '/fields/2' in the new schema has no default value and is missing in the old schema', additionalInfo:'middle_name'}, {oldSchemaVersion: 1}, {oldSchema: '{"type":"record","name":"NewCustomerCreatedEvent","namespace":"com.github.altfatterz.avro","fields":[{"name":"first_name","type":"string","doc":"the first name of the customer"},{"name":"last_name","type":"string","doc":"the last name of the customer"},{"name":"accounts","type":{"type":"array","items":{"type":"record","name":"Account","fields":[{"name":"iban","type":"string"},{"name":"type","type":{"type":"enum","name":"AccountType","symbols":["SAVING","CHECKING","JOINT"]}}]}}},{"name":"settings","type":{"type":"map","values":"boolean"}},{"name":"signup_timestamp","type":{"type":"long","logicalType":"timestamp-millis"},"doc":"Customer Signup Timestamp"},{"name":"phone_number","type":["null","string"],"doc":"the phone number of the customer","default":null}]}'}, {validateFields: 'false', compatibility: 'BACKWARD'}]

```

Fix it by adding a default value to the newly added field (`schema2b.avsc`)

```bash
$ mvn io.confluent:kafka-schema-registry-maven-plugin:test-compatibility
```

Register it

```bash
$ mvn io.confluent:kafka-schema-registry-maven-plugin:register
```

You can set the compatibility at subject level like

```bash
echo '{"compatibility": "FORWARD"}' | http put :8081/config/avro-demo-value
```

The compatibility levels can be:

- BACKWARD (default)
- FORWARD
- FULL
- NONE
- BACKWARD_TRANSITIVE
- FORWARD_TRANSITIVE
- FULL_TRANSITIVE

More details about the `kafka-schema-registry-maven-plugin`:
https://docs.confluent.io/platform/current/schema-registry/develop/maven-plugin.html 

More details about the Schema Registry Rest API:
https://docs.confluent.io/cloud/current/sr/sr-rest-apis.html

Build the avro-examples

```bash
$ mvn clean package
```

```bash
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroProducerDemo config/local-producer.properties
```

Start the consumer:

```bash
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroConsumerDemo config/local-consumer.properties
````

Read via `kafka-avro-console-consumer`

```bash
$ kafka-avro-console-consumer --bootstrap-server localhost:29092 --topic avro-demo
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

# Confluent Cloud  -- TODO to refine

```bash
$ confluent login --save --prompt
$ confluent environment list
$ confluent kafka cluster list
$ confluent kafka cluster use lkc-8ggg70
$ confluent kafka topic create avro-demo
```

Get the subjects
```bash
$ http -a <CHECK-1PASSWORD>:<CHECK-1PASSWORD> https://psrc-9zg5y.europe-west3.gcp.confluent.cloud/subjects
```

Get the latest schema of the `orders` topic versions:
```bash
$ http -a <CHECK-1PASSWORD>:<CHECK-1PASSWORD> https://psrc-9zg5y.europe-west3.gcp.confluent.cloud/subjects/orders-value/versions/latest
```

Run the producer which should fail since the schema has to be registered first since we set the properties

```bash
use.latest.version=true
auto.register.schemas=false
```

```bash
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroProducerDemo config/cloud-producer.properties
```

Register the schema

```bash
$ confluent schema-registry schema create --subject avro-demo-value --schema src/main/resources/avro/schema.avsc --type avro 
```

Try to produce again, it should work

```bash
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroProducerDemo config/cloud-producer.properties
```

Start the consumer also:

```bash
$ java -cp target/avro-examples-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.github.altfatterz.KafkaAvroConsumerDemo config/cloud-consumer.properties
```

### Resources:

https://docs.confluent.io/current/installation/docker/image-reference.html#image-reference
https://github.com/simplesteph/kafka-stack-docker-compose/

https://github.com/confluentinc/learn-kafka-courses/

https://docs.confluent.io/platform/current/schema-registry/develop/api.html#schemas

https://github.com/confluentinc/schema-registry/issues/2479

https://docs.confluent.io/platform/7.6/schema-registry/index.html

Good tutorial:
https://docs.confluent.io/platform/current/schema-registry/schema_registry_onprem_tutorial.html
