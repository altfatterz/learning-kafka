Confluent Cloud Documentation:

https://docs.confluent.io/cloud/current/overview.html


```bash
$ confluent version

Version:     v3.65.0
Git Ref:     0f3cd7ca
Build Date:  2024-07-09T21:50:56Z
Go Version:  go1.22.2 (darwin/amd64)
Development: false

$ confluent update
```

```bash
confluent login --save
cat ~/.ccloud/config.json
```

```bash
$ confluent help
$ confluent admin promo list
$ confluent environment list
$ confluent api-key list
```

Schema Registry is created per `enviroment`

export API_KEY=
export API_SECRET=

List all subjects in your Schema Registry:

```bash
$ curl -s -u $API_KEY:$API_SECRET$ GET https://psrc-9zg5y.europe-west3.gcp.confluent.cloud/subjects
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


### Kafka cluster 

```bash
$ confluent kafka cluster list
```

