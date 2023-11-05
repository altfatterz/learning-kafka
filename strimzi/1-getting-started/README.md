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

<TODO>
```

```bash
$ kubectl api-resources | grep kafka

<TODO>
```

### 4. Before installing your first Kafka cluster, watch the pods and services in the `kafka` namespace in two terminals:

Terminal 1:

```bash
$ watch kubectl get pods -o wide -n kafka
```

The `strimzi` cluster operator pod should have already started up, if not wait until the status is `RUNNING`

```bash
<TODO>
```

Terminal 2:

```bash
$ watch kubectl get svc -n kafka
```

### 5. Deploy a simple Kafka cluster (1 broker/1 zookeeper)

```bash
$ kubectl apply -n kafka -f kafka-ephemeral-single.yaml 
```

You should see that first a `zookeeper` and next a `kafka` pod being started up and later the `strimzi` entity operator.

```bash
$ kubectl get pods -n kafka -o wide

NAME                                          READY   STATUS    RESTARTS   AGE     IP          NODE                    NOMINATED NODE   READINESS GATES
<TODO>
```

By default, is using image `quay.io/strimzi/kafka:0.31.1-kafka-3.2.3` which is a redhat linux (`cat/etc/os-release`
command)

Also view the services which were created:

```bash
$ kubectl get svc -n kafka -o wide
<TODO>
```

Verify that you can get information about kafka as a resource

```bash
$ kubectl get kafkas -n kafka
$ kubectl describe kafka my-cluster -n kafka
```

### 6. Create a topic using a strimzi resource definition:

After the services are up and running, create a topic:

```bash
$ kubectl apply -n kafka -f kafka-topic.yaml
```

Verify that the resource details:

```bash
$ kubectl get kt -n kafka
$ kubectl describe kt my-topic -n kafka
```

### 7. Test the cluster console consumer and console producer within the cluster:

In one terminal create a producer: (this will start up the `kafka-producer` pod)

```bash
$ kubectl -n kafka run kafka-producer -ti --image=quay.io/strimzi/kafka:0.38.0-kafka-3.6.0 --rm=true --restart=Never -- bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic
```

In one another terminal create a consumer: (this will start up a `kafka-consumer` pod)

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.38.0-kafka-3.6.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```

The ephemeral volume is used by the Kafka brokers as log directories mounted into the following path:

```bash
/var/lib/kafka/data/kafka-logIDX
````

```bash
$ kubectl exec -it my-cluster-kafka-0 -n kafka -- ls /var/lib/kafka/data/kafka-log0
```

Kafka configuration

```bash
$ kubectl exec -it my-cluster-kafka-0 -n kafka -- ls /opt/kafka
```

If you delete the `my-cluster-kafka-0` the `my-topic` data is gone. Try to delete the `my-cluster-kafka-0` pod and read
again
from the `my-topic` topic.

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
