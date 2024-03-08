------------------------------------------------------------------------------------------------------------------------
------- older version - to be updated ----------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------------------------

Deploy Connector:

```bash
$ kubectl apply -f connector.yaml
```

Install Confluent kubectl plugin: (https://docs.confluent.io/operator/current/co-quickstart.html#bonus-install-and-use-confluent-kubectl-plugin)

```bash
$ cd ~/temp
$ curl -O https://confluent-for-kubernetes.s3-us-west-1.amazonaws.com/confluent-for-kubernetes-2.8.0.tar.gz
$ tar -xvf confluent-for-kubernetes-2.8.0.tar.gz
$ cd confluent-for-kubernetes-2.8.0.tar.gz
$ tar -xvf confluent-for-kubernetes-2.8.0.tar.gz -C /usr/local/bin/
```

Test if it works:

```bash
$ kubectl confluent version
COMPONENT  NAME       VERSION  OPERATOR-VERSION
Zookeeper  zookeeper  7.2.0    v0.517.23
Kafka      kafka      7.2.0    v0.517.23
Connect    connect    7.2.0    v0.517.23
```

```bash
$ kubectl confluent status
COMPONENT  NAME       READY  STATUS   AGE
Zookeeper  zookeeper  3/3    RUNNING  16m
Kafka      kafka      3/3    RUNNING  16m
Connect    connect    1/1    RUNNING  16m
```

```bash
$ kubectl get topic
NAME        REPLICAS   PARTITION   STATUS    CLUSTERID                AGE
pageviews   3          1           CREATED   44aB1vJITCaMdP25EeqB2A   11m
```

```bash
$ kubectl get connector
NAME        STATUS    CONNECTORSTATUS   TASKS-READY   AGE
pageviews   CREATED   RUNNING           4/4           38s
```

Control Center

```bash
$ kubectl port-forward pod/controlcenter-0 9021:9021
```
or

```bash
$ kubectl confluent dashboard controlcenter
```

Consumer

```bash
$ kubectl exec kafka-0 -it -- bash 
$ kafka-console-consumer --from-beginning --topic pageviews --bootstrap-server  kafka.confluent.svc.cluster.local:9071
```


Notice that for the volumes the RECLAIM_POLICY is 'Delete', this is not a production setyp.



Tear down:
```bash
$ kubectl delete -f connector.yaml
$ kubectl delete -f topic.yaml
$ kubectl delete -f confluent-platform.yaml
```





Resources:
1. https://docs.confluent.io/operator/current/co-quickstart.html
2. cp-demo: https://docs.confluent.io/platform/current/tutorials/cp-demo/docs/index.html
3. jmx-monitoring-stacks: https://github.com/confluentinc/jmx-monitoring-stacks
4. 