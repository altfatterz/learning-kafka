Install the Kafka Connect Datagen connector

```bash
$ docker exec -u root connect confluent-hub install --no-prompt confluentinc/kafka-connect-datagen:0.4.0
$ docker compose restart connect
```

Execute the ksqlDB CLI:

```bash
$ docker exec -it tools bash
$ ksql http://ksqldb-server:8088
$ list topics;
$ list streams;
$ list tables;
$ list connectors;
$ list queries;
$ server
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

Check it the connectors from the `connect` REST interface:

```bash
$ docker exec -it tools bash
$ curl connect:8083/connectors | jq
$ curl connect:8083/connectors/pageviews-ksql-connector | jq
$ curl connect:8083/connectors/users-ksql-connector | jq
```

Create a stream

```bash
$ CREATE STREAM pageviews (viewtime BIGINT, userid VARCHAR, pageid VARCHAR) \
WITH (VALUE_FORMAT = 'AVRO', KAFKA_TOPIC = 'my-pageviews-topic');

$ describe pageviews;
$ describe extended pageviews;
$ SELECT * FROM pageviews EMIT CHANGES;
```

Create a table
```bash
$ CREATE TABLE users (registertime BIGINT, userid VARCHAR PRIMARY KEY, gender VARCHAR, regionid VARCHAR) \
WITH (VALUE_FORMAT = 'AVRO', KAFKA_TOPIC = 'my-users-topic');

$ describe users;
$ describe extended users;
$ SELECT * FROM users EMIT CHANGES;
```

Create a persistent query: (PAGEVIEWS_ENRICHED topic will be created)

```bash
$ CREATE STREAM pageviews_enriched AS \
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
$ SELECT * FROM pageviews_enriched EMIT CHANGES;
```

Customise the topic underneath:

```bash
$ CREATE STREAM pageviews_enriched2 WITH (KAFKA_TOPIC='pageviews_enriched', partitions = 1, replicas = 1) AS \
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

List queries
```bash
$ list queries
$ explain <query-id>
$ list queries extended
```

Persistent query with windowing example:

```bash
$ CREATE TABLE pageviews_count_by_region WITH (KAFKA_TOPIC='pageviews_count_by_region_topic') AS \
    SELECT gender, regionid, COUNT(*) AS total \
    FROM pageviews_enriched \
    WINDOW TUMBLING (SIZE 10 SECONDS) \
    GROUP BY gender, regionid \
    HAVING COUNT(*) > 1 \
    EMIT CHANGES;
```

Push query:

```bash
$ SELECT * FROM pageviews_count_by_region EMIT CHANGES;
```

Pull query:

```bash
$ SELECT * FROM pageviews_count_by_region \ 
WHERE KSQL_COL_0='FEMALE|+|Region_4' AND WINDOWSTART >= 1619552920000 AND WINDOWSTART <= 1619552930000;
```

Try to drop a stream, should fail because there are running queries against it.

```bash
$ drop stream pageviews_enriched
```

Terminate a query
```bash
$ terminate CTAS_PAGEVIEWS_COUNT_BY_REGION_5;
```

```bash
$ drop stream pageviews_enriched
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
