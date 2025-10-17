### Prerequisites

1. Set up a Docker environment with [Docker Desktop](https://www.docker.com/products/docker-desktop/)
2. Install [`k3d`](https://k3d.io/)
3. Install [`kubectl`](https://kubernetes.io/docs/tasks/tools/)

### 1. Create a k8s cluster using k3d

```bash
# Start a k8s cluster with 1 agent node, 1 server node (control-plane), we disable the loadbalancer in front of the server nodes
$ k3d cluster create mycluster --agents 1 --no-lb
# view our k8s cluster 
$ k3d cluster list
# kubectl is automatically will be set to the context
$ kubectl cluster-info
# verify that we have 1 agent nodes and 1 server node
$ kubectl get nodes -o wide
# check with docker that the nodes are running in a docker container
$ docker ps
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
# create the `kafka` namespace
$ kubectl create ns kafka
```

### 2. Install the [Strimzi](https://strimzi.io/) operator

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 3. Analyse what CRDs and new resource types where created

```bash
$ kubectl get crds | grep strimzi

kafkabridges.kafka.strimzi.io           2025-10-17T13:40:16Z
kafkaconnectors.kafka.strimzi.io        2025-10-17T13:40:16Z
kafkaconnects.kafka.strimzi.io          2025-10-17T13:40:16Z
kafkamirrormaker2s.kafka.strimzi.io     2025-10-17T13:40:16Z
kafkanodepools.kafka.strimzi.io         2025-10-17T13:40:16Z
kafkarebalances.kafka.strimzi.io        2025-10-17T13:40:16Z
kafkas.kafka.strimzi.io                 2025-10-17T13:40:16Z
kafkatopics.kafka.strimzi.io            2025-10-17T13:40:16Z
kafkausers.kafka.strimzi.io             2025-10-17T13:40:16Z
strimzipodsets.core.strimzi.io          2025-10-17T13:40:16Z
```

```bash
$ kubectl api-resources | grep kafka

kafkabridges                        kb           kafka.strimzi.io/v1beta2          true         KafkaBridge
kafkaconnectors                     kctr         kafka.strimzi.io/v1beta2          true         KafkaConnector
kafkaconnects                       kc           kafka.strimzi.io/v1beta2          true         KafkaConnect
kafkamirrormaker2s                  kmm2         kafka.strimzi.io/v1beta2          true         KafkaMirrorMaker2
kafkanodepools                      knp          kafka.strimzi.io/v1beta2          true         KafkaNodePool
kafkarebalances                     kr           kafka.strimzi.io/v1beta2          true         KafkaRebalance
kafkas                              k            kafka.strimzi.io/v1beta2          true         Kafka
kafkatopics                         kt           kafka.strimzi.io/v1beta2          true         KafkaTopic
kafkausers                          ku           kafka.strimzi.io/v1beta2          true         KafkaUser
```

### 4. Before installing your first Kafka cluster, watch the pods and services in the `kafka` namespace in two terminals:

Terminal 1:

```bash
$ watch kubectl get pods -o wide -n kafka
```

The `strimzi` cluster operator pod should have already started up, if not wait until the status is `RUNNING`

```bash
NAME                                        READY   STATUS    RESTARTS   AGE   IP          NODE                    NOMINATED NODE   READINESS GATES
strimzi-cluster-operator-7c88589497-qk5hr   1/1     Running   0          59s   10.42.1.4   k3d-mycluster-agent-0   <none>           <none>
```

Terminal 2:

```bash
$ watch kubectl get svc -n kafka
```

### 5. Deploy a simple Kafka cluster (1 broker/1 zookeeper)

```bash
$ kubectl apply -n kafka -f kafka-ephemeral-single.yaml 
```

```bash
$ kubectl get pods -n kafka -o wide

NAME                                          READY   STATUS    RESTARTS   AGE   IP          NODE                    NOMINATED NODE   READINESS GATES
my-cluster-dual-role-0                        1/1     Running   0          60s   10.42.1.5   k3d-mycluster-agent-0   <none>           <none>
my-cluster-entity-operator-5f7984464b-27d77   2/2     Running   0          28s   10.42.1.6   k3d-mycluster-agent-0   <none>           <none>
strimzi-cluster-operator-7c88589497-qk5hr     1/1     Running   0          11m   10.42.1.4   k3d-mycluster-agent-0   <none>           <none>
```

By default, is using image `quay.io/strimzi/kafka:0.48.0-kafka-4.1.0`

Also view the services which were created:

```bash
$ kubectl get svc -n kafka -o wide
NAME                         TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                                        AGE     SELECTOR
my-cluster-kafka-bootstrap   ClusterIP   10.43.204.232   <none>        9091/TCP,9092/TCP,9093/TCP                     3m30s   strimzi.io/broker-role=true,strimzi.io/cluster=my-cluster,strimzi.io/kind=Kafka,strimzi.io/name=my-cluster-kafka
my-cluster-kafka-brokers     ClusterIP   None            <none>        9090/TCP,9091/TCP,8443/TCP,9092/TCP,9093/TCP   3m30s   strimzi.io/cluster=my-cluster,strimzi.io/kind=Kafka,strimzi.io/name=my-cluster-kafka
```

Verify that you can get information about kafka as a resource

```bash
$ kubectl get kafkas -n kafka
NAME         READY   METADATA STATE   WARNINGS
my-cluster   True    KRaft            True


$ kubectl describe kafka my-cluster -n kafka

Name:         my-cluster
Namespace:    kafka
Labels:       <none>
Annotations:  <none>
API Version:  kafka.strimzi.io/v1beta2
Kind:         Kafka
Metadata:
  Creation Timestamp:  2025-10-17T13:50:23Z
  Generation:          1
  Resource Version:    1480
  UID:                 0ac9d25d-856b-454e-b0d9-f6dfc16c2e2d
Spec:
  Entity Operator:
    Topic Operator:
    User Operator:
  Kafka:
    Config:
      default.replication.factor:                1
      min.insync.replicas:                       1
      offsets.topic.replication.factor:          1
      transaction.state.log.min.isr:             1
      transaction.state.log.replication.factor:  1
    Listeners:
      Name:            plain
      Port:            9092
      Tls:             false
      Type:            internal
      Name:            tls
      Port:            9093
      Tls:             true
      Type:            internal
    Metadata Version:  4.1-IV1
    Version:           4.1.0
Status:
  Cluster Id:  W_1AFkD3SPWRZ-BTjYZUJw
  Conditions:
    Last Transition Time:  2025-10-17T13:50:23.770002298Z
    Message:               A Kafka cluster with a single broker node and ephemeral storage will lose topic messages after any restart or rolling update.
    Reason:                KafkaStorage
    Status:                True
    Type:                  Warning
    Last Transition Time:  2025-10-17T13:50:23.770052339Z
    Message:               A Kafka cluster with a single controller node and ephemeral storage will lose data after any restart or rolling update.
    Reason:                KafkaStorage
    Status:                True
    Type:                  Warning
    Last Transition Time:  2025-10-17T13:51:18.019631628Z
    Status:                True
    Type:                  Ready
  Kafka Metadata State:    KRaft
  Kafka Metadata Version:  4.1-IV1
  Kafka Node Pools:
    Name:         dual-role
  Kafka Version:  4.1.0
  Listeners:
    Addresses:
      Host:             my-cluster-kafka-bootstrap.kafka.svc
      Port:             9092
    Bootstrap Servers:  my-cluster-kafka-bootstrap.kafka.svc:9092
    Name:               plain
    Addresses:
      Host:             my-cluster-kafka-bootstrap.kafka.svc
      Port:             9093
    Bootstrap Servers:  my-cluster-kafka-bootstrap.kafka.svc:9093
    Certificates:
      -----BEGIN CERTIFICATE-----
MIIFLTCCAxWgAwIBAgIUXLCo+Vc0XfYIWFA1o9eV8ATON2swDQYJKoZIhvcNAQEN
BQAwLTETMBEGA1UECgwKaW8uc3RyaW16aTEWMBQGA1UEAwwNY2x1c3Rlci1jYSB2
MDAeFw0yNTEwMTcxMzUwMjNaFw0yNjEwMTcxMzUwMjNaMC0xEzARBgNVBAoMCmlv
LnN0cmltemkxFjAUBgNVBAMMDWNsdXN0ZXItY2EgdjAwggIiMA0GCSqGSIb3DQEB
AQUAA4ICDwAwggIKAoICAQCd1iQq/MVOLLiSNa1rhK0xOvqHWT65MP+a7PYSLm6W
KzxhA2h/wj6rdvC7JJD5TckxqsrW7ZiSGAzxpVpEXNGZvorfl6kCDUEEA5mMTHtg
RtxutADApBU/wel9C9W3Pg2vBtgoTCbHWX5FlHcmm+it8ND43FaGx3fJcAFcxy02
HC+DQEAn7CDzSIBwQRzZDRShd1mf0bU+2CP1SC4j8NTToZK+c++46Tq0pBp7agYf
lW26g9cCLb5eIhofrFF+k3opTYyV+9IzLdP5h972uohG5R+cHSSDA46XfkXKMIM6
U3RaMFSdhYB8TyFWvIiGcrhL5vMhxvSqhl4CzuHv9HEKm2OvpCiGRNohH1BRq9Fs
b2q2u09op7Zi1PEeBToL9SsS83jpVBkt0koQplpd41I/GT4f80WJ7xS7xoDXVQIs
IF3X1n1x2umZvTOHPnu2OTfX/gSanSQiSvv+9CKveA3kr8sK4ymeVr5XJeAVH4kO
ltV3F/VwRNhFahbPIE6MWeYD8Puy/GyQRsnPE9PnT5MieDDGwoHWdvZ2DWETNu5z
mbAAzKbFsxNKHBTDWxGdaUWrfVhxcI6m/MgNkFB7QxWvN9L3JqTkDUgLjA2WZSeV
6z4HMC1uzkkS1c5JXZFf7ZTPC/oShx5jzvhNf0ZYyAsrtxE5Yev4Mf6mRiIzL7qT
xwIDAQABo0UwQzAdBgNVHQ4EFgQUNqp9dj+rlbTJs3uyjf2Y01XOpnIwEgYDVR0T
AQH/BAgwBgEB/wIBADAOBgNVHQ8BAf8EBAMCAQYwDQYJKoZIhvcNAQENBQADggIB
AJGFFgy5NUdNoaiGWfHguVIVklEWCYvChQcHQLy6RtsbmRydOh4f8zP5GAxI7VTl
DDPappziVLAW4Z4O3EZ3Mnh0uEdzDbT2Rl4lO2swmH5mIsDU4c720OWgIN6jHJBK
a3X9UyNdu2z7oGKn/bP6Ynz9ko6lFTF6EOwzF9AbZtwg1YmRXl4ucqkB51ua9cFZ
VjqxFycgxiJGqHrgdomlA8Vh5669FkOmCh/AQz9AJY+K8oCqBQW9rUkCWEr/CTfo
xHrWCSWWSn16ZlQf14oUFChJXUS052gIf+xV/qSNL2ms9xub6mpxNJ5N4nc3iN8q
BdWVxI0NuTGdqUzDYXp4LUH0yk82gaJwZ5+YJa3OVqGEn4DaGFtxQMfVvplnXkd8
MSL7gpH1LJDX6DIDrSszdTMf/295Ear1aULuY3Vq3NGpQdGYlBqgjlRAOSipPmBz
7VDq0gWWLxp8tkSyc+oM9cMPd6r8Y/zO3f0GdHJankIchCvPghVkc9FcKOMsOpQI
b9Rcs9GMkUJFJ6OWA6gOIHpigyGLsAHeI0ilzGn6Jr2Tn83IjdrDRuYLVh6I9JLF
C6kvYVkNqa123t+J+fDdbJ/r9X8oKyWYERSRGMQWEPcGwG7kpWfH7/BGv56yInc9
xNilbNMqCZ4B23Tdgs6z5BK22Wl9y+GB/68ZuHD2UvIU
-----END CERTIFICATE-----

    Name:                            tls
  Observed Generation:               1
  Operator Last Successful Version:  0.48.0
Events:                              <none>
```

### 6. Create a topic using a strimzi resource definition:

After the services are up and running, create a topic:

```bash
$ kubectl apply -n kafka -f kafka-topic.yaml
```

Verify that the resource details:

```bash
$ kubectl get kt -n kafka

NAME       CLUSTER      PARTITIONS   REPLICATION FACTOR   READY
my-topic   my-cluster   1            1                    True

$ kubectl describe kt my-topic -n kafka

Name:         my-topic
Namespace:    kafka
Labels:       strimzi.io/cluster=my-cluster
Annotations:  <none>
API Version:  kafka.strimzi.io/v1beta2
Kind:         KafkaTopic
Metadata:
  Creation Timestamp:  2025-10-17T13:54:59Z
  Finalizers:
    strimzi.io/topic-operator
  Generation:        1
  Resource Version:  1680
  UID:               0cc9bc3f-652e-4770-85ee-5ec143bdbf4c
Spec:
  Config:
    retention.ms:   7200000
    segment.bytes:  1073741824
  Partitions:       1
  Replicas:         1
Status:
  Conditions:
    Last Transition Time:  2025-10-17T13:54:59.249740925Z
    Status:                True
    Type:                  Ready
  Observed Generation:     1
  Topic Id:                2rVugoh5SNCG3tvvYI_xWw
  Topic Name:              my-topic
```

### 7. Test the cluster console consumer and console producer within the cluster:

In one terminal create a producer: (this will start up the `kafka-producer` pod)

```bash
$ kubectl -n kafka run kafka-producer -ti --image=quay.io/strimzi/kafka:0.48.0-kafka-4.1.0 --rm --restart=Never -- bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic
```

In one another terminal create a consumer: (this will start up a `kafka-consumer` pod)

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.48.0-kafka-4.1.0 --rm --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```

The ephemeral volume is used by the Kafka brokers as log directories mounted into the following path:

```bash
/var/lib/kafka
````

```bash
$ kubectl exec -it my-cluster-dual-role-0 -n kafka -- ls -l /var/lib/kafka
```

Kafka configuration

```bash
$ kubectl exec -it my-cluster-dual-role-0 -n kafka -- ls /opt/kafka
```

Delete the `my-cluster-dual-role-0` pod and you will see the `my-topic` data is gone. 

Try to read it again. Is empty:

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.48.0-kafka-4.1.0 --rm --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```

Metadata is still there:

```bash
NAME       CLUSTER      PARTITIONS   REPLICATION FACTOR   READY
my-topic   my-cluster   1            1                    True
```

Next we will see how we can save the data in case a kafka pod restarts or stops.

### 8. Cleanup

- Deleting your Apache Kafka cluster

```bash
$ kubectl -n kafka delete $(kubectl get strimzi -o name -n kafka)
```

This will remove all Strimzi custom resources, including the Apache Kafka cluster and any KafkaTopic custom resources
but leave the Strimzi cluster operator running so that it can respond to new Kafka custom resources.

- Deleting the Strimzi cluster operator

```bash
$ kubectl -n kafka delete -f 'https://strimzi.io/install/latest?namespace=kafka'
```

- Delete the Kubernetes cluster

```bash
$ k3d cluster delete mycluster
```
