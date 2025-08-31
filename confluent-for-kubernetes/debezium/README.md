### CFK example with Connector

### Docker 

Have a docker environment. In this example Docker Desktop was used with 8 CPU / 16 GB Memory.  

### Create a k8s cluster and 'confluent' namespace

```bash
$ k3d cluster create confluent -p "9021:80@loadbalancer"
$ kubectl create ns confluent
$ kubectl config set-context --current --namespace confluent
```

### Pull confluent images first into the single node k8s cluster

```bash
./import-images.sh
```

### Verify imported images:

```bash
$ docker exec k3d-confluent-server-0 crictl images | grep confluentinc
```

### Expose Control Center via Ingress using Traefik Controller (built in using k3d) 

You will be able to access Control Center on `http://localhost:9021` without any port-forwarding 

```bash
$ kubectl apply -f ingress.yaml
```

### Install PostgresSQL

```bash
$ helm install my-postgresql oci://registry-1.docker.io/bitnamicharts/postgresql -f postgresql-values.yaml 
$ helm status my-postgresql
```

### Connect to PostgreSQL

```bash
$ kubectl port-forward svc/my-postgresql 5432:5432
$ export POSTGRES_PASSWORD=$(kubectl get secret my-postgresql -o jsonpath="{.data.postgres-password}" | base64 -d)
$ PGPASSWORD="$POSTGRES_PASSWORD" psql --host localhost -U postgres -d postgres -p 5432
```

### Uninstall PostgreSQL (if needed)

```
$ helm uninstall my-postgresql
$ kubectl delete pvc data-my-postgresql-0
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
INSERT INTO customers(first_name, last_name, email) VALUES ('Jane', 'Doe', 'janedoe@gmail.com');
COMMIT;

SELECT * FROM CUSTOMERS;
```

### Install CFK 

```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes
$ helm list
NAME              	NAMESPACE	REVISION	UPDATED                              	STATUS  	CHART                            	APP VERSION
confluent-operator	confluent	1       	2025-08-31 12:41:50.2496 +0200 CEST  	deployed	confluent-for-kubernetes-0.1263.8	3.0.0
$ kubectl get all
# wait until the confluent-operator pod is started 
```

### Install Confluent Platform

```bash
# Create secret for used by the 'connector' and 'debezium-source-connector'
$ kubectl create secret generic sqlcreds --from-file=sqlcreds.txt=sqlcreds.txt
$ kubectl get secret sqlcreds -o yaml
```

```bash
$ kubectl apply -f confluent-platform-base.yaml
# wait until controller kafka nodes are up
$ kubectl apply -f confluent-platform-schemaregistry.yaml
$ kubectl apply -f confluent-platform-connect.yaml
$ kubectl apply -f confluent-platform-controlcenter.yaml

$ kubectl get pods
NAME                                  READY   STATUS    RESTARTS   AGE
confluent-operator-7584795cfd-s6jll   1/1     Running   0          7m15s
connect-0                             1/1     Running   0          80s
controlcenter-0                       1/1     Running   0          5m6s
kafka-0                               1/1     Running   0          6m6s
kafka-1                               1/1     Running   0          6m6s
kafka-2                               1/1     Running   0          6m6s
kraftcontroller-0                     1/1     Running   0          6m51s
my-postgresql-0                       1/1     Running   0          10m
schemaregistry-0                      1/1     Running   0          5m13s
```

### Trouble shoot connect-0 debezium install

```bash
$ kubectl get connect 

NAME      REPLICAS   READY   STATUS    AGE    KAFKA
connect   1          1       RUNNING   2m1s   kafka:9071

$ kubectl logs -f connect-0 -c config-init-container
$ kubectl logs -f connect-0
```

### Create topics

```bash
$ kubectl apply -f topics.yaml
$ kubectl get topic
NAME                         REPLICAS   PARTITION   STATUS    CLUSTERID                AGE
prefix.public.buzzwords      1          1           CREATED   ZjQwOWM0NjQtMDczMS00Ng   7s
prefix.public.catchphrases   1          1           CREATED   ZjQwOWM0NjQtMDczMS00Ng   7s
prefix.public.customers      1          1           CREATED   ZjQwOWM0NjQtMDczMS00Ng   7s
prefix.public.facts          1          1           CREATED   ZjQwOWM0NjQtMDczMS00Ng   7s
prefix.public.heros          1          1           CREATED   ZjQwOWM0NjQtMDczMS00Ng   7s

$ kubectl exec kafka-0 -c kafka -- kafka-topics --bootstrap-server localhost:9092  --list | grep prefix

$ kubectl exec kafka-0 -c kafka -- kafka-topics --bootstrap-server localhost:9092  --list | grep connect
```

### Install connector

```bash
$ kubectl apply -f debezium-source-connector.yaml
$ kubectl get connectors -o wide 

NAME                        STATUS    CONNECTORSTATUS   TASKS-READY   AGE   CONNECTENDPOINT   TASKS-FAILED   WORKERID                                             RESTARTPOLICY   KAFKACLUSTERID
debezium-source-connector   CREATED   RUNNING           1/1           13s                                    connect-0.connect.confluent.svc.cluster.local:8083   OnFailure       ZjQwOWM0NjQtMDczMS00Ng

```

### Check topic: (initial load)

```bash
$ 
$ kubectl exec kafka-0 -c kafka -- kafka-console-consumer --bootstrap-server localhost:9092 --topic prefix.public.customers --from-beginning
```

```json
{
  "before": null,
  "after": {
    "id": 1,
    "first_name": "John",
    "last_name": "Doe",
    "email": "johndoe@gmail.com"
  },
  "source": {
    "version": "2.5.4.Final",
    "connector": "postgresql",
    "name": "prefix",
    "ts_ms": 1756639185525,
    "snapshot": "false",
    "db": "postgres",
    "sequence": "[\"22301472\",\"22501792\"]",
    "schema": "public",
    "table": "customers",
    "txId": 757,
    "lsn": 22501792,
    "xmin": null
  },
  "op": "c",
  "ts_ms": 1756639324348,
  "transaction": null
},
{
  "before": null,
  "after": {
    "id": 2,
    "first_name": "Jane",
    "last_name": "Doe",
    "email": "janedoe@gmail.com"
  },
  "source": {
    "version": "2.5.4.Final",
    "connector": "postgresql",
    "name": "prefix",
    "ts_ms": 1756639193806,
    "snapshot": "false",
    "db": "postgres",
    "sequence": "[\"22502088\",\"22502088\"]",
    "schema": "public",
    "table": "customers",
    "txId": 758,
    "lsn": 22502088,
    "xmin": null
  },
  "op": "c",
  "ts_ms": 1756639324351,
  "transaction": null
}
```

### Capture a delete

```bash
$ delete from customers where id = 1;
```

Event in `prefix.public.customers` topic

```json
{
  "before": {
    "id": 1,
    "first_name": "",
    "last_name": "",
    "email": ""
  },
  "after": null,
  "source": {
    "version": "2.5.4.Final",
    "connector": "postgresql",
    "name": "prefix",
    "ts_ms": 1756639658304,
    "snapshot": "false",
    "db": "postgres",
    "sequence": "[\"22502288\",\"22502608\"]",
    "schema": "public",
    "table": "customers",
    "txId": 759,
    "lsn": 22502608,
    "xmin": null
  },
  "op": "d",
  "ts_ms": 1756639658641,
  "transaction": null
}
```

### Capture an update 

```bash
$ update customers set first_name = 'Mary' where id = 2; 
```

```json
{
  "before": null,
  "after": {
    "id": 2,
    "first_name": "Mary",
    "last_name": "Doe",
    "email": "janedoe@gmail.com"
  },
  "source": {
    "version": "2.5.4.Final",
    "connector": "postgresql",
    "name": "prefix",
    "ts_ms": 1756639742575,
    "snapshot": "false",
    "db": "postgres",
    "sequence": "[\"22502872\",\"22503136\"]",
    "schema": "public",
    "table": "customers",
    "txId": 760,
    "lsn": 22503136,
    "xmin": null
  },
  "op": "u",
  "ts_ms": 1756639742951,
  "transaction": null
}
```

### Capture an insert:

```bash
INSERT INTO customers(first_name, last_name, email) VALUES ('XXX', 'Doe', 'johndoe@gmail.com');
```

In topic:

```json
{
  "before": null,
  "after": {
    "id": 3,
    "first_name": "XXX",
    "last_name": "Doe",
    "email": "johndoe@gmail.com"
  },
  "source": {
    "version": "2.5.4.Final",
    "connector": "postgresql",
    "name": "prefix",
    "ts_ms": 1756639787710,
    "snapshot": "false",
    "db": "postgres",
    "sequence": "[\"22503520\",\"22503680\"]",
    "schema": "public",
    "table": "customers",
    "txId": 761,
    "lsn": 22503680,
    "xmin": null
  },
  "op": "c",
  "ts_ms": 1756639787931,
  "transaction": null
}
```

### Check the Connect topics:

```bash
$ kubectl exec kafka-0 -c kafka -- kafka-topics --bootstrap-server localhost:9092  --list | grep connect
$ kubectl exec kafka-0 -c kafka -- kafka-console-consumer --bootstrap-server localhost:9092 --topic confluent.connect-offsets --from-beginning
$ kubectl exec kafka-0 -c kafka -- kafka-console-consumer --bootstrap-server localhost:9092 --topic confluent.connect-status --from-beginning
$ kubectl exec kafka-0 -c kafka -- kafka-console-consumer --bootstrap-server localhost:9092 --topic confluent.connect-configs --from-beginning
```

### Datagen connector

```bash
$ kubectl apply -f datagen-connector-transactions.yaml

$ kubectl get connector
NAME                             STATUS    CONNECTORSTATUS   TASKS-READY   AGE
datagen-connector-transactions   CREATED   RUNNING           1/1           4m23s
debezium-source-connector        CREATED   RUNNING           1/1           22m

# check the topic
$ kubectl exec kafka-0 -c kafka -- kafka-console-consumer --bootstrap-server localhost:9092 --topic prefix.public.transactions
```

### Error scenario1 : PostgreSQL becomes unavailable

Connection issue within Connector and Postgresql

```bash
$ kubectl scale sts my-postgresql --replicas 0 
```

The connector task will fail and stop. Error can be check via

http://localhost:8083/connectors/debezium-source-connector-filter0/tasks/0/status
http://localhost:8083/connectors/debezium-source-connector-filter1/tasks/0/status

When the server is available again, restart the connector using ControlCenter or CLI

### Terms

#### Debezium PostgreSQL connector

- Debezium is a distributed system that captures all changes in multiple upstream databases; it never misses or loses an event.
- When the system is operating normally or being managed carefully then Debezium provides exactly once delivery of every change event record.
- If a fault does happen then the system does not lose any events.
- However, while it is recovering from the fault, it’s possible that the connector might emit some duplicate change events.
- In these abnormal situations, Debezium, like Kafka, provides at least once delivery of change events.

- Debezium uses PostgreSQL’s logical decoding, which uses replication slots.
- Replication slots are guaranteed to retain all WAL segments required for Debezium even during Debezium outages.

- The first time it connects to a PostgreSQL server or cluster, the connector takes a consistent snapshot of all schemas
- After that snapshot is complete, the connector continuously captures row-level changes that insert, update, and delete database content and that were committed to a PostgreSQL database.
- The connector is using the logical decoding feature - which has limitations
  - Logical decoding does not support DDL changes, the connector is unable to report DDL change events back to consumers.
  - Logical decoding replication slots are supported on only primary servers, if the primary server fails you need to restart the connector

#### LSN

- Log Sequence Number
- The PostgreSQL connector externally stores the last processed offset in the form of a PostgreSQL LSN.
- After a connector restarts and connects to a server instance, the connector communicates with the server to continue streaming from that particular offset.
- This offset is available as long as the Debezium replication slot remains intact.
- Never drop a replication slot on the primary server or you will lose data.

#### WAL

- Write Ahead Log, used interchangeably with transaction log

#### PgOutput

- Is the standard logical decoding output plug-in in PostgreSQL 10+
- It is maintained by the PostgreSQL community, and used by PostgreSQL itself for logical replication.
- This plug-in is always present so no additional libraries need to be installed.

### Publications

- Debezium streams change events for PostgreSQL source tables from publications that are created for the tables.
- There are several options for determining how publications are created.
In general, it is best to manually create publications for the tables that you want to capture, before you set up the connector.
-  However, you can configure your environment in a way that permits Debezium to create publications automatically, and to specify the data that is added to them. (See publication.autocreate.mode)

Resources:

- Debezium Postgresql: https://debezium.io/documentation/reference/stable/connectors/postgresql.html
- Configure Kafka Connect for CFK https://docs.confluent.io/operator/current/co-configure-connect.html
- How to install connector plugins in Kafka Connect: https://rmoff.net/2020/06/19/how-to-install-connector-plugins-in-kafka-connect/