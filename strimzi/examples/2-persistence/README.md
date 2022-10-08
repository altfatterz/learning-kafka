- Persistent storage uses `Persistent Volume Claims` to provision persistent volumes for storing data. 
- `Persistent Volume Claims` can be used to provision volumes depending on the `Storage Class`.

### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
# Start a k8s cluster with 1 agent node, 1 server node (control-plane), 
# we disable the loadbalancer in front of the server nodes and add a volume mapping
# we map the /data on the minikube VM to the /var/lib/rancher/k3s/storage 
$ k3d cluster create mycluster --agents 1 --no-lb -v /data:/var/lib/rancher/k3s/storage@all    
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

### 4. Watch the created PVCs and PVs

Terminal 1:
```bash
$ watch kubectl get pvc -n kafka
```

Terminal 2:
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
data-my-cluster-zookeeper-0   Bound    pvc-61c426f1-e783-44bd-b3a1-868e50609c15   1Gi        RWO            local-path     14m
data-my-cluster-kafka-0       Bound    pvc-754bbbd2-fd35-4f51-ad2c-79f7abf869b9   1Gi        RWO            local-path     13m

$ kubectl get pv -n kafka
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                               STORAGECLASS   REASON   AGE
pvc-61c426f1-e783-44bd-b3a1-868e50609c15   1Gi        RWO            Delete           Bound    kafka/data-my-cluster-zookeeper-0   local-path              16m
pvc-754bbbd2-fd35-4f51-ad2c-79f7abf869b9   1Gi        RWO            Delete           Bound    kafka/data-my-cluster-kafka-0       local-path              15m
```

Since we created a volume mapping to `/data` folder when creating the k8s cluster we can see the data in `minikube vm`

```bash
$ minikube ssh
$ sudo ls -l /data
drwxrwxrwx 4 root root 80 Oct  8 20:17 pvc-61c426f1-e783-44bd-b3a1-868e50609c15_kafka_data-my-cluster-zookeeper-0
drwxrwxrwx 3 root root 60 Oct  8 20:18 pvc-754bbbd2-fd35-4f51-ad2c-79f7abf869b9_kafka_data-my-cluster-kafka-0
```















