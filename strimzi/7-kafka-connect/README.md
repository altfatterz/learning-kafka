Deploying Kafka Connect

The `Cluster Operator` manages Kafka Connect clusters deployed using the `KafkaConnect` resource and connectors created using the `KafkaConnector` resource. [https://strimzi.io/docs/operators/latest/deploying.html#kafka-connect-str](https://strimzi.io/docs/operators/latest/deploying.html#kafka-connect-str)

### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
$ k3d cluster create mycluster --agents 1
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
# create the `kafka` namespace
$ kubectl create ns kafka
```

### 2. Install the [Strimzi](https://strimzi.io/) operator

```bash
$ kubectl create ns kafka
$ kubectl config set-context --current --namespace=kafka
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 3. Deploy a simple Kafka cluster (1 broker/1 zookeeper)

```bash
$ kubectl apply -n kafka -f kafka-ephemeral-single.yaml 
```

### 4. Deploy Kafka Connect cluster (1 instance)
 
```bash
$ kubectl apply -f kafka-connect-with-source-connector.yaml -n kafka
```

Notice that first `my-connect-cluster-connect-build` job is create to create a custom image and upload it to the
`ttl.sh/altfatterz-strimzi-kafka-connect-3.2.3:2h` (valid for 2 hours)

Check in the logs what is doing:

```bash
$ logs -f my-connect-cluster-connect-build -n kafka
```

Then a `my-connect-cluster-connect-7d6b9b7c8c-5z5xb` pod is created which is the actual Connect Instance with the File Source Connector installed on it.

### 5. Create the `my-topic` via

```bash
$ kubectl apply -f kafka-topic.yaml -n kafka
$ kubectl get kt -n kafka
```

### 6. Start a consumer

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.31.1-kafka-3.2.3 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```

### 7. Deploy a `KafkaConnector` resource which represents the actual job

```bash
$ kubectl apply -f kafka-source-connector.yaml -n kafka
```

In the consumer logs you should see the messages from the topic

```bash
{"schema":{"type":"string","optional":false},"payload":"This product contains the dnsinfo.h header file, that provides a way to retrieve the system DNS configuration on MacOS."}
{"schema":{"type":"string","optional":false},"payload":"This private header is also used by Apple's open source"}
{"schema":{"type":"string","optional":false},"payload":" mDNSResponder (https://opensource.apple.com/tarballs/mDNSResponder/)."}
{"schema":{"type":"string","optional":false},"payload":""}
{"schema":{"type":"string","optional":false},"payload":" * LICENSE:"}
{"schema":{"type":"string","optional":false},"payload":"    * license/LICENSE.dnsinfo.txt (Apple Public Source License 2.0)"}
{"schema":{"type":"string","optional":false},"payload":"  * HOMEPAGE:"}
```

### 8. View the relevant resources:

```bash
$ kubectl api-resources | grep connect

kafkaconnectors                   kctr         kafka.strimzi.io/v1beta2               true         KafkaConnector
kafkaconnects                     kc           kafka.strimzi.io/v1beta2               true         KafkaConnect

$ kubectl get kc -n kafka

NAME                 DESIRED REPLICAS   READY
my-connect-cluster   1                  True


$ kubectl get kctr -n kafka

NAME                  CLUSTER              CONNECTOR CLASS                                           MAX TASKS   READY
my-source-connector   my-connect-cluster   org.apache.kafka.connect.file.FileStreamSourceConnector   2           True

$ kubectl describe kc my-connect-cluster -n kafka
$ kubectl describe kctr my-source-connector -n kafka 
```


### Install postgresql 

```bash
$ helm install my-postgresql bitnami/postgresql

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

UPDATE customers SET email = 'johndoe1000@gmail.com' WHERE first_name = 'John' And last_name = 'Doe';
COMMIT;
```


### ### 4. Deploy Kafka Connect cluster with Debezium plugin (1 instance) 

```bash
$ kubectl apply -f kafka-connect-with-debezium.yaml -n kafka
```

```bash
$  kubectl port-forward svc/my-connect-cluster-connect-api 8083:8083 -n kafka
```

```bash
$ kubeclt apply -f kafka-source-connector-debezium.yaml -n kafka

2023-02-24 20:33:20,999 INFO Successfully tested connection for jdbc:postgresql://my-postgresql:5432/postgres with user 'postgres' (io.debezium.connector.postgresql.PostgresConnector) [pool-2-thread-1]
2023-02-24 20:33:20,999 ERROR Postgres server wal_level property must be "logical" but is: replica (io.debezium.connector.postgresql.PostgresConnector) [pool-2-thread-1]
2023-02-24 20:33:21,003 INFO Connection gracefully closed (io.debezium.jdbc.JdbcConnection) [pool-6-thread-1]
```

Info about `wal_level (enum)` https://www.postgresql.org/docs/9.6/runtime-config-wal.html

```bash
select name,setting from pg_settings where name = 'wal_level';
```

```bash
$ ALTER DATABASE dbnane SET wal_level TO logical
```

You need to restart 
```bash
$ k scale sts my-postgresql --replicas=0
$ k scale sts my-postgresql --replicas=1
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
