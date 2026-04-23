# Stream analytics using Confluent Cloud

- `Stream analytics` - compute, in real time, statistics that summarize a ongoing stream of events.
- queries need support from Flink for both `state` and `time` - since they are accumulating a result over some period of time.

```bash
# insttall confluent cli
$ brew install confluentinc/tap/cli
# login
$ confluent login --save
# install the quickstart plugin
$ confluent plugin install confluent-flink-quickstart
# create the flink and kafka resources
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
 
# check the created resources    
$ confluent environment list 
$ confluent flink compute-pool list
$ confluent kafka cluster list
# Describe the Schema Registry cluster for this environment.
$ confluent schema-registry cluster describe
   
# to get a Flink SQL    
$ confluent flink shell    
Flink SQL> use catalog examples;
# switch to marketplace database
Flink SQL> use marketplace;   
Flink SQL> show tables;

+------------+
| Table Name |
+------------+
| clicks     |
| customers  |
| orders     |
| products   |
+------------+
```

```bash
Flink SQL> describe clicks; 

+-------------+-----------+----------+--------+
| Column Name | Data Type | Nullable | Extras |
+-------------+-----------+----------+--------+
| click_id    | STRING    | NOT NULL |        |
| user_id     | INT       | NOT NULL |        |
| url         | STRING    | NOT NULL |        |
| user_agent  | STRING    | NOT NULL |        |
| view_time   | INT       | NOT NULL |        |
+-------------+-----------+----------+--------+

# check data 
Flink SQL> select * from clicks limit 5;

click_id                             user_id url                                user_agent                                                                                                                view_time                             ║
a824c503-29d0-44aa-a77b-e30fb24a91c4 3583    https://www.acme.com/product/sssvn Mozilla/5.0 (Windows NT 6.1; WOW64; rv:54.0) Gecko/20100101 Firefox/54.0                                                  48                                    ║
c509fb3e-9b47-4891-a8ab-2e6141a403c9 3657    https://www.acme.com/product/qolbw Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.75.14 (KHTML, like Gecko) Version/7.0.3 Safari/7046A194A   36                                    ║
b67c4e85-ae74-4446-988e-983766f97824 4760    https://www.acme.com/product/jvrpj Mozilla/4.0 (compatible; MSIE 9.0; Windows NT 6.1; 125LA; .NET CLR 2.0.50727; .NET CLR 3.0.04506.648; .NET CLR 3.5.21022) 24                                    ║
f42f792d-e268-4695-87ed-fae17e91bdf6 4689    https://www.acme.com/product/zbpfo Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36        68                                    ║
90df5e0d-ab4d-4220-ac36-985a9b1f1f96 3150    https://www.acme.com/product/xfknc Mozilla/5.0 (Windows NT 5.1; rv:33.0) Gecko/20100101 Firefox/33.0                                                         49

# check the table definition
Flink SQL> show create table clicks

+------------------------------------------------------------------------------------------+
|                                    SHOW CREATE TABLE                                     |
+------------------------------------------------------------------------------------------+
| CREATE TABLE `examples`.`marketplace`.`clicks` (                                         |
|   `click_id` VARCHAR(2147483647) NOT NULL,                                               |
|   `user_id` INT NOT NULL,                                                                |
|   `url` VARCHAR(2147483647) NOT NULL,                                                    |
|   `user_agent` VARCHAR(2147483647) NOT NULL,                                             |
|   `view_time` INT NOT NULL                                                               |
| )                                                                                        |
| WITH (                                                                                   |
|   'changelog.mode' = 'append',                                                           |
|   'connector' = 'faker',                                                                 |
|   'fields.click_id.expression' = '#{Internet.UUID}',                                     |
|   'fields.url.expression' = '#{regexify ''https://www[.]acme[.]com/product/[a-z]{5}''}', |
|   'fields.user_agent.expression' = '#{Internet.UserAgent}',                              |
|   'fields.user_id.expression' = '#{Number.numberBetween ''3000'',''5000''}',             |
|   'fields.view_time.expression' = '#{Number.numberBetween ''10'',''120''}',              |
|   'rows-per-second' = '50'                                                               |
| )                                                                                        |
|                                                                                          |
+------------------------------------------------------------------------------------------+

# naive approach 
Flink SQL >   
SELECT
  FLOOR(CURRENT_TIMESTAMP TO SECOND) AS window_start,
  count(1) as cnt
FROM clicks
GROUP BY FLOOR(CURRENT_TIMESTAMP TO SECOND);   

# another naive approach using '$rowtime' system column, which is capturing event time 
Flink SQL> describe extended clicks;

+-------------+----------------------------+----------+-----------------------------------------------------+---------+
| Column Name |         Data Type          | Nullable |                       Extras                        | Comment |
+-------------+----------------------------+----------+-----------------------------------------------------+---------+
| click_id    | STRING                     | NOT NULL |                                                     |         |
| user_id     | INT                        | NOT NULL |                                                     |         |
| url         | STRING                     | NOT NULL |                                                     |         |
| user_agent  | STRING                     | NOT NULL |                                                     |         |
| view_time   | INT                        | NOT NULL |                                                     |         |
| $rowtime    | TIMESTAMP_LTZ(3) *ROWTIME* | NOT NULL | METADATA VIRTUAL, WATERMARK AS `SOURCE_WATERMARK`() | SYSTEM  |
+-------------+----------------------------+----------+-----------------------------------------------------+---------+

# here the problem is state retention, Flink will keep the counters for all windows forever, there's a unbounded supply of seconds in the future.
Flink SQL >
SELECT
  FLOOR(`$rowtime` TO SECOND) AS window_start,
  count(1) as cnt
FROM examples.marketplace.clicks
GROUP BY FLOOR(`$rowtime` TO SECOND);

# this is also materialised query like the ones above, but here is no problem here with state because there's a small, finite number of distinct user agents.
SELECT
  user_agent,
  count(1) as cnt
FROM examples.marketplace.clicks
GROUP BY user_agent;
```   

# A better approach to windowing using time attributes and table-valued functions

```bash
# To safely implement windowing:
# - an input table that is append-only (which we have) 
# - a designated timestamp column with timestamps that are known to be advancing ($rowtime)


# a timestamp column to be a `time attribute` it must have `watermarking` defined on it
# `watermarking` will be based on a heuristic describing how out-of-order those timestamps can be.
# in Confluent Cloud includes default watermarking defined on the "$rowtime" column

# check here using the TUMBLE function first

# check window_start 09:56:55.000 (inclusive) / window_end 09:56:56.000 (exclusive) - marking the 1 second duration
# window_time is 09:56:55.999 - the last possible moment inside that window
# rowtime is defined as TIMESTAMP_LTZ(3)  - local time zone, with 3 precision, so window_time ends in .999
Flink SQL>
SELECT
  *
FROM 
  TUMBLE(DATA => TABLE examples.marketplace.clicks, 
         TIMECOL => DESCRIPTOR($rowtime),
         SIZE => INTERVAL '1' SECOND);

click_id                             user_id url                                user_agent                                                                     view_time $rowtime                window_start            window_end              window_time                                                                                                     ║
8d40f785-cced-4171-87da-871bfdf6fff9 3615    https://www.acme.com/product/gqfqb Mozilla/5.0 (Windows NT 5.1; rv:7.0.1) Gecko/20100101 Firefox/7.0.1            106       2026-04-23 09:56:55.010 2026-04-23 09:56:55.000 2026-04-23 09:56:56.000 2026-04-23 09:56:55.999                                                                                         ║
b45492f0-7918-4feb-8a3f-00c6c2bb6950 4678    https://www.acme.com/product/lmegq Mozilla/4.0 (compatible; MSIE 7.0; AOL 9.0; Windows NT 5.1; .NET CLR 1.1.4322) 69        2026-04-23 09:56:55.011 2026-04-23 09:56:55.000 2026-04-23 09:56:56.000 2026-04-23 09:56:55.999                                                                                         ║
7b121169-9f85-4021-a66e-d0229fe9f467 4164    https://www.acme.com/product/wtbgd Mozilla/5.0 (Windows NT x.y; Win64; x64; rv:10.0) Gecko/20100101 Firefox/10.0  118       2026-04-23 09:56:55.011 2026-04-23 09:56:55.000 2026-04-23 09:56:56.000 2026-04-23 09:56:55.999                                                                                                                                                                                                                                      ║ 
 
 
# grouping on top of TUMBLE function output, easy to do any aggregation - counting, summing, averaging
# it will return 50 all the time since the faker generates 50 per second, the rows-per-second faker field does not accept expression
Flink SQL>

SELECT
  window_start,
  count(1) AS cnt
FROM
  TUMBLE(DATA => TABLE examples.marketplace.clicks, 
         TIMECOL => DESCRIPTOR($rowtime),
         SIZE => INTERVAL '1' SECOND)
GROUP BY window_start, window_end;


# to use the current kafka cluster if you need to
Flink SQL >
use catalog flink101_environment;
use flink101_kafka-cluster;
```

# cleanup

```bash
confluent environment delete <ENV_ID>
```