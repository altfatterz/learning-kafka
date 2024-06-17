----------------------- not working yet---------------------------------------------------------------------------------

```bash
$ k3d cluster create confluent --api-port 6550 -p "8081:80@loadbalancer" --agents 2
$ kubectl get ingressclass
NAME      CONTROLLER                      PARAMETERS   AGE
traefik   traefik.io/ingress-controller   <none>       50m
# check all pods running
$ kubeclt get pods --all-namespaces
$ kubectl get nodes
NAME                     STATUS   ROLES                  AGE   VERSION
k3d-confluent-agent-1    Ready    <none>                 11s   v1.28.8+k3s1
k3d-confluent-server-0   Ready    control-plane,master   15s   v1.28.8+k3s1
k3d-confluent-agent-0    Ready    <none>                 11s   v1.28.8+k3s1
$ kubectl create ns confluent
$ kubectl config set-context --current --namespace confluent
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes --set kRaftEnabled=true
$ kubectl apply -f confluent-platform.yaml
```

Check services:

```bash
$ kubectl get svc
NAME                         TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                                                                   AGE
confluent-operator           ClusterIP   10.43.208.207   <none>        7778/TCP                                                                  13m
kraftcontroller              ClusterIP   None            <none>        9074/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP                              12m
kraftcontroller-0-internal   ClusterIP   10.43.132.217   <none>        9074/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP                              12m
kraftcontroller-1-internal   ClusterIP   10.43.206.150   <none>        9074/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP                              12m
kraftcontroller-2-internal   ClusterIP   10.43.169.117   <none>        9074/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP                              12m
kafka                        ClusterIP   None            <none>        9074/TCP,9092/TCP,8090/TCP,9071/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP   4m54s
kafka-0-internal             ClusterIP   10.43.78.128    <none>        9074/TCP,9092/TCP,8090/TCP,9071/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP   4m54s
kafka-1-internal             ClusterIP   10.43.254.206   <none>        9074/TCP,9092/TCP,8090/TCP,9071/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP   4m53s
kafka-2-internal             ClusterIP   10.43.130.121   <none>        9074/TCP,9092/TCP,8090/TCP,9071/TCP,7203/TCP,7777/TCP,7778/TCP,9072/TCP   4m53s
connect                      ClusterIP   None            <none>        8083/TCP,7203/TCP,7777/TCP,7778/TCP                                       12m
connect-0-internal           ClusterIP   10.43.216.245   <none>        8083/TCP,7203/TCP,7777/TCP,7778/TCP                                       12m
controlcenter                ClusterIP   None            <none>        9021/TCP,7203/TCP,7777/TCP,7778/TCP                                       24s
schemaregistry               ClusterIP   None            <none>        8081/TCP,7203/TCP,7777/TCP,7778/TCP                                       24s
schemaregistry-0-internal    ClusterIP   10.43.241.55    <none>        8081/TCP,7203/TCP,7777/TCP,7778/TCP                                       24s
controlcenter-0-internal     ClusterIP   10.43.12.175    <none>        9021/TCP,7203/TCP,7777/TCP,7778/TCP                                       24s
```





```bash
$ k3d cluster create my-k8s-cluster --api-port 6550 -p "8081:80@loadbalancer" --agents 2
$ kubectl create deployment nginx --image=nginx
$ kubectl create service clusterip nginx --tcp=80:80
$ kubectl apply -f ingress.yaml
```

```bash
$ curl localhost:8081/
```

Cleanup

```bash
$ k3d cluster delete my-k8s-cluster
```

More details here: https://k3d.io/v5.4.4/usage/exposing_services/


