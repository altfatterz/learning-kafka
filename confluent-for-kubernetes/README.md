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
$ helm upgrade --install operator confluentinc/confluent-for-kubernetes -n confluent
```

Check that the Confluent For Kubernetes pod comes up and is running:
```bash
$ kubect get pods -n confluent
```

Deploy Confluent platform (Zookeeper / Kafka / Connect):

```bash
$ kubectl apply -f confluent-platform.yaml
```

Deploy Topic: 

```bash
$ kubectl apply -f topic.yaml
```

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

View Data:

```bash
$ kubectl get pvc
$ kubectl get pv 
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