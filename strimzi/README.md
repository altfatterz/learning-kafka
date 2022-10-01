Create k8s cluster and namespace

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

# Kafka Connect

`KafkaConnect` and `KafkaConnector` resources [https://strimzi.io/docs/operators/latest/deploying.html#kafka-connect-str](https://strimzi.io/docs/operators/latest/deploying.html#kafka-connect-str)

```bash
$ kubectl apply -f kafka-connect-with-source-connector.yaml -n kafka
```
First `my-connect-cluster-connect-build` job is create to create a custom image and upload it to the
`ttl.sh/altfatterz-strimzi-kafka-connect-3.2.1:2h` (valid for 2 hours)

Check the logs what is doing:

```bash
$ stern my-connect-cluster-connect-build -n kafka
```

Then a `my-connect-cluster-connect-7d6b9b7c8c-5z5xb` pod is created which is the actual Connect Instance with the File Source Connector installed on it.

Next create the `my-topic` via

```bash
$ kubectl apply -f kafka-topic.yaml -n kafka
$ kubectl get kt -n kafka
```

Start a consumer
```bash
$ kubectl -n kafka run kafka-consumer -ti --image=bitnami/kafka:3.2.1 --rm=true --restart=Never -- kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```

And then configure the File Source Connector using:

```bash
$ kubectl apply -f kafka-source-connector.yaml -n kafka
```

In the consumer logs you should see the messages from the topic

```bash
{"schema":{"type":"string","optional":false},"payload":"This product contains the dnsinfo.h header file, that provides a way to retrieve the system DNS configuration on MacOS."}
{"schema":{"type":"string","optional":false},"payload":"This private header is also used by Apple's open source"}
{"schema":{"type":"string","optional":false},"payload":" mDNSResponder (https://opensource.apple.com/tarballs/mDNSResponder/)."}
{"schema":{"type":"string","optional":false},"payload":""}
{"schema":{"type":"string","optional":false},"payload":" * LICENSE:"}
{"schema":{"type":"string","optional":false},"payload":"    * license/LICENSE.dnsinfo.txt (Apple Public Source License 2.0)"}
{"schema":{"type":"string","optional":false},"payload":"  * HOMEPAGE:"}
```

```bash
$ kubectl api-resources | grep connect

kafkaconnectors                   kctr         kafka.strimzi.io/v1beta2               true         KafkaConnector
kafkaconnects                     kc           kafka.strimzi.io/v1beta2               true         KafkaConnect
```


# Prometheus

## Install the [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator)

```bash
$ curl -s https://raw.githubusercontent.com/coreos/prometheus-operator/master/bundle.yaml \
 | sed -e '/[[:space:]]*namespace: [a-zA-Z0-9-]*$/s/namespace:[[:space:]]*[a-zA-Z0-9-]*$/namespace: kafka/' \
 > prometheus-operator-deployment.yaml
```

The API resources installed:

```bash
$ kubectl api-resources | grep coreos
alertmanagerconfigs               amcfg        monitoring.coreos.com/v1alpha1         true         AlertmanagerConfig
alertmanagers                     am           monitoring.coreos.com/v1               true         Alertmanager
podmonitors                       pmon         monitoring.coreos.com/v1               true         PodMonitor
probes                            prb          monitoring.coreos.com/v1               true         Probe
prometheuses                      prom         monitoring.coreos.com/v1               true         Prometheus
prometheusrules                   promrule     monitoring.coreos.com/v1               true         PrometheusRule
servicemonitors                   smon         monitoring.coreos.com/v1               true         ServiceMonitor
thanosrulers                      ruler        monitoring.coreos.com/v1               true         ThanosRuler
```

```bash
$ kubectl create -f prometheus-operator-deployment.yaml -n kafka
```
You should have the prometheus operator running in the `kafka` namespace

## Deploy Prometheus 

```bash
$ kubectl apply -f prometheus-additional.yaml -n kafka 
$ kubectl apply -f strimzi-pod-monitor.yaml -n kafka
$ kubectl apply -f prometheus-rules.yaml -n kafka (optional)
$ kubectl apply -f prometheus.yaml -n kafka
```
 
Prometheus should have started within a pod.

Access Prometheus UI and verify configured targets:
```bash
$ kubectl port-forward svc/prometheus-operated 9090:9090 -n kafka
```

Created resources:

Resources created:

```bash
$ kubectl get prom -n kafka
$ kubectl get promrule -n kafka
$ kubectl get pmon -n kafka
```

# Grafana

```bash
$ kubectl apply -f grafana.yaml -n kafka
```

```bash
$ kubectl port-forward svc/grafana -n kafka 3000:3000
```

http://localhost:3000 
credentials: admin/admin

Add Prometheus as datasource with URL http://prometheus-operated:9090



### Nodeport 

```bash
$ k3d cluster create mycluster -p "8080-8082:30080-30082@agent:0" --agents 1
$ docker ps -a
1fd35518a5cc   ghcr.io/k3d-io/k3d-proxy:5.4.6   "/bin/sh -c nginx-pr…"   2 hours ago   Up 2 hours   80/tcp, 0.0.0.0:54725->6443/tcp, 0.0.0.0:8080->30080/tcp, 0.0.0.0:8081->30081/tcp   k3d-mycluster-serverlb
```

```bash
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
$ kubectl create ns kafka
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
$ kubectl apply -f kafka-nodeport.yaml -n kafka
```

```bash
$ docker run -it --rm quay.io/strimzi/kafka:0.31.1-kafka-3.2.1 sh
$ bin/kafka-topics.sh --bootstrap-server 172.28.0.4:30081 --list
```

https://strimzi.io/docs/operators/in-development/configuring.html#property-listener-config-preferredNodePortAddressType-reference


```bash
$ kubectl -n kafka exec my-cluster-kafka-0 -c kafka -it -- cat /tmp/strimzi.properties | grep advertised
advertised.listeners=CONTROLPLANE-9090://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9095://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9095,EXTERNAL-9096://172.18.0.3:30081

$ kubectl -n kafka exec my-cluster-kafka-0 -c kafka -it -- cat /opt/kafka/custom-config/server.config | grep advertised
advertised.listeners=CONTROLPLANE-9090://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9090,REPLICATION-9091://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9091,PLAIN-9095://my-cluster-kafka-0.my-cluster-kafka-brokers.kafka.svc:9095,EXTERNAL-9096://${STRIMZI_NODEPORT_DEFAULT_ADDRESS}:30081
```


### Configure image

By default, is using image `quay.io/strimzi/kafka:0.31.1-kafka-3.2.1` which is a redhat linux (`cat/etc/os-release`command) 


### Minikube

```bash
$ brew install minikube
$ minikube version
minikube version: v1.27.0
# if error occues when starting the minikube use bleow command to clean the minikube
$ minikube delete --all --purge
$ minikube start --driver=hyperkit --container-runtime=docker --memory 8192 --cpus 4 --docker-opt=bip=172.17.42.1/16 --no-kubernetes
$ eval $(minikube docker-env)
$ minikube ssh
$ minikube image
$ minikube ip
```

Resources

1. [https://strimzi.io/docs/operators/latest/full/configuring.html](https://strimzi.io/docs/operators/latest/full/configuring.html)
2. [https://github.com/rmarting/strimzi-demo](https://github.com/rmarting/strimzi-demo)
3. [https://dzone.com/articles/grafana-and-prometheus-setup-with-strimzi-aka-kafk](https://dzone.com/articles/grafana-and-prometheus-setup-with-strimzi-aka-kafk)
4. [https://strimzi.io/docs/operators/latest/deploying.html#assembly-metrics-str](https://strimzi.io/docs/operators/latest/deploying.html#assembly-metrics-str)
5. [https://medium.com/rahasak/replace-docker-desktop-with-minikube-and-hyperkit-on-macos-783ce4fb39e3](https://medium.com/rahasak/replace-docker-desktop-with-minikube-and-hyperkit-on-macos-783ce4fb39e3)
6. [https://itnext.io/goodbye-docker-desktop-hello-minikube-3649f2a1c469](https://itnext.io/goodbye-docker-desktop-hello-minikube-3649f2a1c469)