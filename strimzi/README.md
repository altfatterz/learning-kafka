Create k8s cluster and namespace

```bash
$ k3d cluster create mycluster --agents=3
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

Test connection:

Producer:

```bash
$ kubectl -n kafka run kafka-producer -ti --image=bitnami/kafka:3.2.1 --rm=true --restart=Never -- kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic
```

Consumer:
```bash
$ kubectl -n kafka run kafka-consumer -ti --image=bitnami/kafka:3.2.1 --rm=true --restart=Never -- kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```





