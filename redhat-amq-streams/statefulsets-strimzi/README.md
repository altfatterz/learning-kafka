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
$ kubectl create ns kafka
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

Deploy a Kafka 3 node cluster (3 node Zookeeper) with `persistent-claim` persistence configured.

```bash
$ kubectl apply -f kafka-persistent-claim.yaml
```

Check what resources have been created:

1. StatefulSets:

```bash
NAME                   READY   AGE
my-cluster-zookeeper   3/3     5m6s
my-cluster-kafka       3/3     3m31s
```

2. Secrets:

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
data-my-cluster-zookeeper-1   Bound    pvc-f69d1db8-fa67-4f01-afc6-ca1e83697b38   1Gi        RWO            local-path     7m33s
data-my-cluster-zookeeper-2   Bound    pvc-d50ec5f8-79c9-40d9-adc6-04445699b52f   1Gi        RWO            local-path     7m33s
data-my-cluster-zookeeper-0   Bound    pvc-f388ec54-f51e-4089-8f10-8cdac794ddbd   1Gi        RWO            local-path     7m33s
data-my-cluster-kafka-0       Bound    pvc-e6d44d2c-f186-4247-9283-fea9db981dfc   1Gi        RWO            local-path     5m58s
data-my-cluster-kafka-2       Bound    pvc-d1d3738f-eb83-49cd-ae8d-3335bea73f13   1Gi        RWO            local-path     5m58s
data-my-cluster-kafka-1       Bound    pvc-5585b693-d254-435d-abf5-0d17f1035487   1Gi        RWO            local-path     5m58s
```

6. PVs:
 
```bash
$ kubectl get pv
NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                               STORAGECLASS   REASON   AGE
pvc-f388ec54-f51e-4089-8f10-8cdac794ddbd   1Gi        RWO            Delete           Bound    kafka/data-my-cluster-zookeeper-0   local-path              7m40s
pvc-f69d1db8-fa67-4f01-afc6-ca1e83697b38   1Gi        RWO            Delete           Bound    kafka/data-my-cluster-zookeeper-1   local-path              7m39s
pvc-d50ec5f8-79c9-40d9-adc6-04445699b52f   1Gi        RWO            Delete           Bound    kafka/data-my-cluster-zookeeper-2   local-path              7m39s
pvc-e6d44d2c-f186-4247-9283-fea9db981dfc   1Gi        RWO            Delete           Bound    kafka/data-my-cluster-kafka-0       local-path              6m8s
pvc-5585b693-d254-435d-abf5-0d17f1035487   1Gi        RWO            Delete           Bound    kafka/data-my-cluster-kafka-1       local-path              6m7s
pvc-d1d3738f-eb83-49cd-ae8d-3335bea73f13   1Gi        RWO            Delete           Bound    kafka/data-my-cluster-kafka-2       local-path              6m6s
```


Resources:

1. StatefulSet Removal [https://github.com/strimzi/proposals/blob/main/031-statefulset-removal.md](https://github.com/strimzi/proposals/blob/main/031-statefulset-removal.md)
2. UseStrimziPodSets feature gate [https://strimzi.io/docs/operators/in-development/configuring.html#ref-operator-use-strimzi-pod-sets-feature-gate-str](https://strimzi.io/docs/operators/in-development/configuring.html#ref-operator-use-strimzi-pod-sets-feature-gate-str)
3. 