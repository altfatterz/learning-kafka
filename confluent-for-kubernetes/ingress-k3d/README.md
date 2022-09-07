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


