```bash
$ kubectl apply -f kafka-cruise-control.yaml
```

Wait until the `Entity Operator` is up and running and create a topic:

```bash
$ kubectl apply -f my-topic.yaml
$ k get kt
```

Populate:

```bash
$ kubectl run --restart=Never --image=quay.io/strimzi/kafka:0.32.0-kafka-3.3.1 my-pod -- /bin/sh -c "sleep 14400"
$ kubectl exec -it my-pod -- sh 
$ bin/kafka-producer-perf-test.sh \
--topic my-topic \
--throughput 500 \
--num-records 500000 \
--record-size 1024 \
--producer-props acks=all bootstrap.servers=my-cluster-kafka-bootstrap:9092
```

Check the data:

Let see where are the data located:

```bash
$ kubectl exec -it my-pod /bin/bash -- bin/kafka-log-dirs.sh --describe --bootstrap-server my-cluster-kafka-bootstrap:9092 --broker-list 0,1,2 --topic-list my-topic |  grep '^{' | jq 
```

Add another JBOD disk: (modify the `kafka-cruise-control` file)

```bash
$ kubectl apply -f kafka-cruise-control.yaml
```
Note that the brokers are restarted, one after another (0, 1, 2)


Wait until `Cruise Control` is up and running:

Note that Cruise Control does not perform `inter-broker` and `intra-broker` balancing at the same time.

Create `KafkaRebalance` resource with `rebalanceDisk` set to true to enable intra-broker disk balancing
```bash
$ kubectl apply -f kafka-rebalance-full.yaml
```


Wait until the Kafka brokers are restarted.

Let see where are the data located:

```bash
$ kubectl exec -it my-pod /bin/bash -- bin/kafka-log-dirs.sh --describe --bootstrap-server my-cluster-kafka-bootstrap:9092 --broker-list 0,1,2 --topic-list my-topic |  grep '^{' | jq 
```


```bash
$ kubectl apply -f kafka-rebalance-full.yaml
```

```bash
$ kubectl get kr
NAME           CLUSTER      PENDINGPROPOSAL   PROPOSALREADY   REBALANCING   READY   NOTREADY
my-rebalance   my-cluster                     True
```

`PendingProposal` - means the rebalance operator is polling the Cruise Control API to check if the optimization proposal is ready.
`ProposalReady` - means the optimization proposal is ready for review and approval

```bash
$ kubectl describe kr my-rebalance
```

Refresh the proposal:
```bash
$ kubectl annotate kr my-rebalance strimzi.io/rebalance=refresh
$ kubectl describe kr my-rebalance

Spec:
  Rebalance Disk:  true
Status:
  Conditions:
    Last Transition Time:  2022-11-17T13:20:55.237087Z
    Status:                True
    Type:                  ProposalReady
  Observed Generation:     2
  Optimization Result:
    After Before Load Config Map:  my-rebalance
    Data To Move MB:               0
    Excluded Brokers For Leadership:
    Excluded Brokers For Replica Move:
    Excluded Topics:
    Intra Broker Data To Move MB:         63
    Monitored Partitions Percentage:      100
    Num Intra Broker Replica Movements:   59
    Num Leader Movements:                 0
    Num Replica Movements:                0
    On Demand Balancedness Score After:   62.26415094339623
    On Demand Balancedness Score Before:  62.26415094339623
    Provision Recommendation:
    Provision Status:                     UNDECIDED
    Recent Windows:                       1
  Session Id:                             26bb7122-4ab8-4795-99d3-7e74def82b74
Events:                                   <none>
```

Approve the optimization proposal that you want Cruise Control to apply

```bash
$ kubectl annotate kr my-rebalance strimzi.io/rebalance=approve
```

```bash
$ kubectl get kr my-rebalance
NAME           CLUSTER      PENDINGPROPOSAL   PROPOSALREADY   REBALANCING   READY   NOTREADY
my-rebalance   my-cluster                                     True
```

Check the logs of cluster operator:

```bash
$ kubectl logs -f strimzi-cluster-operator-5647fbfc85-j2q8c
```

```bash
$ kubectl get kr my-rebalance
NAME           CLUSTER      PENDINGPROPOSAL   PROPOSALREADY   REBALANCING   READY   NOTREADY
my-rebalance   my-cluster                                                   True
```

Let see where are the data located:

```bash
$ kubectl exec -it my-pod /bin/bash -- bin/kafka-log-dirs.sh --describe --bootstrap-server my-cluster-kafka-bootstrap:9092 --broker-list 0,1,2 --topic-list my-topic |  grep '^{' | jq 
```

You can see that each of the disks have now partition data.


Once started, a cluster rebalance operation might take some time to complete and affect the overall performance of the Kafka cluster.
If you want to stop a cluster rebalance operation that is in progress, apply the stop annotation to the KafkaRebalance custom resource.
This instructs Cruise Control to finish the current batch of partition reassignments and then stop the rebalance.
```bash
$ kubectl annotate kr my-rebalance strimzi.io/rebalance=stop
```


## Cleanup

```bash
$ kubectl delete -f kafka-cruise-control.yaml
$ kubectl delete pvc `kubectl get pvc -o json | jq -r '.items[].metadata.name'`
$ kubectl delete pv `kubectl get pv -o json | jq -r '.items[].metadata.name'`
$ kubectl delete kt my-topic
$ kubectl delete cm my-rebalance
```



More details here: [https://strimzi.io/docs/operators/latest/full/configuring.html#cruise-control-concepts-str](https://strimzi.io/docs/operators/latest/full/configuring.html#cruise-control-concepts-str)

