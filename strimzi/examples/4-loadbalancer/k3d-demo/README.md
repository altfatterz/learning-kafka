```bash
$ k3d cluster create mycluster -p "8080:8080@agent:0,1,2" --agents 3
$ watch kubectl get all --all-namespaces
$ kubectl apply -f demo-app.yaml
$ curl $(minikube ip):8080
```