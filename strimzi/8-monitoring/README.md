# Kafka with Metrics:

Strimzi uses the [Prometheus JMX Exporter](https://github.com/prometheus/jmx_exporter) to expose metrics 
through an HTTP endpoint which can be scraped by the Prometheus server.

### 1. Create a k8s cluster using k3d

```bash
# delete your previous cluster if you haven't done it.
$ k3d cluster delete mycluster
$ k3d cluster create mycluster --agents 3
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
```

### 2. Install the [Strimzi](https://strimzi.io/) operator

```bash
$ kubectl create ns kafka
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 3. Deploy a simple Kafka cluster (1 broker/1 zookeeper) with Prometheus JMX Exporter configured

```bash
$ kubectl apply -f kafka-with-jmx-prometheus-exporter.yaml -n kafka
```

### 4. Verify that the /metrics endpoint is working

```bash
$ kubectl port-forward pod/my-cluster-kafka-0 9404:9404 -n kafka
```

```bash
$ curl http://localhost:9404/metrics
```

### 5. Install the [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator)

```bash
$ kubectl create -f prometheus/prometheus-operator-deployment.yaml -n kafka
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

You should have the prometheus operator running in the `kafka` namespace

## 6. Deploy Prometheus

```bash
$ kubectl apply -f prometheus/prometheus-additional.yaml -n kafka 
$ kubectl apply -f prometheus/strimzi-pod-monitor.yaml -n kafka
$ kubectl apply -f prometheus/prometheus.yaml -n kafka
```

Access Prometheus UI and verify configured targets:

```bash
$ kubectl port-forward svc/prometheus-operated 9090:9090 -n kafka
```

http://localhst:9090

### 7. Verify the created resources:

```bash
$ kubectl get prom -n kafka

NAME         VERSION   REPLICAS   AGE
prometheus             1          2m32s
$ kubectl get pmon -n kafka

cluster-operator-metrics   2m28s
entity-operator-metrics    2m28s
bridge-metrics             2m28s
kafka-resources-metrics    2m28s
```

### 8. Install Grafana:

```bash
$ kubectl apply -f grafana.yaml -n kafka
```

```bash
$ kubectl port-forward svc/grafana -n kafka 3000:3000
```

http://localhost:3000
credentials: admin/admin

Add Prometheus as datasource with URL http://prometheus-operated:9090