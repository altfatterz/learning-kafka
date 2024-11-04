Apache Flink Quick Start on Confluent Cloud

```bash
$ confluent flink
Usage:  
  confluent flink [command]
Available Commands:
  compute-pool      Manage Flink compute pools.
  connectivity-type Manage Flink connectivity type.
  region            List Flink regions.
  shell             Start Flink interactive SQL client.
  statement         Manage Flink SQL statements.
```

A `compute pool` in Confluent Cloud for Apache Flink® represents a set of compute resources bound to a region that is used to run your SQL statements.
The resources provided by a compute pool are shared between all statements that use it.
The statements using a compute pool can only read and write Apache Kafka® topics in the same region as the compute pool.

View the `statements` in the shell using `show jobs;`

Install the Flink quick start plugin

Creates a Flink compute pool, associates an existing database or creates one automatically, then starts then starts a Flink shell session

```bash
$ confluent plugin install confluent-flink-quickstart
```

```bash
$ confluent plugin list
         Plugin Name         |                     File Path
-----------------------------+-----------------------------------------------------
  confluent flink quickstart | ~/.confluent/plugins/confluent-flink-quickstart.py
  confluent hub              | ~/apps/confluent-7.7.1/bin/confluent-hub
  confluent rebalancer       | ~/apps/confluent-7.7.1/bin/confluent-rebalancer
```

```bash
$ confluent flink quickstart

  --name NAME           The name for your Flink compute pool and the environment / Kafka cluster prefix if either is created (default: None)
  --max-cfu {5,10}      The number of Confluent Flink Units (default: 5)
  --environment-name ENVIRONMENT_NAME
                        Environment name to use, will create it if the environment does not exist (default: None)
  --region {us-east-1,us-east-2,eu-central-1,eu-west-1}
                        The cloud region to use (default: us-east-1)
  --cloud {aws}         The cloud provider to use (default: aws)
  --datagen-quickstarts [DATAGEN_QUICKSTARTS ...]
                        Datagen Source connector quickstarts to launch in Confluent Cloud. Provide a space-separatedlist to start more than one. E.g.,
                        --datagen-quickstarts shoe_orders shoe_customers shoes. See the available quickstarts here:
                        https://docs.confluent.io/cloud/current/connectors/cc-datagen-source.html (default: None)
  --debug               Prints the results of every command (default: False)


Create a Flink compute pool. Looks for existing Kafka clusters and prompts the user to select one as a database for the Flink pool. If there are no existing
clusters, the plugin will create one. Creates zero or more datagen source connectors to seed the database. Then it starts a Flink SQL shell. This plugin
assumes confluent CLI v4.0.0 or greater.
```


This will create a new Confluent Cloud environment quickstart_environment and the following resources within it:

- Schema Registry enabled
- a Kafka cluster named quickstart_kafka-cluster
- Datagen Source connectors for orders and associated products, including the API key needed for them to access Kafka
- Flink compute pool named quickstart
- A flink shell

```bash
confluent flink quickstart \
    --name quickstart \
    --max-cfu 10 \
    --region us-east-1 \
    --cloud aws \
    --datagen-quickstarts shoe_orders shoes
```

Open another flink shell:

```bash
$ confluent flink shell --compute-pool <compute-pool-id> --environment <environment-id>
$ confluent flink shell
```

In Confluent Cloud UI Flink Compute Pool -> Open SQL Workspace

```bash
$ confluent kafka cluster list
  Current |     ID     |           Name           | Type  | Provider |  Region   | Availability | Network | Status
----------+------------+--------------------------+-------+----------+-----------+--------------+---------+---------
  *       | lkc-66xrj3 | quickstart_kafka-cluster | BASIC | aws      | us-east-1 | single-zone  |         | UP
$ confluent kafka topic list
     Name     | Internal | Replication Factor | Partition Count
--------------+----------+--------------------+------------------
  shoe_orders | false    |                  3 |               1
  shoes       | false    |                  3 |               1
$ confluent connect cluster  list
      ID     |        Name         | Status  |  Type  | Trace
-------------+---------------------+---------+--------+--------
  lcc-16opz5 | datagen-shoe_orders | RUNNING | source |
  lcc-ovnr1y | datagen-shoes       | RUNNING | source |
```

Consume the created topics:

```bash
// create an api-key for the cluster
$ confluent confluent api-key create --resource lkc-66xrj3
+------------+------------------------------------------------------------------+
| API Key    | 7QH22FSTZ3WMI6YM                                                 |
| API Secret | IQv88Oahctv+PjECjsOOmktGTAviYb4/Ld8+7S80/QO4Q3K3ezzHuq1qgHsi8Do2 |
+------------+------------------------------------------------------------------+
```

```bash
$ confluent kafka topic consume shoe_orders --api-key 7QH22FSTZ3WMI6YM --api-secret IQv88Oahctv+PjECjsOOmktGTAviYb4/Ld8+7S80/QO4Q3K3ezzHuq1qgHsi8Do2 --value-format avro
{"order_id":2258,"product_id":"44f8a1fb-f65c-48e1-891b-409da33a66a1","customer_id":"84badc72-3daf-4b80-a8b9-2c6048084418","ts":1609585000000}
{"order_id":2259,"product_id":"b53fe1f7-a97b-4449-b8d5-873815812cbd","customer_id":"2c33ac67-4237-4cfb-94fb-2423384c6b2d","ts":1609585100000}
...
```

```bash
$ confluent kafka topic consume shoes --api-key 7QH22FSTZ3WMI6YM --api-secret IQv88Oahctv+PjECjsOOmktGTAviYb4/Ld8+7S80/QO4Q3K3ezzHuq1qgHsi8Do2 --value-format avro
{"id":"5f5be41f-c1da-4243-ba00-d2c445b43771","brand":"Gleichner-Buckridge","name":"Pro Max 500","sale_price":10995,"rating":5}
{"id":"41742008-b51e-4ff7-90ac-99ae33cc0b8a","brand":"Ankunding and Sons","name":"TrailRunner Sidekick 461","sale_price":16995,"rating":0}
...
```


```bash
SHOW TABLES;
DESCRIBE shoe_orders;
SELECT * FROM shoe_orders LIMIT 10;
```

We want to know the total order sales per shoe brand

```bash
WITH latest_shoes AS (
    SELECT * FROM (
        SELECT *,
               $rowtime,
               ROW_NUMBER() OVER (PARTITION BY id ORDER BY $rowtime DESC) AS rownum
        FROM shoes
    )
    WHERE rownum = 1
)
SELECT brand, ROUND(SUM(sale_price) / 100.00, 2) AS revenue
FROM shoe_orders
    JOIN latest_shoes
    ON shoe_orders.product_id = latest_shoes.id
GROUP BY brand;
```

Toggle the `M` on the query result screen to see the revenue totals get updated in real time.

Let's compute the total number of orders per day.

- first need to explicitly define the shoe_orders table's `watermark` strategy in terms of the ts column.
- `Watermarks` are used to determine when a window can close and whether an event arrives late and should be ignored.
- In Confluent Cloud for Apache Flink, the default watermark strategy is defined in terms of ingestion time (available as the system column `$rowtime`),

```bash
$ select *, $rowtime from shoe_orders
```
Override the default watermark strategy to use strictly ascending ts column values:

```bash
$ ALTER TABLE shoe_orders MODIFY WATERMARK FOR ts AS ts;
```

- tumbling windows, are fixed-sized

```bash
SELECT count(*) AS order_count, window_start
FROM TABLE(TUMBLE(TABLE shoe_orders, DESCRIPTOR(ts), INTERVAL '1' DAY)) GROUP BY window_start;
```

hopping windows in Flink SQL: https://developer.confluent.io/confluent-tutorials/hopping-windows/flinksql/
commulating windows in Flink SQL: https://developer.confluent.io/confluent-tutorials/cumulating-windows/flinksql/


### Clean up

```bash
$ confluent environment list
$ confluent environment delete <ENVIRONMENT_ID>
```

### Resources:

Quick start Confluent Cloud for Apache Flink: https://developer.confluent.io/quickstart/flink-on-confluent-cloud/
