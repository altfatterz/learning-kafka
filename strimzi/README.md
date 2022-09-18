Create k8s cluster and namespace

```bash
$ k3d cluster create mycluster --agents 3 -v $HOME/temp/strimzi:/var/lib/rancher/k3s/storage@all 
$ kubectl cluster-info
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
$ kubectl create ns kafka
```

```bash
$ curl -L http://strimzi.io/install/latest \
  | sed 's/namespace: .*/namespace: kafka/' \
  | kubectl apply -f - -n kafka  
```

Output:
```bash
clusterrole.rbac.authorization.k8s.io/strimzi-kafka-client created
clusterrole.rbac.authorization.k8s.io/strimzi-cluster-operator-namespaced created
customresourcedefinition.apiextensions.k8s.io/kafkarebalances.kafka.strimzi.io created
clusterrole.rbac.authorization.k8s.io/strimzi-entity-operator created
clusterrolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator-kafka-broker-delegation created
serviceaccount/strimzi-cluster-operator created
customresourcedefinition.apiextensions.k8s.io/kafkaconnectors.kafka.strimzi.io created
rolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator created
deployment.apps/strimzi-cluster-operator created
rolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator-leader-election created
customresourcedefinition.apiextensions.k8s.io/strimzipodsets.core.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkatopics.kafka.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkausers.kafka.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkabridges.kafka.strimzi.io created
clusterrole.rbac.authorization.k8s.io/strimzi-cluster-operator-leader-election created
clusterrolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator created
clusterrole.rbac.authorization.k8s.io/strimzi-kafka-broker created
clusterrolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator-kafka-client-delegation created
clusterrole.rbac.authorization.k8s.io/strimzi-cluster-operator-global created
customresourcedefinition.apiextensions.k8s.io/kafkaconnects.kafka.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkamirrormaker2s.kafka.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkas.kafka.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkamirrormakers.kafka.strimzi.io created
configmap/strimzi-cluster-operator created
rolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator-entity-operator-delegation created
```

Get CRDs:
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
$ watch kubectl get pods -n kafka

NAME                                       READY   STATUS    RESTARTS   AGE
strimzi-cluster-operator-854758757-rc2fv   0/1     Running   0          31s
```

Create 1 zookeeper node and 3 kafka brokers:

```bash
$ kubectl apply -n kafka -f kafka-ephemeral-multiple.yaml
```

```bash
$ kubectl get pods -n kafka -o wide

NAME                                          READY   STATUS    RESTARTS   AGE     IP          NODE                    NOMINATED NODE   READINESS GATES
strimzi-cluster-operator-854758757-4n4vw      1/1     Running   0          8m23s   10.42.3.4   k3d-mycluster-agent-1   <none>           <none>
my-cluster-zookeeper-0                        1/1     Running   0          78s     10.42.1.6   k3d-mycluster-agent-2   <none>           <none>
my-cluster-kafka-1                            1/1     Running   0          54s     10.42.3.6   k3d-mycluster-agent-1   <none>           <none>
my-cluster-kafka-0                            1/1     Running   0          54s     10.42.1.7   k3d-mycluster-agent-2   <none>           <none>
my-cluster-kafka-2                            1/1     Running   0          54s     10.42.2.9   k3d-mycluster-agent-0   <none>           <none>
my-cluster-entity-operator-54f8746cb8-85nbt   1/3     Running   0          11s     10.42.3.7   k3d-mycluster-agent-1   <none>           <none>
```

After the services are up and running, create a topic:

```bash
$ kubectl apply -n kafka -f kafka-topic.yaml
```

Verify
```bash
$ kubectl -n kafka run kafka-topics -ti --image=bitnami/kafka:3.2.1 --rm=true --restart=Never -- kafka-topics.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --list
```

Test connection:

Producer:

```bash
$ kubectl -n kafka run kafka-producer -ti --image=bitnami/kafka:3.2.1 --rm=true --restart=Never -- kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic
```

Consumer:
```bash
$ kubectl -n kafka run kafka-consumer -ti --image=bitnami/kafka:3.2.1 --rm=true --restart=Never -- kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```

The ephemeral volume is used by the Kafka brokers as log directories mounted into the following path:

```bash
$ kubectl exec -it my-cluster-kafka-0 -n kafka -- ls /var/lib/kafka/data/kafka-log0
$ kubectl exec -it my-cluster-kafka-1 -n kafka -- ls /var/lib/kafka/data/kafka-log1
$ kubectl exec -it my-cluster-kafka-2 -n kafka -- ls /var/lib/kafka/data/kafka-log2
```

# Persistent volume

Local Path provisioner in k3d cluster used by the default storage class
[https://k3d.io/v5.4.6/usage/k3s/?h=storage#local-path-provisioner-in-k3d](https://k3d.io/v5.4.6/usage/k3s/?h=storage#local-path-provisioner-in-k3d)

```bash
$ kubectl get sc 
NAME                   PROVISIONER             RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
local-path (default)   rancher.io/local-path   Delete          WaitForFirstConsumer   false                  55m
```

```bash
$ watch kubectl get pv
$ watch kubectl get pvc -n kafka
```

```bash
$ kubectl apply -n kafka -f kafka-persistent-claim.yaml
```

# Kafka with Metrics:

Strimzi uses the [Prometheus JMX Exporter](https://github.com/prometheus/jmx_exporter) to expose metrics through an HTTP endpoint, 
which can be scraped by the Prometheus server.

```bash
$ kubectl apply -f kafka-with-jmx-prometheus-exporter.yaml -n kafka
```

```bash
$ kubectl port-forward pod/my-cluster-kafka-0 9404:9404 -n kafka
```

```bash
$ curl http://localhost:9404/metrics
```

# Prometheus

[**Prometheus Operator**](https://github.com/prometheus-operator/prometheus-operator)





Resources

1. [https://strimzi.io/docs/operators/latest/full/configuring.html](https://strimzi.io/docs/operators/latest/full/configuring.html)



Hello everybody! I would like to expose some metrics using the Prometheus JMX Exporter. I am following the https://strimzi.io/docs/operators/latest/full/deploying.html#ref-metrics-prometheus-metrics-config-str and I am using the latest Strimzi (0.31.0).
Here is my config. 


To my understanding the broker service should expose the 9404 port to access the metrics via 9404/metrics endpoint, but I don't see it being exposed:

```bash
$ kubectl get svc -n kafka
my-cluster-zookeeper-client   ClusterIP   10.43.203.145   <none>        2181/TCP                              31m
my-cluster-zookeeper-nodes    ClusterIP   None            <none>        2181/TCP,2888/TCP,3888/TCP            31m
my-cluster-kafka-brokers      ClusterIP   None            <none>        9090/TCP,9091/TCP,9092/TCP,9093/TCP   30m
my-cluster-kafka-bootstrap    ClusterIP   10.43.180.111   <none>        9091/TCP,9092/TCP,9093/TCP            30m
```
What do I miss? 

