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
$ helm install my-postgresql bitnami/postgresql -f debezium/values.yaml
$ helm status my-postgresql

PostgreSQL can be accessed via port 5432 on the following DNS names from within your cluster:

    my-postgresql.kafka.svc.cluster.local - Read/Write connection

To get the password for "postgres" run:

    export POSTGRES_PASSWORD=$(kubectl get secret --namespace kafka my-postgresql -o jsonpath="{.data.postgres-password}" | base64 -d)

To connect to your database run the following command:

    kubectl run my-postgresql-client --rm --tty -i --restart='Never' --namespace kafka --image docker.io/bitnami/postgresql:15.1.0-debian-11-r0 --env="PGPASSWORD=$POSTGRES_PASSWORD" \
      --command -- psql --host my-postgresql -U postgres -d postgres -p 5432

    > NOTE: If you access the container using bash, make sure that you execute "/opt/bitnami/scripts/postgresql/entrypoint.sh /bin/bash" in order to avoid the error "psql: local user with ID 1001} does not exist"

To connect to your database from outside the cluster execute the following commands:

    kubectl port-forward --namespace kafka svc/my-postgresql 5432:5432 &
    PGPASSWORD="$POSTGRES_PASSWORD" psql --host 127.0.0.1 -U postgres -d postgres -p 5432
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

INFO[0001] Retrieving image manifest quay.io/strimzi/kafka:0.33.2-kafka-3.2.3
INFO[0001] Retrieving image quay.io/strimzi/kafka:0.33.2-kafka-3.2.3 from registry quay.io
INFO[0002] Built cross stage deps: map[]
INFO[0002] Retrieving image manifest quay.io/strimzi/kafka:0.33.2-kafka-3.2.3
INFO[0002] Returning cached image manifest
INFO[0002] Executing 0 build triggers
INFO[0002] Building stage 'quay.io/strimzi/kafka:0.33.2-kafka-3.2.3' [idx: '0', base-idx: '-1']
INFO[0002] Unpacking rootfs as cmd RUN 'mkdir' '-p' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0'       && 'curl' '-f' '-L' '--output' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz' 'https://repo1.maven.org/maven2/io/debezium/debezium-connector-postgres/2.1.2.Final/debezium-connector-postgres-2.1.2.Final-plugin.tar.gz'       && 'tar' 'xvfz' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz' '-C' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0'       && 'rm' '-vf' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz' requires it.
INFO[0017] USER root:root
INFO[0017] Cmd: USER
INFO[0017] RUN 'mkdir' '-p' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0'       && 'curl' '-f' '-L' '--output' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz' 'https://repo1.maven.org/maven2/io/debezium/debezium-connector-postgres/2.1.2.Final/debezium-connector-postgres-2.1.2.Final-plugin.tar.gz'       && 'tar' 'xvfz' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz' '-C' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0'       && 'rm' '-vf' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz'
INFO[0017] Initializing snapshotter ...
INFO[0017] Taking snapshot of full filesystem...
INFO[0018] Cmd: /bin/sh
INFO[0018] Args: [-c 'mkdir' '-p' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0'       && 'curl' '-f' '-L' '--output' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz' 'https://repo1.maven.org/maven2/io/debezium/debezium-connector-postgres/2.1.2.Final/debezium-connector-postgres-2.1.2.Final-plugin.tar.gz'       && 'tar' 'xvfz' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz' '-C' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0'       && 'rm' '-vf' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz']
INFO[0018] Util.Lookup returned: &{Uid:0 Gid:0 Username:root Name: HomeDir:/root}
INFO[0018] Performing slow lookup of group ids for root
INFO[0018] Running: [/bin/sh -c 'mkdir' '-p' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0'       && 'curl' '-f' '-L' '--output' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz' 'https://repo1.maven.org/maven2/io/debezium/debezium-connector-postgres/2.1.2.Final/debezium-connector-postgres-2.1.2.Final-plugin.tar.gz'       && 'tar' 'xvfz' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz' '-C' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0'       && 'rm' '-vf' '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz']
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 3852k  100 3852k    0     0  8505k      0 --:--:-- --:--:-- --:--:-- 8524k
debezium-connector-postgres/CHANGELOG.md
debezium-connector-postgres/CONTRIBUTE.md
debezium-connector-postgres/COPYRIGHT.txt
debezium-connector-postgres/LICENSE-3rd-PARTIES.txt
debezium-connector-postgres/LICENSE.txt
debezium-connector-postgres/README.md
debezium-connector-postgres/README_JA.md
debezium-connector-postgres/README_KO.md
debezium-connector-postgres/README_ZH.md
debezium-connector-postgres/postgres.json
debezium-connector-postgres/debezium-core-2.1.2.Final.jar
debezium-connector-postgres/debezium-api-2.1.2.Final.jar
debezium-connector-postgres/postgresql-42.5.1.jar
debezium-connector-postgres/protobuf-java-3.19.6.jar
debezium-connector-postgres/debezium-connector-postgres-2.1.2.Final.jar
removed '/opt/kafka/plugins/debezium-postgres-connector/72e697f0.tgz'
INFO[0018] Taking snapshot of full filesystem...
INFO[0019] USER 1001
INFO[0019] Cmd: USER
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

Resources:
1. [https://strimzi.io/blog/2020/01/27/deploying-debezium-with-kafkaconnector-resource/](https://strimzi.io/blog/2020/01/27/deploying-debezium-with-kafkaconnector-resource/)
2. [https://strimzi.io/docs/operators/latest/deploying.html](https://strimzi.io/docs/operators/latest/deploying.html)


Next steps:
- ACLs - I can update the demo
- How to generate the image with the plugin
- Make the demo work in the Centris environment

