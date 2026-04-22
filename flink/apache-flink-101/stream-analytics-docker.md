# Stream analytics using Docker

- `Stream analytics` - compute, in real time, statistics that summarize a ongoing stream of events.
- queries need support from Flink for both `state` and `time` - since they are accumulating a result over some period of time.

```bash
$ docker compose up --build -d
$ docker ps

# Once the containers are running, start Flink SQL client
$ docker compose run sql-client

# let count page views per second
# ts column includes some randomness: each timestamp is situated anywhere between 1 and 5 seconds in the past. 
Flink SQL>
CREATE TABLE `pageviews` (
  `url` STRING,
  `browser` STRING,
  `ts` TIMESTAMP(3)
)
WITH (
  'connector' = 'faker',
  'rows-per-second' = '100',
  'fields.url.expression' = '/#{GreekPhilosopher.name}.html',
  'fields.browser.expression' = '#{Options.option ''chrome'', ''firefox'', ''safari'')}',
  'fields.ts.expression' =  '#{date.past ''5'',''1'',''SECONDS''}'
);
  
  
```

- We need to safely implement windowing - windows that will be cleaned up once they are no longer changing
    - an input table that is append-only (which we have)
    - a designated timestamp column with timestamps that are known to be advancing (which we don't have (yet))

- `processing time` - counting the events based on the second during which it is processed, rather than when it occurred.
- `event time` - counting the events based on when it occured rather than when it is processed.

- In general, the use of `event time` is to be preferred, but for this exercise, we will experiment with windows that use processing time.
- `Processing time` windowing requires adding a timestamp column that is tied to the system's time-of-day clock.

```bash
# is a computed column, rather than being physically present in the event stream
Flink SQL > ALTER TABLE pageviews ADD `proc_time` AS PROCTIME();

Flink SQL > DESCRIBE pageviews;
+-----------+-----------------------------+-------+-----+-----------------+-----------+
|      name |                        type |  null | key |          extras | watermark |
+-----------+-----------------------------+-------+-----+-----------------+-----------+
|       url |                      STRING |  TRUE |     |                 |           |
|   browser |                      STRING |  TRUE |     |                 |           |
|        ts |                TIMESTAMP(3) |  TRUE |     |                 |           |
| proc_time | TIMESTAMP_LTZ(3) *PROCTIME* | FALSE |     | AS `PROCTIME`() |           |
+-----------+-----------------------------+-------+-----+-----------------+-----------+

Flink SQL > select * from pageviews limit 10;

+----+--------------------------------+--------------------------------+-------------------------+-------------------------+
| op |                            url |                        browser |                      ts |               proc_time |
+----+--------------------------------+--------------------------------+-------------------------+-------------------------+
| +I |               /Chrysippus.html |                        firefox | 2026-04-22 19:58:31.120 | 2026-04-22 19:58:34.733 |
| +I |              /Antisthenes.html |                        firefox | 2026-04-22 19:58:30.207 | 2026-04-22 19:58:34.733 |
| +I |               /Thucydides.html |                        firefox | 2026-04-22 19:58:33.558 | 2026-04-22 19:58:34.733 |
| +I |               /Archimedes.html |                        firefox | 2026-04-22 19:58:32.168 | 2026-04-22 19:58:34.733 |
| +I |              /Antisthenes.html |                         chrome | 2026-04-22 19:58:32.105 | 2026-04-22 19:58:34.733 |
| +I |                 /Epicurus.html |                         chrome | 2026-04-22 19:58:32.164 | 2026-04-22 19:58:34.733 |
| +I |                    /Plato.html |                         chrome | 2026-04-22 19:58:32.420 | 2026-04-22 19:58:34.733 |
| +I |                    /Galen.html |                         chrome | 2026-04-22 19:58:31.757 | 2026-04-22 19:58:34.733 |
| +I |               /Posidonius.html |                         safari | 2026-04-22 19:58:31.253 | 2026-04-22 19:58:34.733 |
| +I |               /Thucydides.html |                         safari | 2026-04-22 19:58:31.135 | 2026-04-22 19:58:34.733 |
+----+--------------------------------+--------------------------------+-------------------------+-------------------------+
                    
# built-in TUMBLE function is a table-valued function (TVF), takes 3 parameters
# a table descriptor (TABLE pageviews)
# a column descriptor for the time attribute (DESCRIPTOR(proc_time))
# a time interval to use for windowing (one second)                     
Flink SQL > SELECT * FROM TABLE(TUMBLE(TABLE pageviews, DESCRIPTOR(proc_time), INTERVAL '1' SECOND)) limit 10;                  
                    
+----+--------------------------------+--------------------------------+-------------------------+-------------------------+-------------------------+-------------------------+-------------------------+
| op |                            url |                        browser |                      ts |               proc_time |            window_start |              window_end |             window_time |
+----+--------------------------------+--------------------------------+-------------------------+-------------------------+-------------------------+-------------------------+-------------------------+
| +I |                  /Proclus.html |                        firefox | 2026-04-22 19:58:15.420 | 2026-04-22 19:58:19.962 | 2026-04-22 19:58:19.000 | 2026-04-22 19:58:20.000 | 2026-04-22 19:58:19.962 |
| +I |             /Zeno of Elea.html |                         chrome | 2026-04-22 19:58:17.056 | 2026-04-22 19:58:19.962 | 2026-04-22 19:58:19.000 | 2026-04-22 19:58:20.000 | 2026-04-22 19:58:19.962 |
| +I |                /Aristotle.html |                         safari | 2026-04-22 19:58:15.109 | 2026-04-22 19:58:19.962 | 2026-04-22 19:58:19.000 | 2026-04-22 19:58:20.000 | 2026-04-22 19:58:19.962 |
| +I |               /Parmenides.html |                        firefox | 2026-04-22 19:58:18.549 | 2026-04-22 19:58:19.962 | 2026-04-22 19:58:19.000 | 2026-04-22 19:58:20.000 | 2026-04-22 19:58:19.962 |
| +I |                    /Galen.html |                         chrome | 2026-04-22 19:58:16.621 | 2026-04-22 19:58:19.962 | 2026-04-22 19:58:19.000 | 2026-04-22 19:58:20.000 | 2026-04-22 19:58:19.962 |
| +I |                    /Galen.html |                         safari | 2026-04-22 19:58:15.949 | 2026-04-22 19:58:19.962 | 2026-04-22 19:58:19.000 | 2026-04-22 19:58:20.000 | 2026-04-22 19:58:19.962 |
| +I |               /Democritus.html |                         safari | 2026-04-22 19:58:17.312 | 2026-04-22 19:58:19.962 | 2026-04-22 19:58:19.000 | 2026-04-22 19:58:20.000 | 2026-04-22 19:58:19.962 |
| +I |               /Anaxagoras.html |                         safari | 2026-04-22 19:58:17.446 | 2026-04-22 19:58:19.962 | 2026-04-22 19:58:19.000 | 2026-04-22 19:58:20.000 | 2026-04-22 19:58:19.962 |
| +I |               /Arcesilaus.html |                         chrome | 2026-04-22 19:58:17.618 | 2026-04-22 19:58:19.962 | 2026-04-22 19:58:19.000 | 2026-04-22 19:58:20.000 | 2026-04-22 19:58:19.962 |
| +I |               /Posidonius.html |                         chrome | 2026-04-22 19:58:15.690 | 2026-04-22 19:58:19.962 | 2026-04-22 19:58:19.000 | 2026-04-22 19:58:20.000 | 2026-04-22 19:58:19.962 |
+----+--------------------------------+--------------------------------+-------------------------+-------------------------+-------------------------+-------------------------+-------------------------+

# GROUP BY window_start, window_end aggregates together all of the pageview events assigned to each window
Flink SQL> 

SELECT
  window_start, count(1) AS cnt
FROM TABLE(
  TUMBLE(DATA => TABLE pageviews, 
         TIMECOL => DESCRIPTOR(proc_time),
         SIZE => INTERVAL '1' SECOND))
GROUP BY window_start, window_end;

+----+-------------------------+----------------------+
| op |            window_start |                  cnt |
+----+-------------------------+----------------------+
| +I | 2026-04-22 20:01:55.000 |                  100 |
| +I | 2026-04-22 20:01:56.000 |                  100 |
| +I | 2026-04-22 20:01:57.000 |                  100 |
| +I | 2026-04-22 20:01:58.000 |                  100 |
| +I | 2026-04-22 20:01:59.000 |                  100 |
| +I | 2026-04-22 20:02:00.000 |                  100 |
| +I | 2026-04-22 20:02:01.000 |                  100 |
| +I | 2026-04-22 20:02:02.000 |                  100 |

# naive approaches for windowing
# it a materializing query, meaning they keep their state indefinitely
# here this is dangerous since there is an unbounded supply of seconds in the future.
Flink SQL > 
SELECT
  FLOOR(ts TO SECOND) AS window_start,
  count(1) as cnt
FROM pageviews
GROUP BY FLOOR(ts TO SECOND);

# it a materializing query also but here this is perfectly safe, 
# since there are only a small, finite number of different browsers
# this produces a continuously updating stream as its output 
Flink SQL >
SELECT
  browser,
  count(1) as cnt
FROM pageviews
GROUP BY browser;


- The naive windowing approaches with FLOOR produces a continuously updating stream as its output.
- While the function-based approach produces an `append-only` stream, where `each window` produces a single, final result.

- Other windows: HOP / CUMULATE / SESSION

```

# Resources

Time Attributes: https://nightlies.apache.org/flink/flink-docs-stable/docs/dev/table/concepts/time_attributes/