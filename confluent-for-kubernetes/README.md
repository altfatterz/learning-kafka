Create k8s cluster and namespace

```bash
$ k3d cluster create confluent
$ kubectl cluster-info
$ kubectl create ns confluent
```

Set the `confluent` namespace current
```bash  
$ kubectl config set-context --current --namespace confluent
```

Set up the Helm Chart:
```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
```

Install Confluent For Kubernetes using Helm:
```bash
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes --set kRaftEnabled=true
```

```bash
$ kubectl get crds | grep confluent

clusterlinks.platform.confluent.io            2024-02-12T20:03:58Z
confluentrolebindings.platform.confluent.io   2024-02-12T20:03:58Z
connectors.platform.confluent.io              2024-02-12T20:03:58Z
connects.platform.confluent.io                2024-02-12T20:03:58Z
controlcenters.platform.confluent.io          2024-02-12T20:03:58Z
kafkarestclasses.platform.confluent.io        2024-02-12T20:03:58Z
kafkarestproxies.platform.confluent.io        2024-02-12T20:03:58Z
kafkas.platform.confluent.io                  2024-02-12T20:03:58Z
kafkatopics.platform.confluent.io             2024-02-12T20:03:58Z
kraftcontrollers.platform.confluent.io        2024-02-12T20:03:58Z
ksqldbs.platform.confluent.io                 2024-02-12T20:03:58Z
schemaexporters.platform.confluent.io         2024-02-12T20:03:58Z
schemaregistries.platform.confluent.io        2024-02-12T20:03:59Z
schemas.platform.confluent.io                 2024-02-12T20:03:59Z
zookeepers.platform.confluent.io              2024-02-12T20:03:59Z
```

Check that the Confluent For Kubernetes operator pod comes up and is running:

```bash
$ kubectl get pods -n confluent

NAME                                  READY   STATUS    RESTARTS   AGE
confluent-operator-6c7bb75484-k294m   1/1     Running   0          22s
```

Deploy a 3 Controller and 3 broker cluster

```bash
$ kubectl apply -f kraft/kraft-broker-controller.yaml
```

Produce and consume from the topics:

```bash
$ kubectl exec -it kafka-0 -- bash
$ seq 5 | kafka-console-producer --topic demotopic --bootstrap-server kafka.confluent.svc.cluster.local:9092
$ kafka-console-consumer --from-beginning --topic demotopic --bootstrap-server  kafka.confluent.svc.cluster.local:9092
1
2
3
4
5
```

Install and `port-forward` control center

```bash
$ kubectl apply -f kraft/control-center.yaml
$ kubectl port-forward controlcenter-0 9021:9021
```

Install the sample producer app and topic.

```bash
$ kubectl apply -f kraft/producer-app-data.yaml
```

Check the logs for the created demo and view the Controll Center demo how the messages are flowing in

```bash
$ kubectl logs -f elastic-0
```

Cleanup

```
$ kubectl delete -f kraft/producer-app-data.yaml
$ kubectl delete -f kraft/control-center.yaml
$ kubectl delete -f kraft/kraft-broker-controller.yaml

$ helm uninstall confluent-operator
$ kubectl delete namespace confluent

$ k3d cluster delete confluent
```








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
$ curl -O https://confluent-for-kubernetes.s3-us-west-1.amazonaws.com/confluent-for-kubernetes-2.4.1.tar.gz
$ tar -xvf confluent-for-kubernetes-2.4.1.tar.gz
$ cd confluent-for-kubernetes-2.4.1-20220801
$ tar -xvf kubectl-plugin/kubectl-confluent-darwin-amd64.tar.gz -C /usr/local/bin/
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