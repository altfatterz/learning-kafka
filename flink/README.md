
### Flink SQL with Docker

Build the Flink SQL image: 

```bash
$ docker compose up --build -d
```

Start the container:

```bash
$ docker compose run sql-client
Flink SQL>
help;
```

Check the UI: http://localhost:8081/

Create a bounded table with 500 rows of data generated by the [faker-faker](https://github.com/knaufk/flink-faker) table source.

```bash
CREATE TABLE `bounded_pageviews` (
  `url` STRING,
  `user_id` STRING,
  `browser` STRING,
  `ts` TIMESTAMP(3)
)
WITH (
  'connector' = 'faker',
  'number-of-rows' = '500',
  'rows-per-second' = '100',
  'fields.url.expression' = '/#{GreekPhilosopher.name}.html',
  'fields.user_id.expression' = '#{numerify ''user_##''}',
  'fields.browser.expression' = '#{Options.option ''chrome'', ''firefox'', ''safari'')}',
  'fields.ts.expression' =  '#{date.past ''5'',''1'',''SECONDS''}'
);
```

How the data looks like:

```bash
select * from bounded_pageviews limit 10;
```

Set to batch mode. (default is `streaming` )

```bash
set 'execution.runtime-mode' = 'batch';
// the sink receives only a single, final value, count 500, after 5 seconds, 
select count(*) AS `count` from bounded_pageviews;
// set 'execution.runtime-mode' = 'streaming';
// the sink receives 100 to 200, etc, up to 500, again this will take 5 seconds
select count(*) AS `count` from bounded_pageviews;
// In changelog mode, the SQL Client doesn't just update the count in place, but instead displays each message in the stream of updates it's receiving from the Flink SQL runtime.
set 'sql-client.execution.result-mode' = 'changelog';
select count(*) AS `count` from bounded_pageviews;
 op                count
 -----------------------
...                  ...
 -U                  497
 +U                  498
 -U                  498
 +U                  499
 -U                  499
 +U                  500
```

Check the jobs:
```bash
show jobs;
+----------------------------------+----------+----------+-------------------------+
|                           job id | job name |   status |              start time |
+----------------------------------+----------+----------+-------------------------+
| 6fcbf5eab13507f603ca184f94262323 |  collect | FINISHED | 2024-11-03T09:00:26.860 |
| 6c6c9fc20d89f33ba03b494fb4eddf1d |  collect | FINISHED | 2024-11-03T08:58:12.098 |
| d57c6e4ce0cd26cd5f27ae454f698084 |  collect | CANCELED | 2024-11-03T09:05:04.497 |
| 720b07662d49127a9dd857afb5e98941 |  collect | FINISHED | 2024-11-03T08:59:35.077 |
+----------------------------------+----------+----------+-------------------------+
```

Streaming mode, unbounded input

```bash
CREATE TABLE `pageviews` (
  `url` STRING,
  `user_id` STRING,
  `browser` STRING,
  `ts` TIMESTAMP(3)
)
WITH (
  'connector' = 'faker',
  'rows-per-second' = '100',
  'fields.url.expression' = '/#{GreekPhilosopher.name}.html',
  'fields.user_id.expression' = '#{numerify ''user_##''}',
  'fields.browser.expression' = '#{Options.option ''chrome'', ''firefox'', ''safari'')}',
  'fields.ts.expression' =  '#{date.past ''5'',''1'',''SECONDS''}'
);
```

### Cleanup

```bash
docker compose down -v
```

Resources:
- https://developer.confluent.io/courses/apache-flink/intro/
- https://developer.confluent.io/courses/flink-java/overview/
- List of Flink Resources: https://github.com/pmoskovi/flink-learning-resources?tab=readme-ov-file#table-of-contents
- Flink SQL Workshop:
- Blog from Robin Moffatt: https://www.decodable.co/blog/flink-sql-misconfiguration-misunderstanding-and-mishaps#the-jdbc-catalog

Workshop Prerequisite: https://github.com/griga23/shoe-store/blob/main/prereq.md
Workshop LAB1: https://github.com/griga23/shoe-store/blob/main/lab1.md
Workshop LAB2: https://github.com/griga23/shoe-store/blob/main/lab2.md


