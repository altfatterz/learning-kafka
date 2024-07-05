### Create k8s cluster and namespace

```bash
$ k3d cluster create k8s-cluster
$ kubectl cluster-info
$ kubectl get nodes
```

### Install Argo CD

```bash
$ kubectl create namespace argocd
$ kubectl config set-context --current --namespace argocd
$ kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

### Install Argo CD CLI

```bash
$ brew install argocd
```

### Access the Argo CD API Server

```bash
$ kubectl port-forward svc/argocd-server 8080:443
```

Access https://localhost:8080

### ArgoCD Swagger UI

Access https://localhost:8080/swagger-ui



### Login Using the CLI

```bash
$ argocd admin initial-password -n argocd
$ argocd login localhost:8080
```

The secret `argocd-initial-admin-secret` contains the initial password


### ArgoCD CLI

```bash
$ argocd app list
$ argocd cluster list

```

### Create an application from GIT

```bash
$ argocd app create guestbook \
--repo https://github.com/altfatterz/learning-kafka.git \
--path confluent-for-kubernetes/argocd \
--dest-server https://kubernetes.default.svc \
--dest-namespace default
```

### Sync (deploy) the application

```bash
$ argocd app get guestbook
$ argocd app sync guestbook --dry-run
$ argocd app sync guestbook
```

### Check the application

```bash
$ kubectl get all -n default
$ kubectl port-forward service/guestbook-ui 8081:80 -n default 
```

Access http://localhost:8081