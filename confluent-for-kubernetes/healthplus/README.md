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

- #### Out of sync replicas
  - Unclean leader elections are caused when there are no available in-sync replicas for a partition 
  (either due to network issues, lag causing the broker to fall behind, or brokers going down completely), 
  so an out of sync replica is the only option for the leader.

- #### Under Replicated Partitions
  - A partition will also be considered under-replicated if the correct number of replicas exist, but one or more of 
  the replicas have fallen significantly behind the partition leader.

- #### Under Min In-sync Replicas
  - `min.insync.replicas` is a config on the broker that denotes the minimum number of in-sync replicas required to 
  exist for a broker to allow acks=all requests. That is, all requests with acks=all won't be processed and receive 
  an error response if the number of in-sync replicas is below the configured minimum amount.
  
- #### Offline partitions
  - happens when the broker which hosts the elected leader of a partition dies
    
Known issue: 
- If you are operating your cluster in KRaft mode, controllers are currently reported as brokers, and alerts may not function as expected. For more information, see KRaft limitations and known issues. see (https://docs.confluent.io/platform/current/health-plus/health-plus-alerts.html#health-plus-alerts)

Resouces: 
- Master documentation: https://docs.confluent.io/operator/2.8/co-monitor-cp.html
- Monitor Confluent Platform with Health+: https://docs.confluent.io/platform/current/health-plus/index.html
- https://docs.confluent.io/operator/current/co-monitor-cp.html#telemetry-reporter
- How to use OpenTelemetry to Trace and Monitor Apache Kafka Systems: https://www.youtube.com/watch?v=-amJ80DZzv8
- https://www.youtube.com/watch?v=7L1JgkF99LU