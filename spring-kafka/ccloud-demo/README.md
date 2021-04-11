
### Connect a producer to a Confluent Cloud cluster

1. Install ccloud CLI https://docs.confluent.io/ccloud-cli/current/index.html

2. Login 

```bash
$ ccloud login --save
...
Wrote credentials to netrc file "~/.netrc"
Using environment "t829" ("default").
```

3. List the environments 

```bash
$ ccloud environment list 
```

4. List the Kafka clusters

```bash
$ ccloud kafka cluster list
      Id      |         Name         | Type  | Provider |    Region    | Availability | Status
+-------------+----------------------+-------+----------+--------------+--------------+--------+
    lkc-3kj32 | kafka-cluster-on-gcp | BASIC | gcp      | europe-west6 | single-zone  | UP
```

5. Set the `ClUSTER_ID` environment variable, will be referenced later

```bash
export CLUSTER_ID=<cluster-id> 
```

6. Create a topic called `demo-topic` in the https://confluent.cloud 
   List the topics using the CLI: 

```bash
$ ccloud kafka topic list --cluster $CLUSTER_ID
     Name
+------------+
  demo-topic
```

7. To consume from a topic we need an API Key/Secret pair. Create the API Key/Secret using http://confluent.cloud

8. Store an API key/secret locally to use in the CLI.

```bash
$ ccloud api-key store --resource $CLUSTER_ID
Key: MHW7UJ2KN6F53T5N

Secret: ****************************************************************
Stored API secret for API key "MHW7UJ2KN6F53T5N".
```

9. View the keys

```bash
$ ccloud api-key list
         Key         | Description | Owner |     Owner Email      | Resource Type | Resource ID |       Created
+--------------------+-------------+-------+----------------------+---------------+-------------+----------------------+
    MHW7UJ2KN6F53T5N |             |  1160 | altfatterz@gmail.com |               | lkc-3kj32   | 2021-03-21T20:00:19Z
```

10. Export the key

```bash
export KEY=<key>
```

11. Make an API key active for use in other commands.

```bash
$ ccloud api-key use $KEY --resource $CLUSTER_ID
Set API Key "MHW7UJ2KN6F53T5N" as the active API key for "lkc-3kj32".
```

12. Start a Consumer:

```bash
$ ccloud kafka topic consume -b demo-topic --cluster $CLUSTER_ID
```

13. Start a Producer:
```bash
$ ccloud kafka topic produce demo-topic --cluster $CLUSTER_ID
```

14. Get the client configuration from https://confluent.cloud

Go to `Cluster --> Clients` and in the `Select configuration to copy into your client code` select your client type. 
In this example a Spring Boot configuration was used copied into the `/src/main/resources/applicaiton.yaml` 


### Connect via `kafka-console-producer` or `kafka-console-consumer`

1. Get the client configuration from https://confluent.cloud

Go to `Cluster --> CLI and Tools`

2. Export the cluster host name and topic
   
```bash
export KAFKA_CLUSTER_AND_PORT=pkc-lzoyy.europe-west6.gcp.confluent.cloud:9092
```

2. Start a consumer

```bash
$ kafka-console-consumer --from-beginning --bootstrap-server pkc-lzoyy.europe-west6.gcp.confluent.cloud:9092 \
--consumer.config client.properties --topic demo-topic
```

3. Check the consumer groups:

```bash
$ kafka-consumer-groups --bootstrap-server pkc-lzoyy.europe-west6.gcp.confluent.cloud:9092 \
 --command-config client.properties --list
```

### Connector demo

1. Create a topic `pageviews-topic` and setup a `Datagen connector` 

2. Check the running connector:

```bash
$ ccloud connector list --cluster $CLUSTER_ID

     ID     |       Name        | Status  |  Type  | Trace
+-----------+-------------------+---------+--------+-------+
  lcc-vzwdn | datagen-pageviews | RUNNING | source |
```

3. Export the connector 

```bash
export DATAGEN_CONNECTOR_ID=<datagen-connector-id>
```

3. Describe the running connector:

```bash
$ ccloud connector describe lcc-vzwdn --cluster $CLUSTER_ID

Connector Details
+--------+-------------------+
| ID     | lcc-vzwdn         |
| Name   | datagen-pageviews |
| Status | RUNNING           |
| Type   | source            |
| Trace  |                   |
+--------+-------------------+


Task Level Details
  TaskId |  State
+--------+---------+
       0 | RUNNING


Configuration Details
          Config          |                                                                          Value
+-------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------+
  internal.kafka.endpoint | PLAINTEXT://kafka-0.kafka.pkc-lzoyy.svc.cluster.local:9071,kafka-1.kafka.pkc-lzoyy.svc.cluster.local:9071,kafka-2.kafka.pkc-lzoyy.svc.cluster.local:9071
  kafka.api.key           | ****************
  kafka.dedicated         | false
  cloud.provider          | gcp
  quickstart              | PAGEVIEWS
  kafka.api.secret        | ****************
  kafka.region            | europe-west6
  output.data.format      | JSON
  cloud.environment       | prod
  kafka.endpoint          | SASL_SSL://pkc-lzoyy.europe-west6.gcp.confluent.cloud:9092
  kafka.topic             | pageviews-topic
  max.interval            |                                                                                                                                                      100
  name                    | datagen-pageviews
  tasks.max               |                                                                                                                                                        1
  valid.kafka.api.key     | true
  connector.class         | DatagenSource
```

4. Consume from the topic

```bash
$ ccloud kafka topic consume -b pageviews-topic --cluster $CLUSTER_ID

{"viewtime":1,"userid":"User_4","pageid":"Page_15"}
{"viewtime":11,"userid":"User_1","pageid":"Page_68"}
{"viewtime":21,"userid":"User_2","pageid":"Page_52"}
{"viewtime":31,"userid":"User_7","pageid":"Page_45"}
{"viewtime":41,"userid":"User_9","pageid":"Page_78"}
...
```

5. Pause/delete the connector  

```bash
$ ccloud connector pause $DATAGEN_CONNECTOR_ID --cluster $CLUSTER_ID
$ ccloud connector delete $DATAGEN_CONNECTOR_ID --cluster $CLUSTER_ID
```
