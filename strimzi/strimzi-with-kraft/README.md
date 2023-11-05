
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

### 1. Install the [Strimzi](https://strimzi.io/) operator

```bash

$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 2. Analyse what CRDs and new resource types where created

```bash
$ kubectl get crds | grep strimzi

kafkaconnects.kafka.strimzi.io        2023-11-04T08:39:05Z
kafkatopics.kafka.strimzi.io          2023-11-04T08:39:05Z
kafkamirrormaker2s.kafka.strimzi.io   2023-11-04T08:39:05Z
kafkabridges.kafka.strimzi.io         2023-11-04T08:39:05Z
kafkausers.kafka.strimzi.io           2023-11-04T08:39:05Z
kafkaconnectors.kafka.strimzi.io      2023-11-04T08:39:05Z
kafkas.kafka.strimzi.io               2023-11-04T08:39:05Z
kafkarebalances.kafka.strimzi.io      2023-11-04T08:39:05Z
kafkamirrormakers.kafka.strimzi.io    2023-11-04T08:39:05Z
kafkanodepools.kafka.strimzi.io       2023-11-04T08:39:06Z
strimzipodsets.core.strimzi.io        2023-11-04T08:39:06Z
```

```bash
$ kubectl api-resources | grep kafka

kafkabridges                      kb           kafka.strimzi.io/v1beta2               true         KafkaBridge
kafkaconnectors                   kctr         kafka.strimzi.io/v1beta2               true         KafkaConnector
kafkaconnects                     kc           kafka.strimzi.io/v1beta2               true         KafkaConnect
kafkamirrormaker2s                kmm2         kafka.strimzi.io/v1beta2               true         KafkaMirrorMaker2
kafkamirrormakers                 kmm          kafka.strimzi.io/v1beta2               true         KafkaMirrorMaker
kafkanodepools                    knp          kafka.strimzi.io/v1beta2               true         KafkaNodePool
kafkarebalances                   kr           kafka.strimzi.io/v1beta2               true         KafkaRebalance
kafkas                            k            kafka.strimzi.io/v1beta2               true         Kafka
kafkatopics                       kt           kafka.strimzi.io/v1beta2               true         KafkaTopic
kafkausers                        ku           kafka.strimzi.io/v1beta2               true         KafkaUser
```

### 3. Before installing your first Kafka cluster, watch the pods and services in the `kafka` namespace in two terminals:

Terminal 1:
```bash
$ watch kubectl get pods -o wide -n kafka
```

```bash
NAME                                       READY   STATUS    RESTARTS   AGE     IP          NODE                    NOMINATED NODE   READINESS GATES
strimzi-cluster-operator-95d88f6b5-jddvw   1/1     Running   0          3m13s   10.42.0.4   k3d-mycluster-agent-0   <none>           <none>
```

Terminal 2:
```bash
$ watch kubectl get svc -n kafka
```

### 4. Deploy a simple Kafka cluster without Zookeeper

```bash
$ kubectl apply -n kafka -f kafka-with-kraft-ephemeral.yaml 
```


### 5. Get information as CRD resources

```bash
$ kubectl get k -n kafka
$ kubectl describe kafka my-cluster -n kafka
$ kubectl get knp -n kafka
$ kubectl describe knp controller -n kafka
$ kubectl describe knp broker -n kafka
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

### 8. StrimziPodSets are GA and enabled by default

`StrimziPodSets` - What it is and why should you care? --> https://www.youtube.com/watch?v=iSwrn1Gumx4

```bash
$ kubectl get sps -n kafka
$ kubectl describe sps my-cluster-kafka -n kafka
```

### 9. Cleanup

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

### Resources

- [UseKRaft feature gate](https://strimzi.io/docs/operators/latest/deploying#ref-operator-use-kraft-feature-gate-str)
- [KafkaNodePools feature gate](https://strimzi.io/docs/operators/latest/deploying#ref-operator-kafka-node-pools-feature-gate-str)
- [UseStrimziPodSets feature gate is GA](https://strimzi.io/docs/operators/latest/deploying#ref-operator-use-strimzi-pod-sets-feature-gate-str) 