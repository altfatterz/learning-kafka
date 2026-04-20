Apache Flink Quick Start on Confluent Cloud

```bash
# install Confluent CLI
$ brew install confluentinc/tap/cli
# or upgrade if already installed
$ brew upgrade confluentinc/tap/cli

$ confluent version
confluent - Confluent CLI

Version:     v4.58.0
Git Ref:     78eff321
Build Date:  2026-04-14T21:42:14Z
Go Version:  go1.26.1 (darwin/arm64)
Development: false

# login to your Confluent Cloud account
$ confluent login --prompt --save
# Wrote login credentials to keychain.
```

```bash
$ confluent flink
Usage:
  confluent flink [command]

Available Commands:
  artifact            Manage Flink UDF artifacts.
  compute-pool        Manage Flink compute pools.
  compute-pool-config Manage Flink compute pools configs.
  connection          Manage Flink connections.
  connectivity-type   Manage Flink connectivity type.
  endpoint            Manage Flink endpoint.
  region              Manage Flink regions.
  shell               Start Flink interactive SQL client.
  statement           Manage Flink SQL statements.
```

- A `compute pool` represents the compute resources that are used to run your `SQL statements`
  - https://docs.confluent.io/cloud/current/flink/operate-and-deploy/create-compute-pool.html#create-a-compute-pool-manually
- To create a `compute pool` manually, you need the `OrganizationAdmin`, `EnvironmentAdmin`, or `FlinkAdmin` RBAC role
- In `Confluent Cloud for Apache Flink`, a `statement` represents a high-level resource that’s created when you enter a SQL query.
  - https://docs.confluent.io/cloud/current/flink/concepts/statements.html

### Supported Cloud regions for Confluent Cloud for Apache Flink

```bash
$ confluent flink region list
```

### Install the Flink quick start plugin

```bash
$ confluent plugin install confluent-flink-quickstart
```

```bash
$ confluent plugin list
              ID             |            Name            |                     File Path
-----------------------------+----------------------------+-----------------------------------------------------
  confluent-flink-quickstart | confluent flink quickstart | ~/.confluent/plugins/confluent-flink-quickstart.py
  confluent-hub              | confluent hub              | ~/apps/confluent-8.0.0/bin/confluent-hub
  confluent-rebalancer       | confluent rebalancer       | ~/apps/confluent-8.0.0/bin/confluent-rebalancer
```

Create a Flink compute pool. Looks for existing Kafka clusters and prompts the user to select one as a database for the Flink pool. 
If there are no existing clusters, the plugin will create one. 
Creates zero or more datagen source connectors to seed the database. 
Then it either generates a Table API client config file, starts a Flink SQL shell, or completes. 

```bash
$ confluent flink quickstart --help

options:
  -h, --help            show this help message and exit
  --name NAME           The name for your Flink compute pool and the environment / Kafka cluster prefix if either is created (default: None)
  --max-cfu {5,10}      The number of Confluent Flink Units (default: 5)
  --environment-name ENVIRONMENT_NAME
                        Environment name to use, will create it if the environment does not exist (default: None)
  --kafka-cluster-name KAFKA_CLUSTER_NAME
                        Kafka cluster name to use when creating a new cluster. (default: None)
  --region REGION       The cloud region to use (default: us-east-1)
  --cloud {aws,gcp,azure}
                        The cloud provider to use (default: aws)
  --datagen-quickstarts [DATAGEN_QUICKSTARTS ...]
                        Datagen Source connector quickstarts to launch in Confluent Cloud. Provide a space-separatedlist to start more than one. E.g.,
                        --datagen-quickstarts shoe_orders shoe_customers shoes. See the available quickstarts here:
                        https://docs.confluent.io/cloud/current/connectors/cc-datagen-source.html (default: None)
  --create-kafka-keys   Create Kafka API keys for the cluster (default: False)
  --table-api-client-config-file TABLE_API_CLIENT_CONFIG_FILE
                        Path to Table API client config file to create (default: None)
  --debug               Prints the results of every command (default: False)
  --no-flink-shell      Turns off the default behaviour of starting a Flink Shell at the end (default: False)

````

### Flink Quickstart

This will create a new Confluent Cloud environment `quickstart_environment` and the following resources within it:

- Schema Registry enabled
- a Kafka cluster named `quickstart_kafka-cluster`
- Datagen Source connectors for orders and associated products, including the API key needed for them to access Kafka
- Flink compute pool named `quickstart`
- A flink shell

```bash
$ confluent flink quickstart \
    --name quickstart \
    --max-cfu 10 \
    --region us-east-1 \
    --cloud aws \
    --datagen-quickstarts shoe_orders shoes
    
Creating new environment quickstart_environment
Setting the active environment to quickstart_environment (env-p6881o)
Using environment env-p6881o.
Searching for existing databases (Kafka clusters)
No existing database found, will create one in us-east-1
Set Kafka cluster lkc-o893po as the active cluster for environment env-p6881o.
Creating API key for Kafka cluster lkc-o893po
Created Kafka API key saved to quickstart_environment_quickstart_kafka-cluster_kafka_key.properties
Creating the Flink pool
Checking for existing Flink compute pool quickstart
Creating Flink pool quickstart
Waiting for the Datagen connector status(es) to be RUNNING. Checking every 10 seconds
Connector(s) provisioned
Waiting for the Flink compute pool status to be PROVISIONED. Checking every 10 seconds
Flink compute pool lfcp-jqo5d8 is PROVISIONED
Starting interactive Flink shell now
No Flink endpoint is specified, defaulting to public endpoint: https://flink.us-east-1.aws.confluent.cloud
Welcome!
To exit, press Ctrl-Q or type "exit".

[Ctrl-Q] Quit [Ctrl-S] Toggle Completions [Ctrl-G] Toggle Diagnostics
>    
```

Note: if you exit the Flink shell, you can return to it by running `confluent flink shell`

```bash
# default environment, default compute-pool
$ confluent flink shell
# given environment, default compute-pool
$ confluent flink shell --environment ${ENV_ID}
# given environment, given compute-pool
$ confluent flink shell --compute-pool ${COMPUTE_POOL_ID} --environment ${ENV_ID}
```


```bash
$ confluent flink compute-pool list
  Current |     ID      |    Name    | Environment | Current CFU | Max CFU | Default Pool | Cloud |  Region   |   Status
----------+-------------+------------+-------------+-------------+---------+--------------+-------+-----------+--------------
  *       | lfcp-jqo5d8 | quickstart | env-p6881o  |           0 |      10 | false        | AWS   | us-east-1 | PROVISIONED
```

```bash
$ confluent flink region use --cloud aws --region us-east-1
$ confluent flink endpoint list
  Current |                  Endpoint                   | Cloud |  Region   |  Type
----------+---------------------------------------------+-------+-----------+---------
          | https://flink.us-east-1.aws.confluent.cloud | AWS   | us-east-1 | PUBLIC
```


```bash
$ confluent kafka cluster list
  Current |     ID     |           Name           | Type  | Cloud |  Region   | Availability | Network | Status |                         Endpoint
----------+------------+--------------------------+-------+-------+-----------+--------------+---------+--------+-----------------------------------------------------------
  *       | lkc-o893po | quickstart_kafka-cluster | BASIC | aws   | us-east-1 | single-zone  |         | UP     | SASL_SSL://pkc-oxqxx9.us-east-1.aws.confluent.cloud:9092

$ confluent kafka topic list
     Name     | Internal | Replication Factor | Partition Count
--------------+----------+--------------------+------------------
  shoe_orders | false    |                  3 |               1
  shoes       | false    |                  3 |               1
$ confluent connect cluster list
      ID     |        Name         | Status  |  Type  | Trace
-------------+---------------------+---------+--------+--------
  lcc-0orx15 | datagen-shoe_orders | RUNNING | source |
  lcc-85z19q | datagen-shoes       | RUNNING | source |
```

### Create table

```bash
$ confluent flink shell
No Flink endpoint is specified, defaulting to public endpoint: https://flink.us-east-1.aws.confluent.cloud
> CREATE TABLE quickstart(message STRING);
> INSERT INTO quickstart VALUES ('hello world');
> SELECT * from quickstart;

> show tables
> DESCRIBE shoe_orders;
Creating statement: cli-2026-04-18-143634-bcf3a242-2cd2-4403-adc7-3a714f0e3e52
Statement successfully submitted.
Finished statement execution. Statement phase: COMPLETED.
+-------------+------------------+----------+------------+
| Column Name |    Data Type     | Nullable |   Extras   |
+-------------+------------------+----------+------------+
| key         | BYTES            | NULL     | BUCKET KEY |
| order_id    | INT              | NOT NULL |            |
| product_id  | STRING           | NOT NULL |            |
| customer_id | STRING           | NOT NULL |            |
| ts          | TIMESTAMP_LTZ(3) | NOT NULL |            |
+-------------+------------------+----------+------------+
> DESCRIBE shoes;
+-------------+-----------+----------+------------+
| Column Name | Data Type | Nullable |   Extras   |
+-------------+-----------+----------+------------+
| key         | BYTES     | NULL     | BUCKET KEY |
| id          | STRING    | NOT NULL |            |
| brand       | STRING    | NOT NULL |            |
| name        | STRING    | NOT NULL |            |
| sale_price  | INT       | NOT NULL |            |
| rating      | DOUBLE    | NOT NULL |            |
+-------------+-----------+----------+------------+
> DESCRIBE quickstart;
+-------------+-----------+----------+--------+
| Column Name | Data Type | Nullable | Extras |
+-------------+-----------+----------+--------+
| message     | STRING    | NULL     |        |
+-------------+-----------+----------+--------+


> SELECT * FROM shoe_orders LIMIT 10;
> SELECT * FROM shoes LIMIT 10;
# We may see more than one row for a given ID because the shoes table continuously receives product metadata updates.
> SELECT brand, name, sale_price, rating FROM shoes WHERE id='8b577494-782f-453d-82f7-2826d7681afe';

# quickstart topic was created
$ confluent kafka topic list | grep quickstart
     Name     | Internal | Replication Factor | Partition Count
--------------+----------+--------------------+------------------
  quickstart  | false    |                  3 |               6

```

### Run aggregated join query

```bash
# we want to know the total order sales per shoe brand
# because the shoes table can have multiple rows with the same id we use latest_shoes. More details: https://developer.confluent.io/confluent-tutorials/deduplication-windowed/flinksql/

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


### Windowed aggregation

```bash
# let's compute the total number of orders per day.
# rowtime is the ingestion time, ts is a business logic time when the order was made 
SELECT $rowtime, order_id, ts FROM shoe_orders LIMIT 20;

║$rowtime                order_id ts
2026-04-18 14:14:16.900 1001     2021-01-01 02:01:40.000                                                                                                                                                                                         ║
2026-04-18 14:14:20.364 1002     2021-01-01 02:03:20.000                                                                                                                                                                                         ║
2026-04-18 14:14:21.115 1003     2021-01-01 02:05:00.000                                                                                                                                                                                         ║
...
```

- `event time stream processing` 
    - the timestamp used for windowing and determining if an event arrives late comes from the event source itself
      as opposed to from the event's time of ingest into Kafka (here queried with the system column `$rowtime`)

- `tables` watermark strategy - to be based on the `ts` column - from the default ingestion time to Kafka (`$rowtime`)

```bash
ALTER TABLE shoe_orders MODIFY WATERMARK FOR ts AS ts;
```

- `watermark` - are used to determine when a `window` can close and whether an event arrives late and should be ignored

```bash
# tumble windows are fixed-size (1 day in our case), non-overlapping windows
SELECT count(*) AS order_count, window_start
FROM TABLE(TUMBLE(TABLE shoe_orders, DESCRIPTOR(ts), INTERVAL '1' DAY)) GROUP BY window_start;
```

Toggle the `M` on the query result screen to see the revenue totals get updated in real time.


### Example Catalog 

Created a SQL workspace for Azure / switzerlandnorth and it created a `compute-pool`

```bash
# list compute-pool
$ confluent flink compute-pool list

# get a flink shell
$ confluent flink shell --compute-pool lfcp-dmkv7y --environment env-3nj8y0

# Confluent Cloud for Apache Flink provides example data streams that you can experiment with.
# `marketplace` database in the `examples` catalog.
# A `catalog` is a collection of databases that share the same namespace.
# A `database` is a collection of tables that share the same namespace.
# An `environment` is mapped to a `Flink catalog`, and a `Kafka cluster` is mapped to a `Flink database`.
# You can always use `catalog.database.table`, but is easier to set a default

$ USE CATALOG `examples`;
$ USE `marketplace`;
$ SHOW TABLES;
+------------+
| Table Name |
+------------+
| clicks     |
| customers  |
| orders     |
| products   |
+------------+
$ describe orders;
+-------------+-----------+----------+--------+
| Column Name | Data Type | Nullable | Extras |
+-------------+-----------+----------+--------+
| order_id    | STRING    | NOT NULL |        |
| customer_id | INT       | NOT NULL |        |
| product_id  | STRING    | NOT NULL |        |
| price       | DOUBLE    | NOT NULL |        |
+-------------+-----------+----------+--------+

# This statement calculates the total revenue and count of orders in 5-second windows, using a tumbling window function
# The `TUMBLE` function defines a tumbling window based on the event time column (`$rowtime`) with a size of 5 seconds
# The `window_start` and `window_end` columns represent the start and end times of each window
# The `SUM(price)` function calculates the total revenue for each window by summing the `price` column of all orders in the window
# The `COUNT(*)` function calculates the count of orders in each window
# To ensure that the window closes and results are emitted in a timely manner, it's important to have a continuous stream of data coming in to advance the watermark
# If the watermark does not advance, the window will not close and results will not be emitted until new data arrives
# You can also choose to use "sql.tables.scan.idle-timeout" to mark a source as idle, while letting the watermark advance
# See https://docs.confluent.io/cloud/current/flink/reference/statements/set.html for more details
$ SELECT 
  window_start, 
  window_end, 
  SUM(price) AS total_revenue, 
  COUNT(*) AS cnt 
FROM 
  TABLE(TUMBLE(TABLE `orders`, DESCRIPTOR($rowtime), INTERVAL '5' SECONDS)) 
GROUP BY window_start, window_end;

# list running statements
$ confluent flink region use --cloud azure --region switzerlandnorth
$ confluent flink statement list --status running
# stop a running statement
$ confluent flink statement stop <my-statement>
```

### Compute Pools

- You are not charged just for having an empty compute pool.
- You only pay for what you use, not the MAX_CFU limit you set on the pool. (CFUs - Confluent Flux Units) 
- Confluent Cloud, Apache Flink uses a serverless pricing model.
- You are only billed for the active processing power used when statements are actually running.

### Deploy a Flink SQL Statement:

- Deploys a Flink SQL statement programmatically on `Confluent Cloud for Apache Flink` by using `Hashicorp Terraform` and `GitHub Actions`
  - https://docs.confluent.io/cloud/current/flink/operate-and-deploy/deploy-flink-sql-statement.html
- Deploy a Flink SQL Statement on `Confluent Cloud for Apache Flink` with dbt (data build tool)
  - https://docs.confluent.io/cloud/current/flink/operate-and-deploy/deploy-flink-dbt.html

### Clean up

```bash
$ confluent environment list
$ confluent environment delete <ENVIRONMENT_ID>
```

### Resources:

- Quick start Confluent Cloud for Apache Flink: https://developer.confluent.io/quickstart/flink-on-confluent-cloud/
- Get Started with Confluent Cloud for Apache Flink https://docs.confluent.io/cloud/current/flink/get-started/overview.html
- Flink SQL Queries overview https://docs.confluent.io/cloud/current/flink/reference/queries/overview.html
- Hopping window https://developer.confluent.io/confluent-tutorials/hopping-windows/flinksql/
- Cumulating window https://developer.confluent.io/confluent-tutorials/cumulating-windows/flinksql/
