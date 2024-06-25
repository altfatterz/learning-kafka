### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent
$ kubectl cluster-info
$ kubectl create ns confluent
$ kubectl config set-context --current --namespace confluent
```

```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes --set kRaftEnabled=true
## wait until the operator is up and running
$ kubectl get pods
```

```bash
$ kubectl apply -f confluent-platform.yaml
```

```bash
$ kubectl get svc

NAME                         TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                                                                   AGE
confluent-operator           ClusterIP   10.43.64.27     <none>        7778/TCP                                                                  38m
kraftcontroller              ClusterIP   None            <none>        9074/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP                              37m
connect                      ClusterIP   None            <none>        8083/TCP,7203/TCP,7777/TCP,7778/TCP                                       37m
kraftcontroller-0-internal   ClusterIP   10.43.174.127   <none>        9074/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP                              37m
connect-0-internal           ClusterIP   10.43.63.54     <none>        8083/TCP,7203/TCP,7777/TCP,7778/TCP                                       37m
kafka                        ClusterIP   None            <none>        9074/TCP,9092/TCP,8090/TCP,9071/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP   36m
kafka-0-internal             ClusterIP   10.43.226.23    <none>        9074/TCP,9092/TCP,8090/TCP,9071/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP   36m
controlcenter                ClusterIP   None            <none>        9021/TCP,7203/TCP,7777/TCP,7778/TCP                                       35m
schemaregistry               ClusterIP   None            <none>        8081/TCP,7203/TCP,7777/TCP,7778/TCP                                       35m
controlcenter-0-internal     ClusterIP   10.43.48.218    <none>        9021/TCP,7203/TCP,7777/TCP,7778/TCP                                       35m
schemaregistry-0-internal    ClusterIP   10.43.213.81    <none>        8081/TCP,7203/TCP,7777/TCP,7778/TCP                                       35m
kafka-1-internal             ClusterIP   10.43.101.15    <none>        9074/TCP,9092/TCP,8090/TCP,9071/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP   15m
kafka-2-internal             ClusterIP   10.43.209.219   <none>        9074/TCP,9092/TCP,8090/TCP,9071/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP   15m
```

- JMX metrics are available on port 7203 of each pod.
- Jolokia (a REST interface for JMX metrics) is available on port 7777 of each pod.
- JMX Prometheus exporter is available on port 7778.

```bash
$ kubectl exec -it kafka-0 -- bash
```

Jolokia (https://jolokia.org/ -  is remote JMX with JSON over HTTP)
```bash
$ curl localhost:7777/jolokia/read/java.lang:type=Memory/HeapMemoryUsage

{"request":{"mbean":"java.lang:type=Memory","attribute":"HeapMemoryUsage","type":"read"},"value":{"init":268435456,"committed":2080374784,"max":4194304000,"used":624901880},"timestamp":1718873550,"status":200}
```

JMX Prometheus exporter (https://github.com/prometheus/jmx_exporter)
```bash
$ curl localhost:7778
$ curl localhost:7778 | grep 
```

### Expose control center using

```bash
$ kubectl confluent dashboard controlcenter
```

### Install prometheus and grafana

```bash
$ helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
$ helm repo update
$ helm install prom prometheus-community/kube-prometheus-stack -f prom-values.yaml
```

### Install PodMonitor resource

```bash
$ kubectl apply -f pm-confluent.yaml
```

### TODO

Connect Prometheus / Grafana

------------------------------------------------------------------------------------------------------------------------

### Monitor with Metrics Reporter

The Confluent Metrics Reporter is automatically installed onto Kafka brokers if they are running Confluent Platform.

```bash
$ kubectl exec -it kafka-0 -- bash
ls /usr/share/java/confluent-telemetry/
confluent-metrics-7.6.1-ce.jar
```

------------------------------------------------------------------------------------------------------------------------

### Telemetry enabled

```bash
$ kubectl logs -f kafka-0 | grep confluent.telemetry.enabled
```

The setup of Health+ involves configuring the Confluent Telemetry Reporter on each Confluent Platform service that will be monitored.


Resources:

- Doc: https://docs.confluent.io/platform/current/kafka/monitoring.html
- Doc: https://docs.confluent.io/operator/current/co-monitor-cp.html
- Health+ Doc: https://docs.confluent.io/platform/current/health-plus/index.html
- Code: https://github.com/confluentinc/jmx-monitoring-stacks/tree/main/jmxexporter-prometheus-grafana
- Code: https://github.com/confluentinc/cp-demo
- Health+: https://confluent.cloud/health-plus/welcome

