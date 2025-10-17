### 1. Create a k8s cluster using k3d

- Persistent storage uses `Persistent Volume Claims` to provision persistent volumes for storing data.
- `Persistent Volume Claims` can be used to provision volumes depending on the `Storage Class`.

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
local-path (default)   rancher.io/local-path   Delete          WaitForFirstConsumer   false                  20s
```

### 3. Install the [Strimzi](https://strimzi.io/) operator

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 4. Wait until the operator is running

```bash
$ kubectl get pods -n kafka
NAME                                        READY   STATUS    RESTARTS   AGE
strimzi-cluster-operator-7c88589497-j97hl   1/1     Running   0          46s
```

### 4. Deploy a single node Kafka cluster but with storage configured using `persistence volume claim`

```bash
$ kubectl apply -f kafka-persistent-claim.yaml -n kafka 
```

The following `PVC`s and `PV`s are created:

```bash
$ kubectl get pvc -n kafka
NAME                            STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   VOLUMEATTRIBUTESCLASS   AGE
data-0-my-cluster-dual-role-0   Bound    pvc-4c2da424-dd08-4af8-b85d-889e015a0430   200Mi      RWO            local-path     <unset>                 8s

$ kubectl get pv -n kafka
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                                 STORAGECLASS   VOLUMEATTRIBUTESCLASS   REASON   AGE
pvc-4c2da424-dd08-4af8-b85d-889e015a0430   200Mi      RWO            Delete           Bound    kafka/data-0-my-cluster-dual-role-0   local-path     <unset>                          8s
```

```bash
$ ls -l /tmp/kafka-volume
total 0
drwxrwxrwx@ 3 altfatterz  wheel  96 Oct 17 16:21 pvc-4c2da424-dd08-4af8-b85d-889e015a0430_kafka_data-0-my-cluster-dual-role-0
```

### 5. Create a topic using a strimzi resource definition:

```bash
$ kubectl get pods -n kafka

NAME                                          READY   STATUS    RESTARTS   AGE
my-cluster-dual-role-0                        1/1     Running   0          69s
my-cluster-entity-operator-7c86ff87cf-xx6mg   2/2     Running   0          31s
strimzi-cluster-operator-7c88589497-j97hl     1/1     Running   0          4m18s
```

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
$ kubectl -n kafka run kafka-producer -ti --image=quay.io/strimzi/kafka:0.48.0-kafka-4.1.0 --rm --restart=Never -- bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic
```

In one another terminal create a consumer: (this will start up a `kafka-consumer` pod)

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.48.0-kafka-4.1.0 --rm --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```

### 7. Check data:

Delete the `my-cluster-dual-role-0` pod and you will see the `my-topic` is still there.

```bash
$ k delete pod my-cluster-dual-role-0 -n kafka
```

```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.48.0-kafka-4.1.0 --rm --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```


### 8. Cleanup

- Deleting your Apache Kafka cluster

```bash
$ kubectl -n kafka delete $(kubectl get strimzi -o name -n kafka)
```
This will remove all Strimzi custom resources, including the Apache Kafka cluster and any KafkaTopic custom resources
but leave the Strimzi cluster operator running so that it can respond to new Kafka custom resources.

- Deleting the PVCs

```bash
$ kubectl delete pvc -l strimzi.io/name=my-cluster-kafka -n kafka
```

Without deleting the PVC, the next Kafka cluster you might start will fail as it will try to use the volume that belonged to the previous Apache Kafka cluster.

- Deleting the Strimzi cluster operator

```bash
$ kubectl -n kafka delete -f 'https://strimzi.io/install/latest?namespace=kafka'
```

- Delete the Kubernetes cluster

```bash
$ k3d cluster delete mycluster
```
















