```bash
$ k3d cluster create my-k8s-cluster --agents 6
$ kubectl get nodes
NAME                          STATUS   ROLES                  AGE   VERSION
k3d-my-k8s-cluster-server-0   Ready    control-plane,master   13m   v1.24.4+k3s1
k3d-my-k8s-cluster-agent-2    Ready    <none>                 13m   v1.24.4+k3s1
k3d-my-k8s-cluster-agent-1    Ready    <none>                 13m   v1.24.4+k3s1
k3d-my-k8s-cluster-agent-5    Ready    <none>                 13m   v1.24.4+k3s1
k3d-my-k8s-cluster-agent-4    Ready    <none>                 13m   v1.24.4+k3s1
k3d-my-k8s-cluster-agent-3    Ready    <none>                 13m   v1.24.4+k3s1
k3d-my-k8s-cluster-agent-0    Ready    <none>                 13m   v1.24.4+k3s1
```

Install Strimzi:

```bash
$ kubectl create ns kafka
$ kubectl config set-context --current --namespace=kafka
$ kubectl apply -f strimzi.yaml 
```

Taint the master node:
```
$ kubectl taint nodes k3d-my-k8s-cluster-server-0 dedicated=master:NoSchedule
```

Tainting a node means that no pod will be able to schedule onto node1 unless it has a matching toleration.

To remove the taint use:
```bash
$ kubectl taint nodes k3d-my-k8s-cluster-server-0 dedicated=master:NoSchedule-
```

Taint the first 3 nodes:
```bash
$ kubectl taint node k3d-my-k8s-cluster-agent-0 dedicated=Kafka:NoSchedule
$ kubectl taint node k3d-my-k8s-cluster-agent-1 dedicated=Kafka:NoSchedule
$ kubectl taint node k3d-my-k8s-cluster-agent-2 dedicated=Kafka:NoSchedule
```

View the taints:
```bash
$ kubectl get nodes -o=custom-columns=NAME:.metadata.name,TAINTS:.spec.taints
```

Test it out, if you don't add the tolerations the pods will not be scheduled on the 0,1,2 nodes.

Label the first 3 nodes:
```bash
$ kubectl label nodes k3d-my-k8s-cluster-agent-0 dedicated=Kafka
$ kubectl label node k3d-my-k8s-cluster-agent-1 dedicated=Kafka
$ kubectl label node k3d-my-k8s-cluster-agent-2 dedicated=Kafka
```

Show labels:
```bash
$ kubectl get pods --show-labels
```

To remove a label:

```bash
$ kubectl label nodes k3d-my-k8s-cluster-agent-0 dedicated=Kafka-
```

More info:
[https://kubernetes.io/docs/concepts/scheduling-eviction/taint-and-toleration/]


