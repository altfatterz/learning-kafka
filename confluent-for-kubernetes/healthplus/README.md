### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent
$ kubectl cluster-info
$ kubectl create ns confluent
$ kubectl config set-context --current --namespace confluent
```

```bash
$ kubectl create secret generic telemetry --from-file=./telemetry.txt
$ kubectl get secret telemetry -o yaml
```

```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes \
--values confluent-operator-values.yaml \
--namespace confluent
## wait until the operator is up and running
$ kubectl get pods
```

Check logs of the confluent-operator
```bash
$ kubectl logs -f confluent-operator-5d5d74567d-dl9s8

{"level":"INFO","time":"2024-06-20T13:39:34.850Z","name":"setup","caller":"log/log.go:31","msg":"Fips mode is set to : ","FIPS mode":false}
{"level":"INFO","time":"2024-06-20T13:39:34.850Z","name":"setup","caller":"log/log.go:31","msg":"KRaftClusterIdRecovery is set to : ","KRaftClusterIdRecovery":true}
{"level":"INFO","time":"2024-06-20T13:39:34.850Z","name":"setup","caller":"log/log.go:31","msg":"confluent telemetry reporter is enabled for CP"}
{"level":"INFO","time":"2024-06-20T13:39:34.850Z","name":"setup","caller":"log/log.go:31","msg":"confluent telemetry reporter is enabled for Operator"}
{"level":"INFO","time":"2024-06-20T13:39:34.852Z","name":"setup","caller":"log/log.go:31","msg":"confluent telemetry reporter is using secret","secretRef":"telemetry"}
{"level":"INFO","time":"2024-06-20T13:39:34.871Z","name":"setup","caller":"log/log.go:31","msg":"Starting Confluent Operator","version":"v0.921.20","build_time":"2024-04-12T14:53:00Z","kubernetes_version":"v1.28.8+k3s1"}
```

```bash
$ kubectl apply -f confluent-platform.yaml
```

Check:

https://confluent.cloud/health-plus/

Expose control center

```bash
$ kubectl confluent dashboard controlcenter
```

View again Confluent Cloud UI

```bash
$ kubectl apply -f topic.yaml
$ kubectl apply -f connector.yaml
```

Resouces: 
- https://docs.confluent.io/operator/2.8/co-monitor-cp.html
- https://docs.confluent.io/operator/current/co-monitor-cp.html#telemetry-reporter
- How to use OpenTelemetry to Trace and Monitor Apache Kafka Systems: https://www.youtube.com/watch?v=-amJ80DZzv8
- https://www.youtube.com/watch?v=7L1JgkF99LU