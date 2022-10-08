### Prerequisites

1. Set up a Docker environment, in this workshop we will use Minikube [https://minikube.sigs.k8s.io/docs/start/](https://minikube.sigs.k8s.io/docs/start/)
2. Install [`k3d`](https://k3d.io/)
3. Install [`kubectl`](https://kubernetes.io/docs/tasks/tools/)

### 1. Start Minikube

```bash
$ minikube version
minikube version: v1.27.0
# don't start the embedded kubernetes cluster using `--no-kubernetes` option, we use k3d for that
# add more memory and cpu using the `--memory` and `--cpu` options
# with `--driver` option you can set the prefered driver on macOS is `hyperkit` on windows is `hyperv` 
$ minikube start --driver=hyperkit --container-runtime=docker --memory 8192 --cpus 4 --no-kubernetes
```

### 2. Point your terminal’s docker-cli to the Docker Engine inside minikube. 
```bash
$ eval $(minikube docker-env)
```

Other Useful commands:
```bash
# if error occurs when starting the minikube use command to clean the minikube
$ minikube delete --all --purge
# ssh into the minikube VM
$ minikube ssh
# get the status of minikube VM
$ minikube status
# get the ip of the minikube VM
$ minikube ip
```

### 3. Create a k8s cluster using k3d

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

### 4. Install the [Strimzi](https://strimzi.io/) operator

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 5. Analyse what CRDs and new resource types where created

```bash
$ kubectl get crds | grep strimzi

kafkarebalances.kafka.strimzi.io        2022-09-16T15:14:05Z
kafkaconnectors.kafka.strimzi.io        2022-09-16T15:14:05Z
strimzipodsets.core.strimzi.io          2022-09-16T15:14:05Z
kafkatopics.kafka.strimzi.io            2022-09-16T15:14:05Z
kafkausers.kafka.strimzi.io             2022-09-16T15:14:05Z
kafkabridges.kafka.strimzi.io           2022-09-16T15:14:05Z
kafkaconnects.kafka.strimzi.io          2022-09-16T15:14:05Z
kafkamirrormaker2s.kafka.strimzi.io     2022-09-16T15:14:05Z
kafkas.kafka.strimzi.io                 2022-09-16T15:14:05Z
kafkamirrormakers.kafka.strimzi.io      2022-09-16T15:14:05Z
```

```bash
$ kubectl api-resources | grep kafka

kafkabridges                      kb           kafka.strimzi.io/v1beta2               true         KafkaBridge
kafkaconnectors                   kctr         kafka.strimzi.io/v1beta2               true         KafkaConnector
kafkaconnects                     kc           kafka.strimzi.io/v1beta2               true         KafkaConnect
kafkamirrormaker2s                kmm2         kafka.strimzi.io/v1beta2               true         KafkaMirrorMaker2
kafkamirrormakers                 kmm          kafka.strimzi.io/v1beta2               true         KafkaMirrorMaker
kafkarebalances                   kr           kafka.strimzi.io/v1beta2               true         KafkaRebalance
kafkas                            k            kafka.strimzi.io/v1beta2               true         Kafka
kafkatopics                       kt           kafka.strimzi.io/v1beta2               true         KafkaTopic
kafkausers                        ku           kafka.strimzi.io/v1beta2               true         KafkaUser
```

### 6. Before installing your first Kafka cluster, watch the pods and services in the `kafka` namespace in two terminals:

Terminal 1:
```bash
$ watch kubectl get pods -o wide -n kafka
```

The `strimzi` cluster operator pod should have already started up, if not wait until the status is `RUNNING`

```bash
NAME                                        READY   STATUS    RESTARTS   AGE    IP          NODE                    NOMINATED NODE   READINESS GATES
strimzi-cluster-operator-54cb64cfdd-nhtn8   1/1     Running   0          118s   10.42.0.6   k3d-mycluster-agent-0   <none>           <none>
```

Terminal 2:
```bash
$ watch kubectl get svc -n kafka
```

### 7. Deploy a simple Kafka cluster (1 broker/1 zookeeper)

```bash
$ kubectl apply -n kafka -f kafka-ephemeral-single.yaml 
```

You should see that first a `zookeeper` and next a `kafka` pod being started up and later the `strimzi` entity operator.

```bash
$ kubectl get pods -n kafka -o wide

NAME                                          READY   STATUS    RESTARTS   AGE     IP          NODE                    NOMINATED NODE   READINESS GATES
strimzi-cluster-operator-854758757-4n4vw      1/1     Running   0          8m23s   10.42.0.6   k3d-mycluster-agent-0   <none>           <none>
my-cluster-zookeeper-0                        1/1     Running   0          78s     10.42.0.7   k3d-mycluster-agent-0   <none>           <none>
my-cluster-kafka-0                            1/1     Running   0          54s     10.42.0.8   k3d-mycluster-agent-0   <none>           <none>
my-cluster-entity-operator-54f8746cb8-85nbt   1/3     Running   0          11s     10.42.0.9   k3d-mycluster-agent-0   <none>           <none>
```

Also view the services which were created:

```bash
$ kubectl get svc -n kafka -o wide
my-cluster-zookeeper-client   ClusterIP   10.43.25.136    <none>        2181/TCP                              3m8s
my-cluster-zookeeper-nodes    ClusterIP   None            <none>        2181/TCP,2888/TCP,3888/TCP            3m8s
my-cluster-kafka-brokers      ClusterIP   None            <none>        9090/TCP,9091/TCP,9092/TCP,9093/TCP   2m32s
my-cluster-kafka-bootstrap    ClusterIP   10.43.180.195   <none>        9091/TCP,9092/TCP,9093/TCP            2m32s
```

Verify that you can get inforation about kafka as a resource

```bash
$ kubectl get kafkas -n kafka
$ kubectl describe kafka my-cluster -n kafka
```

### 8. Create a topic using a strimzi resource definition:

After the services are up and running, create a topic:

```bash
$ kubectl apply -n kafka -f kafka-topic.yaml
```

Verify that the resource details:

```bash
$ kubectl get kt -n kafka
$ kubectl describe kt my-topic -n kafka
```

### 9. Test the cluster console consumer and console producer within the cluster:

In one terminal create a producer: (this will start up the `kafka-producer` pod)

```bash
$ kubectl -n kafka run kafka-producer -ti --image=quay.io/strimzi/kafka:0.31.1-kafka-3.2.3 --rm=true --restart=Never -- bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic
```

In one another terminal create a consumer: (this will start up a `kafka-consumer` pod)

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.31.1-kafka-3.2.3 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
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

If you delete the `my-cluster-kafka-0` the `my-topic` data is gone. Try to delete the `my-cluster-kafka-0` pod and read again 
from the `my-topic` topic.

Next we will see how we can save the data in case a kafka pod restarts or stops. 

### Cleanup

```bash
$ k3d cluster delete mycluster
```