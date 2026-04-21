### Flink with Kafka using Confluent Cloud

```bash
$ brew install confluentinc/tap/cli
$ confluent login --save
# install the quickstart plugin
$ confluent plugin install confluent-flink-quickstart
# creates a "flink101_environment", 
# creates a "flink101_kafka-cluster" kafka cluster, 
# creates a "flink101" compute pool
# Schema Registry enabled
# --max-cfu 1 (Confluent Cloud Unit) upper bound on how many resources the compute pool can consume
$ confluent flink quickstart \
    --name flink101 \
    --max-cfu 10 \
    --region us-central1 \
    --cloud gcp
    
 
$ confluent environment list
  Current |     ID     |         Name         | Stream Governance Package
----------+------------+----------------------+----------------------------
...
  *       | env-7dr182 | flink101_environment | ESSENTIALS

Flink SQL> 
CREATE TABLE simple_table (
  `key` STRING,
  `value` STRING
);

Flink SQL> show tables
+--------------+    
|  Table Name  |    
+--------------+    
| simple_table |   
+--------------+

# creating a table in Flink automatically creates the associated topic and schema.
$ confluent kafka topic list
      Name     | Internal | Replication Factor | Partition Count
---------------+----------+--------------------+------------------
  simple_table | false    |                  3 |               6
  
$ confluent schema-registry schema list
    ID   |      Subject       | Version
---------+--------------------+----------
  100001 | simple_table-value |       1
  
# check schema definition  
$ confluent schema-registry schema describe 100001  
  
# Creating a topic you can see it as a table already in Flink SQL  
$ confluent kafka topic create test-topic
Flink SQL> show tables
+--------------+
|  Table Name  |
+--------------+
| simple_table |
| test-topic   |
+--------------+
```

- `In Confluent Cloud for Apache Flink`, a `table` is a combination of some or all of these components:

- some metadata
- schemas stored in Schema Registry
- data stored in a Kafka topic
- data stored in Apache Iceberg (using `Tableflow`)

```bash
$ confluent api-key create --resource <KAFKA_CLUSTER_ID>
$ confluent api-key use <API_KEY>
# try to consume from the topic, meanwhile in Flink SQL insert data
$ confluent kafka topic consume --value-format avro --from-beginning simple_table

Flink SQL> INSERT INTO simple_table VALUES ('foo','one'), ('foo', 'two');

Creating statement: cli-2026-04-21-211438-8c9adafe-3c46-4e0a-a45b-459ac24b70d2
> Statement successfully submitted.
Waiting for statement to be ready. Statement phase: PENDING.
Waiting for statement to be ready. Statement phase is PENDING. (Timeout 6s/600s)
Statement phase is RUNNING.
Listening for execution errors. Press Enter to detach.
Finished statement execution. Statement phase: COMPLETED.
The server returned empty rows for this statement.

# in confluent cli consumer window we see 
Starting Kafka Consumer. Use Ctrl-C to exit.
{"key":{"string":"foo"},"value":{"string":"one"}}
{"key":{"string":"foo"},"value":{"string":"two"}}


```

# only single "confluent" connector

- There is a single `confluent` connector, which can be configured to use various `changelog` modes.

```bash
# check full specification
# created 6 partitions / value format set to avro / connector set to "confluent"
Flink SQL> show create table simple_table;

+-------------------------------------------------------------------+
|                         SHOW CREATE TABLE                         |
+-------------------------------------------------------------------+
| CREATE TABLE `flink101_environment`.`lkc-2vr0m1`.`simple_table` ( |
|   `key` VARCHAR(2147483647),                                      |
|   `value` VARCHAR(2147483647)                                     |
| )                                                                 |
| DISTRIBUTED INTO 6 BUCKETS                                        |
| WITH (                                                            |
|   'changelog.mode' = 'append',                                    |
|   'connector' = 'confluent',                                      |
|   'kafka.cleanup-policy' = 'delete',                              |
|   'kafka.compaction.time' = '0 ms',                               |
|   'kafka.max-message-size' = '2097164 bytes',                     |
|   'kafka.message-timestamp-type' = 'create-time',                 |
|   'kafka.retention.size' = '0 bytes',                             |
|   'kafka.retention.time' = '0 ms',                                |
|   'scan.bounded.mode' = 'unbounded',                              |
|   'scan.startup.mode' = 'earliest-offset',                        |
|   'value.format' = 'avro-registry'                                |
| )                                                                 |
|                                                                   |
+-------------------------------------------------------------------+

Flink SQL> 
CREATE TABLE updating_table (
  `key` STRING PRIMARY KEY NOT ENFORCED,
  `value` STRING
) WITH (
  'changelog.mode' = 'upsert'
);
  
# setting the changelog to upsert, automatically to compact cleanup policy  
Flink SQL> show create table updating_table;

+---------------------------------------------------------------------+
|                          SHOW CREATE TABLE                          |
+---------------------------------------------------------------------+
| CREATE TABLE `flink101_environment`.`lkc-2vr0m1`.`updating_table` ( |
|   `key` VARCHAR(2147483647) NOT NULL,                               |
|   `value` VARCHAR(2147483647),                                      |
|   CONSTRAINT `PK_key` PRIMARY KEY (`key`) NOT ENFORCED              |
| )                                                                   |
| DISTRIBUTED BY HASH(`key`) INTO 6 BUCKETS                           |
| WITH (                                                              |
|   'changelog.mode' = 'upsert',                                      |
|   'connector' = 'confluent',                                        |
|   'kafka.cleanup-policy' = 'compact',                               |
|   'kafka.compaction.time' = '7 d',                                  |
|   'kafka.max-message-size' = '2097164 bytes',                       |
|   'kafka.message-timestamp-type' = 'create-time',                   |
|   'kafka.retention.size' = '0 bytes',                               |
|   'kafka.retention.time' = '0 ms',                                  |
|   'key.format' = 'avro-registry',                                   |
|   'scan.bounded.mode' = 'unbounded',                                |
|   'scan.startup.mode' = 'earliest-offset',                          |
|   'value.format' = 'avro-registry'                                  |
| )                                                                   |
|                                                                     |
+---------------------------------------------------------------------+
 
```

More SQL examples: https://docs.confluent.io/cloud/current/flink/reference/sql-examples.html

- Kafka topics and schemas are always in sync with Flink
- Any topic created in Kafka is visible directly as a table in Flink, and any table created in Flink is visible as a topic in Kafka.
- Effectively, Flink provides a SQL interface on top of Confluent Cloud.

  Kafka ----------- Flink
- Environment ----> Catalog
- Cluster     ----> Database
- Topic + Schema -> Table

```bash
CREATE TABLE orders(
 order_id VARCHAR,
 product_id VARCHAR,
 price DECIMAL(10,2),
 customer_id VARCHAR,
 PRIMARY KEY (order_id) NOT ENFORCED
);

# topic orders created
# schema orders-key is created with order_id field using Avro
# schema orders-value created with orders-value - all fields except orders-key using Avro
  
+------------------------------------------------------------------+
|                        SHOW CREATE TABLE                         |
+------------------------------------------------------------------+
| CREATE TABLE `flink101_environment`.`lkc-2vr0m1`.`orders` (      |
|   `order_id` VARCHAR(2147483647) NOT NULL,                       |
|   `product_id` VARCHAR(2147483647),                              |
|   `price` DECIMAL(10, 2),                                        |
|   `customer_id` VARCHAR(2147483647),                             |
|   CONSTRAINT `PK_order_id` PRIMARY KEY (`order_id`) NOT ENFORCED |
| )                                                                |
| DISTRIBUTED BY HASH(`order_id`) INTO 6 BUCKETS                   |
| WITH (                                                           |
|   'changelog.mode' = 'upsert',                                   |
|   'connector' = 'confluent',                                     |
|   'kafka.cleanup-policy' = 'compact',                            |
|   'kafka.compaction.time' = '7 d',                               |
|   'kafka.max-message-size' = '2097164 bytes',                    |
|   'kafka.message-timestamp-type' = 'create-time',                |
|   'kafka.retention.size' = '0 bytes',                            |
|   'kafka.retention.time' = '0 ms',                               |
|   'key.format' = 'avro-registry',                                |
|   'scan.bounded.mode' = 'unbounded',                             |
|   'scan.startup.mode' = 'earliest-offset',                       |
|   'value.format' = 'avro-registry'                               |
| )                                                                |
|                                                                  |
+------------------------------------------------------------------+  
```

More info here: https://docs.confluent.io/cloud/current/flink/overview.html

```bash
  
# cleanup 
$ confluent environment delete env-7dr182
  
```