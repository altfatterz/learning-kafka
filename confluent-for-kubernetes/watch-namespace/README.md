### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent
$ kubectl cluster-info
$ kubectl get nodes
NAME                     STATUS   ROLES                  AGE   VERSION
k3d-confluent-server-0   Ready    control-plane,master   73s   v1.27.4+k3s1
$ kubectl create ns confluent-operator
$ kubectl create ns dev
$ kubectl create ns staging
$ kubectl create ns prod
$ kubectl config current-context
k3d-confluent
```

### Deploy CFK from Confluent’s Helm repo

Downloaded the `confluent-operator-values.yaml` is from `confluent-for-kubernetes-2.8.2` and modified:

```bash
kRaftEnabled: true
namespaceList: [dev, staging, prod]
```

```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes \
--values confluent-operator-values.yaml \
--namespace confluent-operator 
$ helm list --namespace confluent-operator
$ kubectl get pods --all-namespaces
```

### Deploy to Confluent Platform to dev

View the end result:

```bash
$ kubectl apply -k overlays/dev --dry-run=client -o yaml 
```

Deploy to dev

```bash
$ kubectl apply -k overlays/dev 
```

### Deploy to Confluent Platform to staging

View the end result:

```bash
$ kubectl apply -k overlays/staging --dry-run=client -o yaml 
```

Deploy to dev

```bash
$ kubectl apply -k overlays/staging
```

### Deploy to Confluent Platform to prod

View the end result:

```bash
$ kubectl apply -k overlays/prod --dry-run=client -o yaml 
```

Deploy to dev

```bash
$ kubectl apply -k overlays/prod
```

### Verify pods:

```bash
$ kubectl get pods --all-namespaces
```

