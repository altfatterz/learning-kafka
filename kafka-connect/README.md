Start up 
```bash
$ docker compose up -d
```

# Kafka Connect Datagen Connector 

* https://github.com/confluentinc/kafka-connect-datagen
* Quickstarts: https://github.com/confluentinc/kafka-connect-datagen/tree/master/src/main/resources
`
# Install Datagen connector via [confluent-hub](https://www.confluent.io/hub/)

```bash
$ docker compose exec -u root connect confluent-hub install confluentinc/kafka-connect-datagen:0.4.0
The component can be installed in any of the following Confluent Platform installations:
  1. / (installed rpm/deb package)
  2. / (where this tool is installed)
Choose one of these to continue the installation (1-2): 1
Do you want to install this into /usr/share/confluent-hub-components? (yN) y


Component's license:
Apache License 2.0
https://www.apache.org/licenses/LICENSE-2.0
I agree to the software license agreement (yN) y

Downloading component Kafka Connect Datagen 0.4.0, provided by Confluent, Inc. from Confluent Hub and installing into /usr/share/confluent-hub-components
Detected Worker's configs:
  1. Standard: /etc/kafka/connect-distributed.properties
  2. Standard: /etc/kafka/connect-standalone.properties
  3. Standard: /etc/schema-registry/connect-avro-distributed.properties
  4. Standard: /etc/schema-registry/connect-avro-standalone.properties
  5. Used by Connect process with PID : /etc/kafka-connect/kafka-connect.properties
Do you want to update all detected configs? (yN) y

Adding installation directory to plugin path in the following files:
  /etc/kafka/connect-distributed.properties
  /etc/kafka/connect-standalone.properties
  /etc/schema-registry/connect-avro-distributed.properties
  /etc/schema-registry/connect-avro-standalone.properties
  /etc/kafka-connect/kafka-connect.properties

Completed
```

Restart the connect worker

```bash
$ docker compose restart connect
```

From the tools image to the followings:
```bash
$ docker exec -it tools bash
```

# connectors 

Create the connector:

```bash
echo '
{
  "name": "DatagenConnector-pageviews",
  "config": {
        "connector.class": "io.confluent.kafka.connect.datagen.DatagenConnector",
        "iterations": "10000000",
        "kafka.topic": "pageviews",
        "max.interval": "1000",
        "quickstart": "pageviews",
        "tasks.max": "1"
    } 
}' | http post http://connect:8083/connectors 
```

Useful commands:

```bash
$ http http://connect:8083/connectors
[
    "DatagenConnector-pageviews"
]
```

```bash
$ http http://connect:8083/connectors/DatagenConnector-pageviews
{
    "config": {
        "connector.class": "io.confluent.kafka.connect.datagen.DatagenConnector",
        "iterations": "10000000",
        "kafka.topic": "pageviews",
        "max.interval": "1000",
        "name": "DatagenConnector-pageviews",
        "quickstart": "pageviews",
        "tasks.max": "1"
    },
    "name": "DatagenConnector-pageviews",
    "tasks": [
        {
            "connector": "DatagenConnector-pageviews",
            "task": 0
        }
    ],
    "type": "source"
}
```

```bash
$ http http://connect:8083/connectors/DatagenConnector-pageviews/status
{
    "connector": {
        "state": "RUNNING",
        "worker_id": "connect:8083"
    },
    "name": "DatagenConnector-pageviews",
    "tasks": [
        {
            "id": 0,
            "state": "RUNNING",
            "worker_id": "connect:8083"
        }
    ],
    "type": "source"
}
```

```bash
$ http http://connect:8083/connectors?expand=status&expand=info
{
    "DatagenConnector-pageviews": {
        "status": {
            "connector": {
                "state": "RUNNING",
                "worker_id": "connect:8083"
            },
            "name": "DatagenConnector-pageviews",
            "tasks": [
                {
                    "id": 0,
                    "state": "RUNNING",
                    "worker_id": "connect:8083"
                }
            ],
            "type": "source"
        }
    }
}
```

```bash
$ http delete http://connect:8083/connectors/DatagenConnector-pageviews
```

# connector-plugins

```bash
$ http http://connect:8083/connector-plugins
[
    {
        "class": "io.confluent.kafka.connect.datagen.DatagenConnector",
        "type": "source",
        "version": "null"
    },
    {
        "class": "org.apache.kafka.connect.mirror.MirrorCheckpointConnector",
        "type": "source",
        "version": "1"
    },
    {
        "class": "org.apache.kafka.connect.mirror.MirrorHeartbeatConnector",
        "type": "source",
        "version": "1"
    },
    {
        "class": "org.apache.kafka.connect.mirror.MirrorSourceConnector",
        "type": "source",
        "version": "1"
    }
]```




```bash
$ docker exec -it tools bash
kafka-avro-console-consumer \
--bootstrap-server kafka:9092 \
--property schema.registry.url=http://schema-registry:8081 \
--topic pageviews \
--property print.key=true \
--key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
```


# Sink

```bash
$ psql -h localhost -p 5432 -d postgres -U postgres
Password for user postgres: mysecretpassword
```




Resources:

* Introducing `Confluent Hub`:https://www.confluent.io/blog/introducing-confluent-hub/
* `Confluent Hub`: https://www.confluent.io/hub/
* Kafka Connect official documentation: https://docs.confluent.io/current/connect/index.html
* The Simplest Useful Kafka Connect Data Pipeline In The World:
  1. https://www.confluent.io/blog/simplest-useful-kafka-connect-data-pipeline-world-thereabouts-part-1/
  2. https://www.confluent.io/blog/the-simplest-useful-kafka-connect-data-pipeline-in-the-world-or-thereabouts-part-2/
  3. https://www.confluent.io/blog/simplest-useful-kafka-connect-data-pipeline-world-thereabouts-part-3/
* https://developer.confluent.io/courses/kafka-connect/intro/
