### Getting started

https://docs.confluent.io/cloud/current/get-started/index.html


```bash
$ confluent login
$ confluent organization list
$ confluent organization describe
```

```bash
$ confluent environment list

 Current |     ID     |  Name   | Stream Governance Package
----------+------------+---------+----------------------------
  *       | env-7qg722 | default |

$ confluent environment update --governance-package essentials

$ confluent environment list
  Current |     ID     |  Name   | Stream Governance Package
----------+------------+---------+----------------------------
  *       | env-7qg722 | default | ESSENTIALS
```

Downgrading the package from "advanced" to "essentials" is not allowed once the Schema Registry cluster is provisioned.

### Create a kafka cluster

```bash
$ confluent kafka cluster create quickstart-cluster --cloud "aws" --region "us-west-2"

| Current              | false                                                   |
| ID                   | lkc-0w6755                                              |
| Name                 | quickstart-cluster                                      |
| Type                 | BASIC                                                   |
| Ingress Limit (MB/s) | 250                                                     |
| Egress Limit (MB/s)  | 750                                                     |
| Storage              | 5 TB                                                    |
| Provider             | aws                                                     |
| Region               | us-west-2                                               |
| Availability         | single-zone                                             |
| Status               | PROVISIONING                                            |
| Endpoint             | SASL_SSL://pkc-rgm37.us-west-2.aws.confluent.cloud:9092 |
| REST Endpoint        | https://pkc-rgm37.us-west-2.aws.confluent.cloud:443
```

```bash
$ confluent kafka cluster list

  Current |     ID     |        Name        | Type  | Provider |  Region   | Availability | Network | Status
----------+------------+--------------------+-------+----------+-----------+--------------+---------+---------
          | lkc-0w6755 | quickstart-cluster | BASIC | aws      | us-west-2 | single-zone  |         | UP
```

Make it the active cluster for the given environment

```bash
$ confluent kafka cluster use lkc-0w6755
```

### Create a topic


```bash
$ confluent kafka topic create users
$ confluent kafka topic list

  Name  | Internal | Replication Factor | Partition Count
--------+----------+--------------------+------------------
  users | false    |                  3 |               6
```

### Create an API Key examples
 
```bash
Create a Cloud API key:
$ confluent api-key create --resource cloud

Create a Flink API key for region "N. Virginia (us-east-1)":
$ confluent api-key create --resource flink --cloud aws --region us-east-1

Create an API key with full access to Kafka cluster "lkc-123456":
$ confluent api-key create --resource lkc-123456

Create an API key for Kafka cluster "lkc-123456" and service account "sa-123456":
$ confluent api-key create --resource lkc-123456 --service-account sa-123456

Create an API key for Schema Registry cluster "lsrc-123456":
$ confluent api-key create --resource lsrc-123456

Create an API key for KSQL cluster "lksqlc-123456":
$ confluent api-key create --resource lksqlc-123456
```

We need now an API key with full access to the Kafka cluster

```bash
$ confluent confluent api-key create --resource lkc-0w6755

+------------+------------------------------------------------------------------+
| API Key    | CI4JADFSDFQZXO3U                                                 |
| API Secret | hZYCSSfDYnaz0/n+IgBLwkvxx6hnzMy5aLOgJ1kadX7YVdmoy5uBR80lg177ElXb |
+------------+------------------------------------------------------------------+
```

### Create a sample connect producer

```bash
$ confluent connect cluster create --config-file quick-start.json --cluster lkc-0w6755
$ confluent connect cluster list

      ID     |             Name             |    Status    |  Type  | Trace
-------------+------------------------------+--------------+--------+--------
  lcc-znwrkd | DatagenSourceConnector_users | PROVISIONING | source |
$  confluent connect cluster describe lcc-znwrkd

  
Connector Details
+--------+------------------------------+
| ID     | lcc-znwrkd                   |
| Name   | DatagenSourceConnector_users |
| Status | RUNNING                      |
| Type   | source                       |
+--------+------------------------------+


Task Level Details
  Task ID |  State
----------+----------
        0 | RUNNING


Configuration Details
        Config       |                          Value
---------------------+----------------------------------------------------------
  cloud.environment  | prod
  cloud.provider     | aws
  connector.class    | DatagenSource
  kafka.api.key      | ****************
  kafka.api.secret   | ****************
  kafka.auth.mode    | KAFKA_API_KEY
  kafka.endpoint     | SASL_SSL://pkc-rgm37.us-west-2.aws.confluent.cloud:9092
  kafka.region       | us-west-2
  kafka.topic        | users
  name               | DatagenSourceConnector_users
  output.data.format | JSON_SR
  quickstart         | USERS
  tasks.max          | 1  
```

### Consume from topic

```bash
$ confluent api-key use CI4JADFSDFQZXO3U
$ confluent kafka topic consume users

��{"registertime":1497921029945,"userid":"User_5","regionid":"Region_2","gender":"OTHER"}
% Headers: [task.generation="0" task.id="0" current.iteration="852"]
��{"registertime":1488763838678,"userid":"User_3","regionid":"Region_6","gender":"FEMALE"}
% Headers: [task.generation="0" task.id="0" current.iteration="853"]
��{"registertime":1490689265991,"userid":"User_4","regionid":"Region_6","gender":"OTHER"}
% Headers: [task.generation="0" task.id="0" current.iteration="854"]
```

### Cleanup

Delete connect cluster, kafka topic, kafka cluster and api key

```bash
$ confluent connect cluster delete lcc-znwrkd
$ confluent kafka topic delete users
$ confluent kafka cluster delete lkc-0w6755
$ confluent api-key delete CI4JADFSDFQZXO3U
```

------------------------------------ API Keys --------------------------------------------------------------------------

- API keys for Confluent Cloud can be created with user and service accounts.
- A service account is intended to provide an identity for an application or service that needs to perform programmatic operations within Confluent Cloud.
- In production ensure that only service account API keys are used
- Avoid user account API keys, except for development and testing. 
- Regularly review and clean up your existing API keys and service accounts.
- Confluent Cloud audit logs - monitor authorization and authentication events


List service accounts and their api keys

```bash
$ confluent iam service-account list
$ confluent iam 
```

List users

```bash
$ confluent iam user list
```