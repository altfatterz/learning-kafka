### Create a 3 node Kubernetes cluster:

```bash
$ k3d cluster create my-k8s-cluster --agents 3
# view our k8s cluster 
$ k3d cluster list
# kubectl is automatically will be set to the context
$ kubectl cluster-info
# verify that we have 1 agent nodes and 1 server node
$ kubectl get nodes -o wide
# check with docker that the nodes are running in a docker container
$ docker ps
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-my-k8s-cluster-server-0 key1=value1:NoSchedule
```

After creating the cluster automatically we should switch to the created `k3d-my-k8s-cluster` context.

```bash
$ kubectl config get-contexts
CURRENT   NAME                 CLUSTER              AUTHINFO                   NAMESPACE
*         k3d-my-k8s-cluster   k3d-my-k8s-cluster   admin@k3d-my-k8s-cluster
```

Let's create a `kafka` namespaces for our playground and set permanently save the namespace for all subsequent `kubectl` 
commands in that context.

```bash
$ kubectl create ns kafka
$ kubectl config set-context --current --namespace=kafka 
```

If we retrieve the current context again the kafka namespace shoud be set

```bash
$ kubectl config get-contexts
CURRENT   NAME                 CLUSTER              AUTHINFO                   NAMESPACE
*         k3d-my-k8s-cluster   k3d-my-k8s-cluster   admin@k3d-my-k8s-cluster   kafka
```

### Disable `UseStrimziPodSets` feature gate 

Current version of Strimzi does not use `StatefulSets` anymore when you deploy a Kafka cluster. They use a custom build 
`StrimziPodSets`.

To disable the `UseStrimziPodSets` feature gate, specify `-UseStrimziPodSets` in the `STRIMZI_FEATURE_GATES`
environment variable in the Cluster Operator configuration.

### Install Strimzi

In this `strimzi.yaml` file the `STRIMZI_FEATURE_GATES` was configured to `-UseStrimziPodSets`

```bash
$ kubectl create -f strimzi.yaml
```

### Analyse what CRDs and new resource types where created

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

We should see that the `strimzi-cluster-operator` deployment was created with a single pod and also a 
`strimzi-cluster-operator` ConfigMap

```bash
$ kubectl describe deploy strimzi-cluster-operator
```
Important properties:

`STRIMZI_NAMESPACE`

- A comma-separated list of namespaces that the operator operates in. When not set, set to empty string, or set to *, the Cluster Operator operates in all namespaces.
- The Cluster Operator deployment might use the downward API to set this automatically to the namespace the Cluster Operator is deployed in.

```bash
env:
  - name: STRIMZI_NAMESPACE
    valueFrom:
      fieldRef:
        fieldPath: metadata.namespace
```

`STRIMZI_FEATURE_GATES` what we already discussed

different container images:
`STRIMZI_KAFKA_IMAGES`, `STRIMZI_KAFKA_CONNECT_IMAGES`, `STRIMZI_DEFAULT_TOPIC_OPERATOR_IMAGE`, etc..

The `strimzi-cluster-operator` contains log4j logging configurations:

```bash
$ kubectl describe cm strimzi-cluster-operator
```

### Deploy a simple Kafka cluster (3 broker/3 zookeeper) with persistence configured

Before we deploy the Kafka cluster create 6 terminals and issue the following commands in each:

```bash
$ watch kubectl get sts 
$ watch kubectl get secret
$ watch kubectl get cm
$ watch kubectl get pods -o wide
$ watch kubectl get pvc
$ watch kubectl get pv
```

Create a `StorageClass` where we configure the `Retain` ClaimPolicy to avoid makes sure the dynamically provisioned PVs 
are not deleted when the PVCs are deleted. 

```bash
$ kubectl apply -f local-path-retain-sc.yaml 
```

Deploy a Kafka 3 node cluster (3 node Zookeeper) with `JBOD` persistence configured.

```bash
$ kubectl apply -f kafka-jbod-persistent-claim.yaml
```

When writing / reading to disks there are two main factors which can limit performance
- the bandwith of the broker node on which the Kafka broker runs
- performance of the disk itself

If the bandwith of the broker node is much bigger than the performance of the disk by adding more disks you can increase 
the overall writing / reading performance by better utilizing the capacity of the broker node.
You also have to make sure the partitions are distributed across the disks to better utilize them.

If the bandwith of a broker node is already saturated by a single disk by adding more disks you don't increase the overall performance.
If will still use the same capacity of the borker node just share it between multiple disks.

In both cases though you can increase the capacity of the broker. Every infrastructure has a limit how a single disk volume can be.
Adding more disks you can increase the capacity of the broker. 

Another note is that a single partition can only be on a single volume. 
So none of the partitions can be bigger than the size of single disk. 


- You can configure Strimzi to use JBOD, a data storage configuration of multiple disks or volumes. 
- JBOD is one approach to providing increased data storage for Kafka brokers. It can also improve performance.
- JBOD storage is supported for Kafka only not ZooKeeper.
- Storage size for persistent volumes can be
  - increased (but not decreased) 
  - additional volumes may be added to the JBOD storage

Data storage considerations: 
- Block storage is required (XFS, ext4), file storage like NFS does not work with Kafka.
- Use separate disks for Apache Kafka and ZooKeeper.
- Solid-state drives (SSDs), though not essential, can improve the performance of Kafka in large clusters where data is sent to and received from multiple topics asynchronously.
- SSDs are particularly effective with ZooKeeper, which requires fast, low latency data access.
- You do not need to provision replicated storage because Kafka and ZooKeeper both have built-in data replication.

Check what resources have been created:

1. StatefulSets:

```bash
$ kubectl get sts
NAME                   READY   AGE
my-cluster-zookeeper   3/3     5m6s
my-cluster-kafka       3/3     3m31s
```

2. Services
```bash
$ kubectl get services
my-cluster-zookeeper-client   ClusterIP   10.43.185.183   <none>        2181/TCP                              2m57s
my-cluster-zookeeper-nodes    ClusterIP   None            <none>        2181/TCP,2888/TCP,3888/TCP            2m57s
my-cluster-kafka-brokers      ClusterIP   None            <none>        9090/TCP,9091/TCP,9092/TCP,9093/TCP   2m28s
my-cluster-kafka-bootstrap    ClusterIP   10.43.81.95     <none>        9091/TCP,9092/TCP,9093/TCP            2m28s
```

4. Secrets:

```bash
$ kubectl get secrets
my-cluster-cluster-ca-cert               Opaque   3      5m36s
my-cluster-clients-ca-cert               Opaque   3      5m36s
my-cluster-cluster-ca                    Opaque   1      5m36s
my-cluster-clients-ca                    Opaque   1      5m36s
my-cluster-cluster-operator-certs        Opaque   4      5m35s
my-cluster-zookeeper-nodes               Opaque   12     5m33s
my-cluster-kafka-brokers                 Opaque   12     3m58s
my-cluster-entity-topic-operator-certs   Opaque   4      3m
my-cluster-entity-user-operator-certs    Opaque   4      3m
```

3. ConfigMaps:

```bash
NAME                                      DATA   AGE
kube-root-ca.crt                          1      37m
strimzi-cluster-operator                  1      24m
my-cluster-zookeeper-config               2      6m6s
my-cluster-kafka-config                   5      4m31s
my-cluster-entity-topic-operator-config   1      3m34s
my-cluster-entity-user-operator-config    1      3m34s
```

4. Pods:

```bash
$ kubectl get pods -o wide
strimzi-cluster-operator-5647fbfc85-lp9gp     1/1     Running   0          24m     10.42.1.5   k3d-my-k8s-cluster-agent-2   <none>           <none>
my-cluster-zookeeper-0                        1/1     Running   0          6m56s   10.42.3.5   k3d-my-k8s-cluster-agent-0   <none>           <none>
my-cluster-zookeeper-2                        1/1     Running   0          6m55s   10.42.1.7   k3d-my-k8s-cluster-agent-2   <none>           <none>
my-cluster-zookeeper-1                        1/1     Running   0          6m55s   10.42.2.6   k3d-my-k8s-cluster-agent-1   <none>           <none>
my-cluster-kafka-0                            1/1     Running   0          5m21s   10.42.3.7   k3d-my-k8s-cluster-agent-0   <none>           <none>
my-cluster-kafka-1                            1/1     Running   0          5m21s   10.42.2.8   k3d-my-k8s-cluster-agent-1   <none>           <none>
my-cluster-kafka-2                            1/1     Running   0          5m21s   10.42.1.9   k3d-my-k8s-cluster-agent-2   <none>           <none>
my-cluster-entity-operator-6df799fffd-xjzmj   3/3     Running   0          4m23s   10.42.3.8   k3d-my-k8s-cluster-agent-0   <none>           <none>
```

5. PVCs:

```bash
$ kubectl get pvc 
NAME                          STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
data-my-cluster-zookeeper-2   Bound    pvc-614ba371-b837-42e3-b60a-778bc3c573de   1Gi        RWO            local-path-retain-sc   44m
data-my-cluster-zookeeper-0   Bound    pvc-a64b8fb1-1f31-4f5f-a611-98105177c8c1   1Gi        RWO            local-path-retain-sc   44m
data-my-cluster-zookeeper-1   Bound    pvc-c810cc19-58bf-4553-979c-0d8e8a5d8a87   1Gi        RWO            local-path-retain-sc   44m
data-0-my-cluster-kafka-0     Bound    pvc-f1727646-86eb-44f0-abad-c2d16bf3b5b1   1Gi        RWO            local-path-retain-sc   42m
data-0-my-cluster-kafka-1     Bound    pvc-46191e05-b719-4738-8d01-bc373228edc3   1Gi        RWO            local-path-retain-sc   42m
data-1-my-cluster-kafka-0     Bound    pvc-c9f91cea-ca3e-4d69-b4f2-4e29c563a3fc   1Gi        RWO            local-path-retain-sc   42m
data-0-my-cluster-kafka-2     Bound    pvc-b72086bb-df47-43bc-aa3c-01ec35fd6b29   1Gi        RWO            local-path-retain-sc   42m
data-1-my-cluster-kafka-1     Bound    pvc-d13fac0c-d209-4b3c-a0e7-7c6b848fb1b9   1Gi        RWO            local-path-retain-sc   42m
data-1-my-cluster-kafka-2     Bound    pvc-a78e1e3f-410e-440f-a5c8-e58ad2f31c72   1Gi        RWO            local-path-retain-sc   42m
```

6. PVs:
 
```bash
$ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                               STORAGECLASS           REASON   AGE
pvc-614ba371-b837-42e3-b60a-778bc3c573de   1Gi        RWO            Retain           Bound    kafka/data-my-cluster-zookeeper-2   local-path-retain-sc            5m18s
pvc-c810cc19-58bf-4553-979c-0d8e8a5d8a87   1Gi        RWO            Retain           Bound    kafka/data-my-cluster-zookeeper-1   local-path-retain-sc            5m17s
pvc-a64b8fb1-1f31-4f5f-a611-98105177c8c1   1Gi        RWO            Retain           Bound    kafka/data-my-cluster-zookeeper-0   local-path-retain-sc            5m17s
pvc-46191e05-b719-4738-8d01-bc373228edc3   1Gi        RWO            Delete           Bound    kafka/data-0-my-cluster-kafka-1     local-path-retain-sc            3m9s
pvc-f1727646-86eb-44f0-abad-c2d16bf3b5b1   1Gi        RWO            Delete           Bound    kafka/data-0-my-cluster-kafka-0     local-path-retain-sc            3m6s
pvc-c9f91cea-ca3e-4d69-b4f2-4e29c563a3fc   1Gi        RWO            Delete           Bound    kafka/data-1-my-cluster-kafka-0     local-path-retain-sc            3m5s
pvc-b72086bb-df47-43bc-aa3c-01ec35fd6b29   1Gi        RWO            Delete           Bound    kafka/data-0-my-cluster-kafka-2     local-path-retain-sc            3m4s
pvc-a78e1e3f-410e-440f-a5c8-e58ad2f31c72   1Gi        RWO            Delete           Bound    kafka/data-1-my-cluster-kafka-2     local-path-retain-sc            3m3s
pvc-d13fac0c-d209-4b3c-a0e7-7c6b848fb1b9   1Gi        RWO            Delete           Bound    kafka/data-1-my-cluster-kafka-1     local-path-retain-sc            3m
```

The persistent volume is used by the Kafka brokers as log directories mounted into the following path:

```bash
for i in 0 1 2; do kubectl exec my-cluster-kafka-$i -- ls -l /var/lib/kafka/; done
```

```bash
for i in 0 1 2; do kubectl exec my-cluster-kafka-$i -- ls -l /var/lib/kafka/data-0; done
for i in 0 1 2; do kubectl exec my-cluster-kafka-$i -- ls -l /var/lib/kafka/data-1; done
```

Delete all PVCs
```bash
$ kubectl delete pvc `kubectl get pvc -o json | jq -r '.items[].metadata.name'`
```

Notice that the PVs are still kept. Using the `Retain` policy is important not to accidentally use data.

Delete all PVs
```bash
$ kubectl delete pv `kubectl get pv -o json | jq -r '.items[].metadata.name'`
```

# Adding volumes to JBOD storage

First lets create a topic:

```bash
$ kubectl apply -f my-topic.yaml
$ kubectl get kt -o yaml
```

Let's create some data:
```bash
$ kubectl run --restart=Never --image=quay.io/strimzi/kafka:0.32.0-kafka-3.3.1 my-pod -- /bin/sh -c "sleep 14400"
$ kubectl exec -it my-pod -- sh 
$ bin/kafka-producer-perf-test.sh \
--topic my-topic \
--throughput -1 \
--num-records 10000 \
--record-size 8000 \
--producer-props acks=all bootstrap.servers=my-cluster-kafka-bootstrap:9092


10000 records sent, 198.440260 records/sec (1.51 MB/sec), 14352.42 ms avg latency, 24735.00 ms max latency, 16580 ms 50th, 23221 ms 95th, 24331 ms 99th, 24701 ms 99.9th. 
```

Let see where are the data located:

```bash
$ kubectl exec -it my-pod /bin/bash -- bin/kafka-log-dirs.sh --describe --bootstrap-server my-cluster-kafka-bootstrap:9092 --broker-list 0,1,2 --topic-list my-topic |  grep '^{' | jq 
```

Let’s see if we can increase performance by moving a partition to use another JBOD volume.

Add a new JBOD volume:

```yaml
        - id: 2
          type: persistent-claim
          size: 1Gi
          class: local-path-retain-sc
```

```bash
$ kubectl apply -f kafka-jbod-persistent-claim.yaml
```

Run the `kafka-log-dirs` command again:

```bash
$ kubectl exec -it my-pod /bin/bash -- bin/kafka-log-dirs.sh --describe --bootstrap-server my-cluster-kafka-bootstrap:9092 --broker-list 0,1,2 --topic-list my-topic |  grep '^{' | jq  
```

There is a new `logDir` but there are no partitions there yet. 

Let's run the performance test again:

```bash
$ kubectl exec -it my-pod -- sh
bin/kafka-producer-perf-test.sh \
--topic my-topic \
--throughput -1 \
--num-records 10000 \
--record-size 8000 \
--producer-props acks=all bootstrap.servers=my-cluster-kafka-bootstrap:9092
```

We will notice that the new disk is not used. We need to move a partition to another disk.

We are going to use the `kafka-reassign-partitions` command.

The tool has three modes of operation:
1. --execute: This initiates a reassignment that you describe using a JSON file.
2. --generate: Generates a reassignment file used in the --execute step
3. --verify: checks whether reassignment started has completed

```bash
$ kubectl cp topics.json my-pod:/tmp/topics.json
$ kubectl exec -it my-pod /bin/bash -- bin/kafka-reassign-partitions.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 \
--topics-to-move-json-file /tmp/topics.json \
--broker-list 0,1,2 \
--generate

Current partition replica assignment
{"version":1,"partitions":[{"topic":"my-topic","partition":0,"replicas":[2,0,1],"log_dirs":["any","any","any"]},{"topic":"my-topic","partition":1,"replicas":[1,2,0],"log_dirs":["any","any","any"]},{"topic":"my-topic","partition":2,"replicas":[0,1,2],"log_dirs":["any","any","any"]}]}

Proposed partition reassignment configuration
{"version":1,"partitions":[{"topic":"my-topic","partition":0,"replicas":[1,0,2],"log_dirs":["any","any","any"]},{"topic":"my-topic","partition":1,"replicas":[2,1,0],"log_dirs":["any","any","any"]},{"topic":"my-topic","partition":2,"replicas":[0,2,1],"log_dirs":["any","any","any"]}]}
```

Modify the generated `reassignment.json` file which partiton to put where.

```bash
$ kubectl cp reassignment.json my-pod:/tmp/reassignment.json
```

Execute the reassignment:

```bash
$ kubectl exec -it my-pod /bin/bash -- bin/kafka-reassign-partitions.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 \
--reassignment-json-file /tmp/reassignment.json \
--execute
```

Verify:

```bash
kubectl exec -it my-pod /bin/bash -- bin/kafka-reassign-partitions.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 \
--reassignment-json-file /tmp/reassignment.json \
--verify

Status of partition reassignment:
Reassignment of partition my-topic-0 is completed.
Reassignment of partition my-topic-1 is completed.
Reassignment of partition my-topic-2 is completed.
Reassignment of replica my-topic-2-0 completed successfully.
Reassignment of replica my-topic-1-1 completed successfully.
Reassignment of replica my-topic-2-2 completed successfully.
Clearing broker-level throttles on brokers 0,1,2
Clearing topic-level throttles on topic my-topic
```

Run the `kafka-log-dirs` command again:

```bash
$ kubectl exec -it my-pod /bin/bash -- bin/kafka-log-dirs.sh --describe --bootstrap-server my-cluster-kafka-bootstrap:9092 --broker-list 0,1,2 --topic-list my-topic |  grep '^{' | jq  
```

Let's start the `kafka-producer-perf-test` tool again

```bash
$ kubectl exec -it my-pod -- sh
$ bin/kafka-producer-perf-test.sh \
--topic my-topic \
--throughput -1 \
--num-records 10000 \
--record-size 8000 \
--producer-props acks=all bootstrap.servers=my-cluster-kafka-bootstrap:9092
```

## kafka-topics command:

```bash
$ kubectl exec -it my-pod /bin/bash -- bin/kafka-topics.sh --describe --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic

Topic: my-topic	TopicId: 6J9xIO6fRmSog5k3IF97WQ	PartitionCount: 3	ReplicationFactor: 3	Configs: min.insync.replicas=1,segment.bytes=1073741824,retention.ms=7200000,message.format.version=3.0-IV1
	Topic: my-topic	Partition: 0	Leader: 1	Replicas: 1,2,0	Isr: 1,2,0
	Topic: my-topic	Partition: 1	Leader: 0	Replicas: 0,1,2	Isr: 0,1,2
	Topic: my-topic	Partition: 2	Leader: 2	Replicas: 2,0,1	Isr: 2,0,1
```

- To utilise a newly added JBOD volume it’s probably more convenient to perform a global rebalance 
using `KafkaRebalance` once the volume has been added.


# Remove volumes to JBOD storage

# Scaling clusters


## Cleanup

```bash
$ kubectl delete -f kafka-jbod-persistent-claim.yaml
$ kubectl delete pvc `kubectl get pvc -o json | jq -r '.items[].metadata.name'`
$ kubectl delete pv `kubectl get pv -o json | jq -r '.items[].metadata.name'`
```


Resources:

1. StatefulSet Removal [https://github.com/strimzi/proposals/blob/main/031-statefulset-removal.md](https://github.com/strimzi/proposals/blob/main/031-statefulset-removal.md)
2. UseStrimziPodSets feature gate [https://strimzi.io/docs/operators/in-development/configuring.html#ref-operator-use-strimzi-pod-sets-feature-gate-str](https://strimzi.io/docs/operators/in-development/configuring.html#ref-operator-use-strimzi-pod-sets-feature-gate-str)
3. Reassigning partitions in Apache Kafka Cluster [https://strimzi.io/blog/2022/09/16/reassign-partitions/](https://strimzi.io/blog/2022/09/16/reassign-partitions/)