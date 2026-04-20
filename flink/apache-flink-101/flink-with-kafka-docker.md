### Flink with Kafka using Docker

- Open source Flink has two distinct Kafka connectors: `kafka` and `upsert-kafka`

```bash
$ docker compose up --build -d
$ docker ps

# Once the containers are running, start Flink SQL client
$ docker compose run sql-client

# create a table, backed by a kafka topic named "append"
Flink SQL >
CREATE TABLE json_table (
    `key` STRING,
    `value` STRING
) WITH (
    'connector' = 'kafka',
    'topic' = 'append',
    'properties.bootstrap.servers' = 'broker:9092',
    'format' = 'json',
    'scan.startup.mode' = 'earliest-offset'
);
  
  
Flink SQL> show tables;
+------------+
| table name |
+------------+
| json_table |
+------------+  

# topic won't have been created. But once you write into it, the topic will be  
$ docker compose exec -it kcat kcat -b broker:9092 -L  

Flink SQL > INSERT INTO json_table VALUES ('foo','one'), ('foo', 'two');

# check the topic again
$ docker compose exec -it kcat kcat -b broker:9092 -L

Metadata for all topics (from broker 1: broker:9092/1):
 1 brokers:
  broker 1 at broker:9092 (controller)
 1 topics:
  topic "append" with 3 partitions:
    partition 0, leader 1, replicas: 1, isrs: 1
    partition 1, leader 1, replicas: 1, isrs: 1
    partition 2, leader 1, replicas: 1, isrs: 1
 
# read data 
Flink SQL > SELECT * FROM json_table;
----------------------------+----------------------------+
                        key |                      value |
----------------------------+----------------------------+
                        foo |                        one |
                        foo |                        two |

# read the data from a topic
$ docker compose exec -it kcat kcat -b broker:9092 -C -t append -f 'Partition: %p | Offset: %o | Data: %s\n'

# create another table from the same topic 
Flink SQL> 
CREATE TABLE raw_table (
    `data` STRING
) WITH (
    'connector' = 'kafka',
    'topic' = 'append',
    'properties.bootstrap.servers' = 'broker:9092',
    'format' = 'raw',
    'scan.startup.mode' = 'earliest-offset'
);
  
Flink SQL> show tables;
+------------+
| table name |
+------------+
| json_table |
|  raw_table |

Flink SQL> SELECT * FROM raw_table;
-----------------------------+
                        data |
-----------------------------+
 {"key":"foo","value":"one"} |
 {"key":"foo","value":"two"} | 
 
# Flink SQL is designed around the idea of processing changelog streams.
# The simplest form of changelog stream is an insert-only, or append stream, each new message is a new record being appended at the end of the stream
Flink SQL > SET 'sql-client.execution.result-mode' = 'tableau'; 
Flink SQL > SELECT * FROM json_table;
+----+----------------------------+----------------------------+
| op |                        key |                      value |
+----+----------------------------+----------------------------+
| +I |                        foo |                        one |
| +I |                        foo |                        two |

# changing the connector to "upsert-kafka"
Flink SQL > 
CREATE TABLE updating_table (
    `key` STRING PRIMARY KEY NOT ENFORCED,
    `value` STRING
) WITH (
    'connector' = 'upsert-kafka',
    'topic' = 'update',
    'properties.bootstrap.servers' = 'broker:9092',
    'key.format' = 'json',
    'value.format' = 'json'
);

Flink SQL > INSERT INTO updating_table VALUES ('foo','one'), ('foo', 'two');

# Here -U and +U represent the update of the value associated with foo in two steps:
# an UPDATE_BEFORE that deletes the old value, 
# and an UPDATE_AFTER that inserts the new value. 
Flink SQL > SELECT * FROM updating_table;
+----+--------------------------------+--------------------------------+
| op |                            key |                          value |
+----+--------------------------------+--------------------------------+
| +I |                            foo |                            one |
| -U |                            foo |                            one |
| +U |                            foo |                            two |

# set back to "table" mode
Flink SQL > SET 'sql-client.execution.result-mode' = 'table';
Flink SQL > SELECT * FROM updating_table;
----------------------------+----------------------------+
                        key |                      value |
----------------------------+----------------------------+
                        foo |                        two |

# table / tableau / changelog modes                                                 
Flink SQL > SET 'sql-client.execution.result-mode' = 'changelog';

# if for some reason the Flink SQL shell quits we lose the table definitions, two options
- 1. Use a Persistent Catalog example:

CREATE CATALOG my_hive WITH ('type'='hive');
USE CATALOG my_hive;      

- 2. Use an Initialization SQL File

# CREATE TABLE statements into a .sql
./sql-client.sh -i init.sql
                             
```