# Confluent Cloud Documentation:

- Documentation: https://docs.confluent.io/cloud/current/overview.html

- Confluent Cloud APIs: https://docs.confluent.io/cloud/current/api.html#section/Introduction 


### Install

```bash
# use install 
$ brew install confluentinc/tap/cli
# or upgrade if you already installed it
$ brew upgrade confluentinc/tap/cli
```

### Version

```bash
$ confluent version

Version:     v4.55.0
Git Ref:     4fb7a0a9
Build Date:  2026-03-17T22:42:46Z
Go Version:  go1.25.7 (darwin/arm64)
Development: false
```

### Login

```bash
confluent login --save
cat ~/.confluent/config.json
```

```bash
$ confluent help
$ confluent billing promo list
$ confluent environment list
# set default environment
$ confluent environment use <>
$ confluent kafka cluster list
# set default kafka cluster
$ confluent kafka cluster use <>
 
$ confluent kafka  region list
$ confluent kafka  region list | grep Zurich
  Zurich (eu-central-2)          | aws   | eu-central-2
  Zurich (europe-west6)          | gcp   | europe-west6
  Zurich (switzerlandnorth)      | azure | switzerlandnorth

# You must pass --cluster with the command or set an active Kafka cluster in your context with confluent kafka cluster use.
$ confluent kafka topic list
```

### IAM

```bash
# Manage all the users within your organization
$ confluent iam user

# Manager all service accounts
$ confluent iam service-accounts

# Manage role bindings for principals
$ confluent iam rbac role-binding
```

### API-KEYS

Good explanation about api-keys: https://docs.confluent.io/cloud/current/api.html#section/Authentication/API-keys

Each Confluent Cloud API key is associated with a `principal` (specific user or service account) and inherits the permissions granted to the owner.

Two types:

- `Cloud API Key` 
  - A Cloud API key is at the Organization level and is only used for REST API clients.
  - grants access to the Confluent Cloud Management APIs, such as for `Provisioning` and `Metrics` integrations.
  - `confluent api-key create --resource cloud`

- `Resource specific api key`
  - grants access to:
    - a Confluent Kafka cluster (Kafka API key), 
      - associated to user account: `confluent api-key create --resource lkc-123456`
      - associated to service account: `confluent api-key create --resource lkc-123456 --service-account sa-123456`
    - a Confluent Cloud Schema Registry (Schema Registry API key), - `confluent api-key create --resource lsrc-123456`
    - Flink (Flink API key scoped to an Environment + Region pair), - `confluent api-key create --resource flink --cloud aws --region us-east-1`
      - Required to access the Flink compute pools and statements in a specified region.
    - a ksqlDB application. - `confluent api-key create --resource lksqlc-123456`
      - Required to interact with your ksqlDB applications in Confluent Cloud.
    - TableFlow - `confluent api-key create --resource tableflow`
      - Required to interact with Tableflow Iceberg REST catalog.

```bash
# Manage the API keys of all resources (cluster, SR, ksqlDB, Cloud metrics)
$ confluent api-key list

  Current |       Key        |         Description          |  Owner   |     Owner Email      | Resource Type |  Resource  |       Created
----------+------------------+------------------------------+----------+----------------------+---------------+------------+-----------------------
          | EE44WAH5G3MDFWXO | create in confluent cloud ui | u-422714 | altfatterz@gmail.com | kafka         | lkc-k95pwg | 2026-03-19T15:24:18Z
          | QE65I5UTKFYMGZCO | created using cli            | u-422714 | altfatterz@gmail.com | kafka         | lkc-k95pwg | 2026-03-19T15:16:21Z 
```

- When api keys are created with Confluent Cloud UI, they are visible (they can be listed) but their associated secret needs to be stored in the CLI
- When api keys are created with CLI their associated secret is stored automatically 

```bash
$ confluent api-key store EE44WAH5G3MDFWXO cfltL3cduVkBj4FFPOEpzKuhv6n5aiMNAt7qapNfUDJ+qjmcvclyD3msF/PrOHog
$ confluent api-key use EE44WAH5G3MDFWXO
```

Best practices for Using API Keys

- Ensure only service account API keys are used in production
- User account API keys recommended only for development and testing 
- Delete unneeded API keys and service accounts
- Rotate API keys regularly:
  - Create a new API key
  - Update the resource or application to use the new API key 
  - Delete the old API key

- If a user leaves and a user account is deleted, all API keys created with that user account are deleted, which might break applications.

### Producer / Consumer

```bash
# without capture keys / values
$ confluent kafka topic produce cli-test
# with keys / values
$ confluent kafka topic produce cli-test --parse-key --delimiter ":"

# consume cli-test
$ confluent kafka topic consume --from-beginning --print-key --delimiter ":" cli-test
```

### Confluent Environment

Stream Governance package, more info here: https://docs.confluent.io/cloud/current/stream-governance/packages.html

- `Essentials` - free
- `Advanced` - starting $1/hr

```bash
# Manage confluent environments
$ confluent environment
```

### Kafka clusters

```bash
# Manage Kafka
$ confluent kafka
```

### Cluster Types

- Basic
- Standard
- Enterprise
- Freight
- Dedicated

### eCKU vs CKUs

- CKU stands for `Confluent Unit for Kafka`
- eCKUs/CKUs are units of horizontal scalability. 
- eCKUs or Elastic CKUs instantly autoscale up to meet demand and back down to zero when unused so `you pay only for the resources you use` when you actually need them. 
- CKUs are pre-provisioned capacity units that `are billed based on resources provisioned`

More info here: https://docs.confluent.io/cloud/current/billing/overview.html?ajs_aid=e2f60d85-3bbb-450a-b450-461daee489f2&ajs_uid=1160#compare-billing-units-for-ak-clusters


### Kafka CLI Tools with Confluent Cloud

```bash
export BOOTSTRAP_SERVERS=TODO
export CLUSTER_API_KEY=TODO
export CLUSTER_API_SECRET=TODO

$ envsubst < kafka-cli.properties.template > kafka-cli.properties

$ kafka-topics --bootstrap-server=$BOOTSTRAP_SERVERS --command-config kafka-cli.properties --create --topic demo-topic --create --partitions 3 --replication-factor 2
$ kafka-console-producer --bootstrap-server=$BOOTSTRAP_SERVERS --producer.config kafka-cli.properties --topic demo-topic --property parse.key=true --property key.separator=":"
$ kafka-console-consumer --bootstrap-server=$BOOTSTRAP_SERVERS --consumer.config kafka-cli.properties --topic demo-topic --from-beginning --group console-consumer --property print.key=true --property key.separator=":"
$ kafka-consumer-groups --bootstrap-server=$BOOTSTRAP_SERVERS --command-config kafka-cli.properties --list
$ kafka-consumer-groups --bootstrap-server=$BOOTSTRAP_SERVERS --command-config kafka-cli.properties --describe --group console-consumer
```

------------------------------------------------------------------------------------------------------------------------

## Schema Registry

```bash
# get schema-registry connect info, schema registry is per environment
$ confluent schema-registry cluster describe

$ confluent api-key create 
```

Schema Registry is created per `enviroment`

List all subjects in your Schema Registry:

```bash
$ export API_KEY=<API_KEY>
$ export API_SECRET=<APS_SECRET>
$ curl -s -u $API_KEY:$API_SECRET https://psrc-9zg5y.europe-west3.gcp.confluent.cloud/subjects
```

Register an Avro schema under the subject `kafka-value`:

```bash
$ curl -s -u $API_KEY:$API_SECRET POST -H "Content-Type: application/vnd.schemaregistry.v1+json" https://psrc-9zg5y.europe-west3.gcp.confluent.cloud/subjects/kafka-value/versions --data '{"schema": "{\"type\": \"string\"}", "schemaType": "AVRO" }'
```

Fetch the latest version of the schema registered under subject `kafka-value`

```bash
$ curl -s -u $API_KEY:$API_SECRET GET https://psrc-9zg5y.europe-west3.gcp.confluent.cloud/subjects/kafka-value/versions/latest
```

Delete all schema versions registered under the subject `kafka-value`

```bash
$ curl -s -u $API_KEY:$API_SECRET -X DELETE https://psrc-9zg5y.europe-west3.gcp.confluent.cloud/subjects/kafka-value/
```

Schema Registry reference: https://docs.confluent.io/platform/current/schema-registry/develop/api.html


### Commercial Workshops from Confluent

https://github.com/confluentinc/commercial-workshops/


