Create k8s cluster and namespace

### Minikube

```bash
$ brew install minikube
$ minikube version
minikube version: v1.27.0
# if error occurs when starting the minikube use command to clean the minikube
$ minikube delete --all --purge
$ minikube start --driver=hyperkit --container-runtime=docker --memory 8192 --cpus 4 --no-kubernetes
$ eval $(minikube docker-env)
$ minikube ssh
$ minikube image
$ minikube ip
```

```bash
$ k3d cluster create mycluster --agents 3 -v $HOME/temp/strimzi:/var/lib/rancher/k3s/storage@all 
$ kubectl cluster-info
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
$ kubectl create ns kafka
```

```bash
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
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

Get the new resources:
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




By default, is using image `quay.io/strimzi/kafka:0.31.1-kafka-3.2.1` which is a redhat linux (`cat/etc/os-release`command)


Resources

1. [https://strimzi.io/docs/operators/latest/full/configuring.html](https://strimzi.io/docs/operators/latest/full/configuring.html)
2. [https://github.com/rmarting/strimzi-demo](https://github.com/rmarting/strimzi-demo)
3. [https://dzone.com/articles/grafana-and-prometheus-setup-with-strimzi-aka-kafk](https://dzone.com/articles/grafana-and-prometheus-setup-with-strimzi-aka-kafk)
4. [https://strimzi.io/docs/operators/latest/deploying.html#assembly-metrics-str](https://strimzi.io/docs/operators/latest/deploying.html#assembly-metrics-str)
5. [https://medium.com/rahasak/replace-docker-desktop-with-minikube-and-hyperkit-on-macos-783ce4fb39e3](https://medium.com/rahasak/replace-docker-desktop-with-minikube-and-hyperkit-on-macos-783ce4fb39e3)
6. [https://itnext.io/goodbye-docker-desktop-hello-minikube-3649f2a1c469](https://itnext.io/goodbye-docker-desktop-hello-minikube-3649f2a1c469)
7. [https://itnext.io/goodbye-docker-desktop-hello-minikube-3649f2a1c469](https://itnext.io/goodbye-docker-desktop-hello-minikube-3649f2a1c469)