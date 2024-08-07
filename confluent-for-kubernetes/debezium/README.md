### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent -p "9021:80@loadbalancer"
$ kubectl cluster-info
$ kubectl create ns confluent
$ kubectl config set-context --current --namespace confluent
```

### Pulling images

Makes sure you these images already on the Docker environment

```bash
./import-images.sh
```

Verify imported images:

```bash
$ docker exec k3d-confluent-server-0 crictl images | grep 7.6.1
$ docker exec k3d-confluent-server-0 crictl images | grep 2.8.0
```

### Expose Control Center via Ingress using Traefik Controller (built in using k3d)

```bash
$ kubectl apply -f ingress.yaml
```

### Install PostgresSQL

```bash
$ helm install my-postgresql bitnami/postgresql -f postgresql-values.yaml
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
  foo1 NUMERIC(1) NOT NULL,
  foo2 NUMERIC(2) NOT NULL,
  foo3 INTEGER NOT NULL,
  foo4 NUMERIC NOT NULL,
  PRIMARY KEY(id)
);
  
INSERT INTO customers(first_name, last_name, email, foo1, foo2, foo3, foo4) VALUES ('John', 'Doe', 'johndoe@gmail.com', 1, 1, 1, 1);
COMMIT;

INSERT INTO customers(first_name, last_name, email, foo1, foo2, foo3, foo4) VALUES ('Jane', 'Doe', 'janedoe@gmail.com', 1, 1, 1, 1);
COMMIT;

SELECT * FROM CUSTOMERS;
```

### Install CFK 

```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes --set kRaftEnabled=true
$ helm list
NAME              	NAMESPACE	REVISION	UPDATED                              	STATUS  	CHART                            	APP VERSION
confluent-operator	confluent	1       	2024-06-17 10:06:45.422168 +0200 CEST	deployed	confluent-for-kubernetes-0.921.20	2.8.2
$ kubectl get all
# wait until the confluent-operator pod is started 
```

### Install Confluent Platform

```bash
$ kubectl apply -f confluent-platform-base.yaml
# wait until controller and broker nodes are up
```

### Create secret for 

```bash
$ kubectl create secret generic sqlcreds --from-file=sqlcreds.txt=sqlcreds.txt
$ kubectl get secret sqlcreds -o yaml
```


```bash
$ kubectl apply -f confluent-platform-schemaregistry.yaml
$ kubectl apply -f confluent-platform-connect.yaml
$ kubectl apply -f confluent-platform-controlcenter.yaml
```

### Trouble shoot connect-0 debezium install

```bash
$ kubectl logs -f connect-0 -c config-init-container
$ kubectl logs -f connect-0
```

### Create Topic:

```bash
$ kubectl apply -f topic.yaml
```

### Install connector

```bash
$ kubectl apply -f debezium-source-connector.yaml
$ kubectl get connectors

NAME                        STATUS    CONNECTORSTATUS   TASKS-READY   AGE
debezium-source-connector   CREATED   RUNNING           1/1           2m18s
```

### Check topic: (initial load)

```bash
$ kubectl exec -it kafka-0 -- sh
$ kafka-console-consumer --bootstrap-server localhost:9092 --topic prefix.public.customers --from-beginning

{"before":null,"after":{"id":1,"first_name":"John","last_name":"Doe","email":"johndoe@gmail.com"},"source":{"version":"2.5.4.Final","connector":"postgresql","name":"prefix","ts_ms":1720271497008,"snapshot":"first","db":"postgres","sequence":"[null,\"22401960\"]","schema":"public","table":"customers","txId":750,"lsn":22401960,"xmin":null},"op":"r","ts_ms":1720271497074,"transaction":null}
{"before":null,"after":{"id":2,"first_name":"Jane","last_name":"Doe","email":"janedoe@gmail.com"},"source":{"version":"2.5.4.Final","connector":"postgresql","name":"prefix","ts_ms":1720271497008,"snapshot":"last","db":"postgres","sequence":"[null,\"22401960\"]","schema":"public","table":"customers","txId":750,"lsn":22401960,"xmin":null},"op":"r","ts_ms":1720271497075,"transaction":null}
```

### Capture a delete

```bash
$ delete from customers where id = 1;
```

Topic information:

```bash
{"before":{"id":1,"first_name":"","last_name":"","email":""},"after":null,"source":{"version":"2.5.4.Final","connector":"postgresql","name":"prefix","ts_ms":1720271727393,"snapshot":"false","db":"postgres","sequence":"[null,\"22402288\"]","schema":"public","table":"customers","txId":751,"lsn":22402288,"xmin":null},"op":"d","ts_ms":1720271727769,"transaction":null}
```

### Capture an update 

```bash
$ update customers set first_name = 'Mary' where id = 2; 
```

```bash
{"before":null,"after":{"id":2,"first_name":"Mary","last_name":"Doe","email":"janedoe@gmail.com"},"source":{"version":"2.5.4.Final","connector":"postgresql","name":"prefix","ts_ms":1720271832655,"snapshot":"false","db":"postgres","sequence":"[\"22402552\",\"22402608\"]","schema":"public","table":"customers","txId":752,"lsn":22402608,"xmin":null},"op":"u","ts_ms":1720271833082,"transaction":null}
```

### Capture an insert:

```bash
INSERT INTO customers(first_name, last_name, email) VALUES ('XXX', 'Doe', 'johndoe@gmail.com');
```

In topic:

```bash
{"before":null,"after":{"id":3,"first_name":"XXX","last_name":"Doe","email":"johndoe@gmail.com"},"source":{"version":"2.5.4.Final","connector":"postgresql","name":"prefix","ts_ms":1720271920769,"snapshot":"false","db":"postgres","sequence":"[\"22402760\",\"22403152\"]","schema":"public","table":"customers","txId":753,"lsn":22403152,"xmin":null},"op":"c","ts_ms":1720271920962,"transaction":null}
```


### Check failover of the tasks

After running, scale down the connect cluster (updating the replicas to 1), you should see in the logs that tasks moves to the other worker

```bash
[INFO] 2024-07-07 19:52:24,396 [SourceTaskOffsetCommitter-1] org.apache.kafka.connect.runtime.WorkerSourceTask commitOffsets - WorkerSourceTask{id=debezium-source-connector-0} Committing offsets for 40 acknowledged messages
```

```bash
$ http :8083/connectors/debezium-source-connector/tasks/0/status
{
    "id": 0,
    "state": "RUNNING",
    "worker_id": "connect-1.connect.confluent.svc.cluster.local:8083"
}
```

and later moves to the `connect-0`

```bash
$ http :8083/connectors/debezium-source-connector/tasks/0/status
{
    "id": 0,
    "state": "RUNNING",
    "worker_id": "connect-0.connect.confluent.svc.cluster.local:8083"
}
```

### Check the Connect topics:

```bash
$ kubectl exec -it kafka-0 -- sh
$ kafka-console-consumer --bootstrap-server kafka:9092 --topic confluent.connect-offsets --from-beginning
$ kafka-console-consumer --bootstrap-server kafka:9092 --topic confluent.connect-status --from-beginning
$ kafka-console-consumer --bootstrap-server kafka:9092 --topic confluent.connect-config --from-beginning
```

### Scale with multiple connector instances

```bash
$ kubectl apply -f debezium-source-connector-filter1.yaml
$ kubectl apply -f debezium-source-connector-filter1.yaml
```

```bash
$ kubeclt get connectors -o wide 

NAME                                STATUS    CONNECTORSTATUS   TASKS-READY   AGE     CONNECTENDPOINT                                   TASKS-FAILED   WORKERID                                             RESTARTPOLICY   KAFKACLUSTERID
debezium-source-connector-filter1   CREATED   RUNNING           1/1           3m54s   http://connect.confluent.svc.cluster.local:8083                  connect-1.connect.confluent.svc.cluster.local:8083   OnFailure       3b240be5-cb23-4d82-85Q
debezium-source-connector-filter2   CREATED   RUNNING           1/1           3m43s   http://connect.confluent.svc.cluster.local:8083                  connect-0.connect.confluent.svc.cluster.local:8083   OnFailure       3b240be5-cb23-4d82-85Q
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
- 

Resources:

- Debezium Postgresql: https://debezium.io/documentation/reference/stable/connectors/postgresql.html
- Configure Kafka Connect for CFK https://docs.confluent.io/operator/current/co-configure-connect.html
- How to install connector plugins in Kafka Connect: https://rmoff.net/2020/06/19/how-to-install-connector-plugins-in-kafka-connect/