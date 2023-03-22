Deploying Kafka Connect

- Source Connector
    - task polls the external data system and returns a list of records that a worker sends to the Kafka brokers.
---------------------------------------------------------------------------
PostgreSQL ----------> Kafka Connect (Debezium Plugin) -------> Kafka Topic 
---------------------------------------------------------------------------

- Sink Connector 
  - task receives Kafka records from a worker for writing to the external data system

---------------------------------------------------------------------------
Kafka Topic ------------------> Kafka Connect --------------> Elasticsearch
---------------------------------------------------------------------------

- Kafka Connect cluster is basically a `K8S Deployment` (1 or more pods) - they are also called `Worker Nodes` 
- Worker Nodes distribute the streaming `tasks`
- If there are more `tasks` than `workers`, `workers` are assigned multiple `tasks`.
- If a `worker` fails, its `tasks` are automatically assigned to active `workers` in the `Kafka Connect cluster`.

Things to remember:
- `Connectors` to create `tasks` (We are going to use Debezium Connector or also called plugin)
- `Tasks` move the data
- `Workers` run the `tasks`
- `Transforms` transform external data 
  - `Source connectors` apply transforms before converting data into a format supported by Kafka.
  - `Sink connectors` apply transforms after converting data into a format suitable for an external data system.
- `Converters` converts the data (JSON / Avro)

Good picture here: [https://strimzi.io/docs/operators/latest/overview.html#connectors](https://strimzi.io/docs/operators/latest/overview.html#connectors)

- Sink connectors, the number of `tasks` created relates to the number of partitions being consumed.
- Source connectors, how the source data is partitioned is defined by the connector, see `tasksMax` 
(maximum number of tasks that can run in parallel). The Connector might create fewer tasks if not possible 
to split the source data into that many partitions.


The `Cluster Operator` manages Kafka Connect clusters deployed using the `KafkaConnect` resource and connectors created using the `KafkaConnector` resource. [https://strimzi.io/docs/operators/latest/deploying.html#kafka-connect-str](https://strimzi.io/docs/operators/latest/deploying.html#kafka-connect-str)

### Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
$ k3d cluster create mycluster --agents 1
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
# create the `kafka` namespace
$ kubectl create ns kafka
# set default namespace to kafka
$ kubectl config set-context --current --namespace=kafka
```

### Watch the namespace

```bash
$ watch kubectl get pods
```

### Install PostgresSQL 

```bash
$ helm install my-postgresql bitnami/postgresql -f values.yaml
$ helm status my-postgresql
```

### Connect to PostgreSQL

```bash
$ kubectl port-forward --namespace kafka svc/my-postgresql 5432:5432
$ export POSTGRES_PASSWORD=$(kubectl get secret --namespace kafka my-postgresql -o jsonpath="{.data.postgres-password}" | base64 -d)
$ PGPASSWORD="$POSTGRES_PASSWORD" psql --host 127.0.0.1 -U postgres -d postgres -p 5432
```

### Create a table with some data

```bash
CREATE TABLE customers (
  id SERIAL,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  PRIMARY KEY(id)
);
  
INSERT INTO customers(first_name, last_name, email) VALUES ('John', 'Doe', 'johndoe@gmail.com');
COMMIT;

INSERT INTO customers(first_name, last_name, email) VALUES ('Jane', 'Doe', 'janedoe@gmail.com');
COMMIT;

SELECT * FROM CUSTOMERS;
```

### Install the [Strimzi](https://strimzi.io/) operator

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' 
```

### Deploy a simple Kafka cluster (1 broker/1 zookeeper)

```bash
$ kubectl apply -f kafka-ephemeral-single.yaml 
```

### Deploy Kafka Connect cluster (1 instance) with Debezium plugin
 
```bash
$ kubectl apply -f debezium/kafka-connect-with-debezium.yaml
```

Notice that first `my-connect-cluster-connect-build` job is create to create a custom image and upload it to the
`ttl.sh` (valid for 2 hours)

Check in the logs what is doing:

```bash
$ kubectl logs -f my-connect-cluster-connect-build
...
INFO[0019] Pushing image to ttl.sh/altfatterz-strimzi-kafka-connect-debezium-demo-3.2.3:2h
INFO[0043] Pushed ttl.sh/altfatterz-strimzi-kafka-connect-debezium-demo-3.2.3@sha256:3ba07438591f2f7f3a2e46d0fc84841d59f6c8d6222d1dcc8d9098a629bae32f
```

Then a `my-connect-cluster-connect-7d6b9b7c8c-5z5xb` pod is created which is the actual `Connect Instance` 
with the Debezium source connector installed on it.

Look around in the container:

```bash
$ kubectl exec -it my-connect-cluster-connect-68d4657899-mfpcd -- sh
## the debezium source connector is installed here:
$ ls -l /opt/kafka/plugins
```

Expose the `Kafka Connect` API 

```bash
$ kubectl port-forward svc/my-connect-cluster-connect-api 8083:8083
$ curl localhost:8083/connectors
```

Verify the new resource created:

```bash
$ kubectl api-resources | grep kafka
$ kubectl get kc
$ kubectl get kctr
$ kubectl describe kc my-connect-cluster
```


### Deploy a `KafkaConnector` resource which represents the actual job

Before deploying check stream the logs of `Kafka Connect` instance:
```bash
$ k logs -f my-connect-cluster-connect-68d4657899-mfpcd
```

```bash
# check the password connection properties are ok ! -- TODO externalise
$ kubectl apply -f debezium/kafka-source-connector-debezium.yaml 
```

Issues with `wal_level` property - PostgreSQL specific
More info here: [https://stackoverflow.com/questions/71214664/how-to-change-bitnami-postgresql-helm-chart-configs-ph-hba-conf-postgresql-co](https://stackoverflow.com/questions/71214664/how-to-change-bitnami-postgresql-helm-chart-configs-ph-hba-conf-postgresql-co)

In the logs you should see:

```bash
2023-03-02 20:33:12,975 INFO Successfully tested connection for jdbc:postgresql://my-postgresql:5432/postgres with user 'postgres' (io.debezium.connector.postgresql.PostgresConnector) [pool-2-thread-19]
```

### View topics: 

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.33.2-kafka-3.2.3 --rm=true --restart=Never -- bin/kafka-topics.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --list

__consumer_offsets
__strimzi-topic-operator-kstreams-topic-store-changelog
__strimzi_store_topic
connect-cluster-configs
connect-cluster-offsets
connect-cluster-status
postgresql.public.customers
````

### Consume from the created `postgresql.public.customers` topic

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.31.1-kafka-3.2.3 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic postgresql.public.customers --from-beginning
```


### Start a consumer

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.31.1-kafka-3.2.3 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```

### Check the connector config:

```bash
$ curl localhost:8083/connectors
$ curl localhost:8083/connectors/my-source-connector
```

```json
{
  "name": "my-source-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.dbname": "postgres",
    "database.user": "postgres",
    "topic.prefix": "postgresql",
    "database.hostname": "my-postgresql",
    "tasks.max": "1",
    "database.password": "U1Rzn6nRMf",
    "name": "my-source-connector",
    "database.port": "5432",
    "plugin.name": "pgoutput"
  },
  "tasks": [
    {
      "connector": "my-source-connector",
      "task": 0
    }
  ],
  "type": "source"
}
```

Do an update and delete

```bash
$ UPDATE customers SET email = 'janedoe1@gmail.com' WHERE first_name = 'Jane' AND last_name = 'Doe';
$ DELETE FROM customers WHERE first_name = 'Jane' AND last_name = 'Doe';
```

Delete topics:

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.33.2-kafka-3.2.3 --rm=true --restart=Never -- bin/kafka-topics.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --delete --topic postgresql.public.customers

```


### Apicurio Registry Operator & Apicurio Registry

```bash
$ kubectl apply -f apicurio-registry-operator.yaml
$ kubectl apply -f apicurio-registry.yaml
```

```bash
$ kubectl port-forward --namespace kafka svc/my-postgresql 5432:5432
$ kubectl port-forward svc/my-connect-cluster-connect-api 8083:8083
$ kubectl port-forward svc/apicurio-registry-service 8080:8080
$ kubectl port-forward svc/my-cluster-kafka-bootstrap 9092:9092
-- needed to change /etc/hosts to be able to connect 
127.0.0.1 my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc 
```

Check with browser:

```bash
http://localhost:8080/
http://localhost:8080/apis/
```

# Kafka Connect and JDBC Sink Connector

```bash
$ kubectl apply -f kafka-connect.yaml
$ kubectl apply -f kafka-jdbc-sink-connector.yaml
```

Useful commands:
```bash
$ kcat -L -b localhost:9092 -t my-topic
$ kcat -C -b localhost:9092 -t my-topic

$ http put :8083/connectors/my-source-connector/pause
$ http put :8083/connectors/my-source-connector/resume

$ http get :8083/connector-plugins/
```

Current issue:

```bash
Caused by: io.apicurio.registry.rest.client.exception.RestClientException: RESTEASY003210: Could not find resource for full path: http://apicurio-registry-service:8080/apis/registry/v2/ids/globalIds/1/references

Similar issue: https://github.com/quarkusio/quarkus/issues/25814

After upgrading to Apicurio Version 2.4.1.Final the issue does not occur.
```




Resources:
1. [https://strimzi.io/blog/2020/01/27/deploying-debezium-with-kafkaconnector-resource/](https://strimzi.io/blog/2020/01/27/deploying-debezium-with-kafkaconnector-resource/)
2. [https://strimzi.io/docs/operators/latest/deploying.html](https://strimzi.io/docs/operators/latest/deploying.html)


Next steps:
- ACLs - I can update the demo
- How to generate the image with the plugin
- Make the demo work in the Centris environment

