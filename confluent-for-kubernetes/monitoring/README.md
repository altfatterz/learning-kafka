### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent -p "9021:80@loadbalancer"
$ kubectl cluster-info
$ kubectl create ns confluent
$ kubectl config set-context --current --namespace confluent
# install ingress to expose control center easily at http://localhost:9021
$ kubectl apply -f ingress.yaml
```

### Import images and verify imported images

```bash
$ ./import-images
$ docker exec k3d-confluent-server-0 crictl images | grep 7.6.1
$ docker exec k3d-confluent-server-0 crictl images | grep 2.8.0
```

### Install the CFK operator

```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes --set kRaftEnabled=true
```

### Verify running pods

```bash
# wait until the operator is up and running
$ watch kubectl get pods --all-namespaces
```

### JMX Metrics

CFK deploys all Confluent components with JMX metrics enabled by default. 
These JMX metrics are made available on all pods at the following endpoints:

- JMX metrics are available on port 7203 of each pod.
- Jolokia (a REST interface for JMX metrics) is available on port 7777 of each pod.
- JMX Prometheus exporter is available on port 7778.

```bash
$ kubectl describe svc kafka

Port:              controller  9074/TCP
TargetPort:        9074/TCP
Endpoints:         10.42.0.19:9074,10.42.0.20:9074,10.42.0.21:9074
Port:              external  9092/TCP
TargetPort:        9092/TCP
Endpoints:         10.42.0.19:9092,10.42.0.20:9092,10.42.0.21:9092
Port:              http-external  8090/TCP
TargetPort:        8090/TCP
Endpoints:         10.42.0.19:8090,10.42.0.20:8090,10.42.0.21:8090
Port:              internal  9071/TCP
TargetPort:        9071/TCP
Endpoints:         10.42.0.19:9071,10.42.0.20:9071,10.42.0.21:9071
Port:              jmx  7203/TCP
TargetPort:        7203/TCP
Endpoints:         10.42.0.19:7203,10.42.0.20:7203,10.42.0.21:7203
Port:              jolokia  7777/TCP
TargetPort:        7777/TCP
Endpoints:         10.42.0.19:7777,10.42.0.20:7777,10.42.0.21:7777
Port:              prometheus  7778/TCP
TargetPort:        7778/TCP
Endpoints:         10.42.0.19:7778,10.42.0.20:7778,10.42.0.21:7778
Port:              replication  9072/TCP
TargetPort:        9072/TCP
Endpoints:         10.42.0.19:9072,10.42.0.20:9072,10.42.0.21:9072
```

Authentication / encryption is not supported for Prometheus exporter.

More info here: https://docs.confluent.io/operator/current/co-monitor-cp.html

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
$ curl localhost:7778 | grep kafka
```

### Expose control center using

```bash
$ kubectl confluent dashboard controlcenter
```

### Install prometheus and grafana

Using the helm chart: https://github.com/prometheus-community/helm-charts/


```bash
$ helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
$ helm repo update
$ helm install prom prometheus-community/kube-prometheus-stack -f prom-values.yaml
```

### Install podmonitor resource

```bash
$ kubectl apply -f pm-confluent.yaml -n confluent
```

Check the installed CRDs

```bash
$ kubectl get crds | grep coreos

alertmanagerconfigs.monitoring.coreos.com     2024-07-16T18:42:53Z
alertmanagers.monitoring.coreos.com           2024-07-16T18:42:53Z
podmonitors.monitoring.coreos.com             2024-07-16T18:42:53Z
probes.monitoring.coreos.com                  2024-07-16T18:42:53Z
prometheusagents.monitoring.coreos.com        2024-07-16T18:42:53Z
prometheuses.monitoring.coreos.com            2024-07-16T18:42:54Z
prometheusrules.monitoring.coreos.com         2024-07-16T18:42:54Z
scrapeconfigs.monitoring.coreos.com           2024-07-16T18:42:54Z
servicemonitors.monitoring.coreos.com         2024-07-16T18:42:54Z
thanosrulers.monitoring.coreos.com            2024-07-16T18:42:54Z
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

