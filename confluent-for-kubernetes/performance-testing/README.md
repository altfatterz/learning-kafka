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

### Generate a CA pair to use:

```
$ openssl genrsa -out ca-key.pem 2048

$ openssl req -new -key ca-key.pem -x509 \
  -days 1000 \
  -out ca.pem \
  -subj "/CN=ca1.mimacom.com/OU=development/O=mimacom/L=Zurich/C=CH"
```

### Create a Kubernetes secret for the certificate authority:

```
$ kubectl create secret tls ca-pair-sslcerts --cert=ca.pem --key=ca-key.pem
$ kubectl get secret ca-pair-sslcerts -o yaml

data:
 tls.crt:
 tls.key:
```

### Deploy Kafka cluster

```bash
$ kubectl apply -f confluent-platform-base.yaml
$ kubectl apply -f confluent-platform-controlcenter.yaml
```

### Create a configuration secret for client applications to use:

```bash
$ kubectl create secret generic kafka-client-config-secure --from-file=kafka.properties
$ kubectl get secret kafka-client-config-secure -o yaml
```

### Create topic

```bash
$ kubectl apply -f topic.yaml
```

### Deploy perf producer

```bash
$ kubectl logs -f perf-producer-54c97bb5f7-tzt57

251281 records sent, 50256.2 records/sec (49.08 MB/sec), 460.2 ms avg latency, 980.0 ms max latency.
609645 records sent, 121929.0 records/sec (119.07 MB/sec), 258.3 ms avg latency, 507.0 ms max latency.
645690 records sent, 129138.0 records/sec (126.11 MB/sec), 236.6 ms avg latency, 284.0 ms max latency.
542685 records sent, 108537.0 records/sec (105.99 MB/sec), 281.8 ms avg latency, 452.0 ms max latency.
512880 records sent, 102576.0 records/sec (100.17 MB/sec), 298.2 ms avg latency, 394.0 ms max latency.
3000000 records sent, 101102.011930 records/sec (98.73 MB/sec), 291.27 ms avg latency, 980.00 ms max latency, 275 ms 50th, 425 ms 95th, 614 ms 99th, 916 ms 99.9th.
```

### Deploy perf consumer

```bash
$ kubectl apply -f perf-consumer.yaml 
$ kubectl logs -f perf-consumer-f69qc

start.time: 2024-07-17 19:57:43:209 
end.time: 2024-07-17 19:57:56:271
data.consumed.in.MB: 2930.1025 
MB.sec: 224.3227
data.consumed.in.nMsg: 3000425 
nMsg.sec: 229706.4002
rebalance.time.ms: 3758
fetch.time.ms: 9304 
fetch.MB.sec: 314.9293
fetch.nMsg.sec: 322487.6397
```

### Deploy perf end-to-end

```bash
$ kubectl apply -f perf-endtoend-yaml
$ kubectl logs -f perf-endtoend-562hm

0	91.077158
1000	1.993549
2000	1.812902
3000	1.79668
4000	1.662695
5000	1.829755
6000	1.7018440000000001
7000	3.380487
8000	1.913619
9000	2.254422
Avg latency: 2.1601 ms
Percentiles: 50th = 1, 99th = 5, 99.9th = 44
```

Resources:
- 99th Percentile Latency at Scale with Apache Kafka https://www.confluent.io/blog/configure-kafka-to-minimize-latency/
