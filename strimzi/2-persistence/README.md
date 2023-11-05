- Persistent storage uses `Persistent Volume Claims` to provision persistent volumes for storing data. 
- `Persistent Volume Claims` can be used to provision volumes depending on the `Storage Class`.

### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
# Start a k8s cluster with 1 agent node, 1 server node (control-plane), 
# we disable the loadbalancer in front of the server nodes and add a volume mapping
# we map the /tmp/kafka-volume to the /var/lib/rancher/k3s/storage
$ rm -r /tmp/kafka-volume
$ mkdir -p /tmp/kafka-volume 
$ k3d cluster create mycluster --agents 1 --no-lb -v /tmp/kafka-volume/:/var/lib/rancher/k3s/storage@all    
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule 
# create the `kafka` namespace
$ kubectl create ns kafka
```

### 2. Check the `storage class` available in our k8s cluster:

```bash
$ kubectl get sc
NAME                   PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
local-path (default)   rancher.io/local-path   Delete          WaitForFirstConsumer   false                  81s
```

### 3. Install the [Strimzi](https://strimzi.io/) operator

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 4. Watch the created Pods PVCs and PVs

Terminal 1:
```bash
$ watch kubectl get all -n kafka
```

Terminal 2:
```bash
$ watch kubectl get pvc -n kafka
```

Terminal 3:
```bash
$ watch kubectl get pv -n kafka
```

### 4. Deploy a simple Kafka cluster (1 broker/1 zookeeper) but with storage configured using `persistence volume claim`

```bash
$ kubectl apply -f kafka-persistent-claim.yaml -n kafka 
```

The following `PVC`s and `PV`s are created:

```bash
$ kubectl get pvc -n kafka
NAME                          STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   AGE
data-my-cluster-zookeeper-0   Bound    pvc-b049f816-adaf-44c3-9c97-e88fd2c47c2f   1Gi        RWO            local-path     3m1s
data-my-cluster-kafka-0       Bound    pvc-18aad7b8-85eb-4cc0-9500-7db171f053f1   1Gi        RWO            local-path     2m3s

$ kubectl get pv -n kafka
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                               STORAGECLASS   REASON   AGE
pvc-b049f816-adaf-44c3-9c97-e88fd2c47c2f   1Gi        RWO            Delete           Bound    kafka/data-my-cluster-zookeeper-0   local-path              3m7s
pvc-18aad7b8-85eb-4cc0-9500-7db171f053f1   1Gi        RWO            Delete           Bound    kafka/data-my-cluster-kafka-0       local-path              2m12s
```

```bash
$ ls -l /tmp/kafka-volume
total 0
drwxrwxrwx@ 3 altfatterz  wheel   96 Nov  5 13:43 pvc-18aad7b8-85eb-4cc0-9500-7db171f053f1_kafka_data-my-cluster-kafka-0
drwxrwxrwx@ 4 altfatterz  wheel  128 Nov  5 13:42 pvc-b049f816-adaf-44c3-9c97-e88fd2c47c2f_kafka_data-my-cluster-zookeeper-0
```

### 5. Create a topic using a strimzi resource definition:

After the services are up and running, create a topic:

```bash
$ kubectl apply -n kafka -f kafka-topic.yaml
```

Verify that the resource details:

```bash
$ kubectl get kt -n kafka
$ kubectl describe kt my-topic -n kafka
```

### 6. Test the cluster console consumer and console producer within the cluster:

In one terminal create a producer: (this will start up the `kafka-producer` pod)

```bash
$ kubectl -n kafka run kafka-producer -ti --image=quay.io/strimzi/kafka:0.38.0-kafka-3.6.0 --rm=true --restart=Never -- bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic
```

In one another terminal create a consumer: (this will start up a `kafka-consumer` pod)

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.38.0-kafka-3.6.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```

### 7. Check data:

```bash
$ tree /tmp/kafka-volume
```

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
















