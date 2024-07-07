### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent
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
  PRIMARY KEY(id)
);
  
INSERT INTO customers(first_name, last_name, email) VALUES ('John', 'Doe', 'johndoe@gmail.com');
COMMIT;

INSERT INTO customers(first_name, last_name, email) VALUES ('Jane', 'Doe', 'janedoe@gmail.com');
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
$ kubectl apply -f confluent-platform.yaml
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



Resources:

- Configure Kafka Connect for CFK https://docs.confluent.io/operator/current/co-configure-connect.html
- How to install connector plugins in Kafka Connect: https://rmoff.net/2020/06/19/how-to-install-connector-plugins-in-kafka-connect/