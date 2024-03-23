Start the cluster

```bash
$ docker compose up -d
```

Check:

```bash
$ open http://localhost:9021
```

Install the Kafka Connect Datagen connector

```bash
$ docker exec -u root kafka-connect confluent-hub install --no-prompt confluentinc/kafka-connect-datagen:0.6.5 
$ docker-compose restart kafka-connect
```

Execute the ksqlDB CLI:

```bash
$ docker exec -it ksqldb-cli bash
$ ksql http://ksqldb-server:8088
$ list topics;
$ list streams;
$ list tables;
$ list connectors;
$ list queries;
$ server
$ version
```

Create data in the `pageviews-ksql-connector` and `users-ksql-connector` connectors

```bash
$ CREATE SOURCE CONNECTOR `pageviews-ksql-connector` WITH( 
  'connector.class'='io.confluent.kafka.connect.datagen.DatagenConnector',
  "key.converter"='org.apache.kafka.connect.storage.StringConverter',
  "kafka.topic"='my-pageviews-topic',
  "quickstart"='pageviews',
  "max.interval"=1000,
  "iterations"=10000000,
  "tasks.max"='1');
```

```bash
$ CREATE SOURCE CONNECTOR `users-ksql-connector` WITH( 
  'connector.class'='io.confluent.kafka.connect.datagen.DatagenConnector',
  "key.converter"='org.apache.kafka.connect.storage.StringConverter',
  "kafka.topic"='my-users-topic',
  "quickstart"='users',
  "max.interval"=1000,
  "iterations"=10000000,
  "tasks.max"='1');
```

```bash
$ list connectors;
 Connector Name           | Type   | Class                                               | Status
-----------------------------------------------------------------------------------------------------------------------
 pageviews-ksql-connector | SOURCE | io.confluent.kafka.connect.datagen.DatagenConnector | RUNNING (1/1 tasks RUNNING)
 users-ksql-connector     | SOURCE | io.confluent.kafka.connect.datagen.DatagenConnector | RUNNING (1/1 tasks RUNNING)
-----------------------------------------------------------------------------------------------------------------------
```

```bash
$ print 'my-pageviews-topic' from beginning;
$ print 'my-users-topic' from beginning;
```


Check it the connectors from the `kafka-connect` REST interface:

```bash
$ docker exec -it tools bash
$ curl kafka-connect:8083/connectors | jq
$ curl kafka-connect:8083/connectors/pageviews-ksql-connector | jq
$ curl kafka-connect:8083/connectors/users-ksql-connector | jq
```

Create a stream

```bash
$ CREATE STREAM pageviews (viewtime BIGINT, userid VARCHAR, pageid VARCHAR) \
WITH (VALUE_FORMAT = 'AVRO', KAFKA_TOPIC = 'my-pageviews-topic');

$ describe pageviews;
$ describe pageviews extended;
$ SELECT * FROM pageviews EMIT CHANGES;
```

Create a table
```bash
$ CREATE TABLE users (registertime BIGINT, userid VARCHAR PRIMARY KEY, gender VARCHAR, regionid VARCHAR) \
WITH (VALUE_FORMAT = 'AVRO', KAFKA_TOPIC = 'my-users-topic');

$ describe users;
$ describe users extended;
$ SELECT * FROM users EMIT CHANGES;
```

Create a persistent query: (pageviews_enriched topic will be created with the provided customizations)

```bash
$ CREATE STREAM pageviews_enriched WITH (KAFKA_TOPIC='pageviews_enriched', partitions = 1, replicas = 1) AS \
    SELECT pv.userid AS userid, \
           pv.viewtime, \
           pv.pageid, \
           u.gender, \
           u.regionid \
    FROM pageviews pv \
      LEFT JOIN users u \
      ON pv.userid = u.userid \
    EMIT CHANGES;
```

```bash
$ list queries;
$ explain <query-id>;
$ SELECT * FROM pageviews_enriched EMIT CHANGES;
```

Persistent query with windowing example:

```bash
$ CREATE TABLE pageviews_count_by_region WITH (KAFKA_TOPIC='pageviews_count_by_region_topic') AS \
    SELECT regionid, COUNT(*) AS total \
    FROM pageviews_enriched \
    WINDOW TUMBLING (SIZE 10 SECONDS) \
    GROUP BY regionid \
    HAVING COUNT(*) > 1 \
    EMIT CHANGES;
```

Push query:

```bash
$ SELECT * FROM pageviews_count_by_region EMIT CHANGES;
```

Pull query:

```bash
$ SELECT * FROM pageviews_count_by_region WHERE TOTAL >= 10;
```

Try to drop a stream, should fail because there are running queries against it.

```bash
$ drop stream pageviews_enriched

Cannot drop PAGEVIEWS_ENRICHED.
The following streams and/or tables read from this source: [PAGEVIEWS_COUNT_BY_REGION].
You need to drop them before dropping PAGEVIEWS_ENRICHED.
```

Terminate a query. Persistent queries run continuously until they are explicitly terminated.
In client-server mode, exiting the CLI doesn't stop persistent queries, because the ksqlDB Server(s) continue to process the queries.
A non-persistent query can also be terminated by using Ctrl+C in the CLI.

```bash
$ terminate <CTAS_PAGEVIEWS_COUNT_BY_REGION-TODO-CHECK-ID>
$ terminate all;
```

```bash
$ drop table pageviews_count_by_region;
$ drop stream pageviews_enriched;
$ drop stream pageviews;
$ drop table users;
$ drop connector "users-ksql-connector";
$ drop connector "pageviews-ksql-connector";
```



------------------------------------------------------------------------------------------------------------------------
[Quick Start for Apache Kafka using Confluent Platform (Local)](https://docs.confluent.io/platform/current/quickstart/ce-quickstart.html)

Put the followings into `.zshrc`

```bash
export CONFLUENT_HOME=~/apps/confluent-6.1.1
export PATH="$CONFLUENT_HOME/bin:$PATH"

export CONFLUENT_CLI_HOME=~/apps/confluent-cli
export PATH="$CONFLUENT_CLI_HOME/bin:$PATH"
```

Manage a local Confluent Platform development environment.

```bash
$ confluent local current   // Get the path of the current Confluent run.
$ confluent local destroy   // Delete the data and logs for the current Confluent run.
$ confluent local version   // Print the Confluent Platform version.
```

Manage Confluent Platform services.

```bash
$ confluent local services list
$ confluent local services status
$ confluent local services start
$ confluent local services stop
```
