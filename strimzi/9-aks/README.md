Install Azure CLI using [https://learn.microsoft.com/en-us/cli/azure/install-azure-cli](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli)

```bash
$ az login
$ az account show
$ az account list
```

```bash
export RG=aks-strimzi
export LOCATION=swedencentral
export AKS_CLUSTER_NAME=aks-kafka-strimzi
export NODE_TYPE=Standard_B2s

# Create a resource group
$ az group create --name $RG --location $LOCATION

# Generate SSH keys
$ ssh-keygen -t rsa -b 4096 -f ~/.ssh/aks_ssh_key 

# Create a basic AKS cluster
$ az aks create \
    --resource-group $RG \
    --name $AKS_CLUSTER_NAME \
    --node-count 3 \
    --node-vm-size $NODE_TYPE \
    --zones 1 2 3 \
    --ssh-key-value ~/.ssh/aks_ssh_key.pub
    
# View created cluster   
$ az aks list -o table   

Name               Location       ResourceGroup    KubernetesVersion    CurrentKubernetesVersion    ProvisioningState    Fqdn
-----------------  -------------  ---------------  -------------------  --------------------------  -------------------  ------------------------------------------------------------------
aks-kafka-strimzi  swedencentral  aks-strimzi      1.32                 1.32.7                      Succeeded            aks-kafka--aks-strimzi-7ac273-vspd61ri.hcp.swedencentral.azmk8s.io 

# Configure Credentials
$ az aks get-credentials -g $RG --name $AKS_CLUSTER_NAME

$ kubectl get nodes -o wide
NAME                                STATUS   ROLES    AGE     VERSION   INTERNAL-IP   EXTERNAL-IP   OS-IMAGE             KERNEL-VERSION      CONTAINER-RUNTIME
aks-nodepool1-12807488-vmss000000   Ready    <none>   2m45s   v1.32.7   10.224.0.6    <none>        Ubuntu 22.04.5 LTS   5.15.0-1092-azure   containerd://1.7.28-1
aks-nodepool1-12807488-vmss000001   Ready    <none>   2m37s   v1.32.7   10.224.0.5    <none>        Ubuntu 22.04.5 LTS   5.15.0-1092-azure   containerd://1.7.28-1
aks-nodepool1-12807488-vmss000002   Ready    <none>   2m34s   v1.32.7   10.224.0.4    <none>        Ubuntu 22.04.5 LTS   5.15.0-1092-azure   containerd://1.7.28-1

# nodes are in different zones
$ kubectl get nodes -o custom-columns=NAME:.metadata.name,ZONE:.metadata.labels.topology\\.kubernetes\\.io/zone
NAME                                ZONE
aks-nodepool1-12807488-vmss000000   swedencentral-1
aks-nodepool1-12807488-vmss000001   swedencentral-2
aks-nodepool1-12807488-vmss000002   swedencentral-3

$ label nodes

$ k label node aks-nodepool1-12807488-vmss000000 app=kafka
$ k label node aks-nodepool1-12807488-vmss000001 app=kafka
$ k label node aks-nodepool1-12807488-vmss000002 app=kafka

```

```bash
# Create kafka namespace
kubectl create namespace strimzi-operator  
kubectl create namespace kafka  

# Install strimzi cluster operator
$ helm install strimzi-cluster-operator oci://quay.io/strimzi-helm/strimzi-kafka-operator \
--namespace strimzi-operator \
--values strimzi-operator-values.yaml

# Verify the installed strimzi operator
$ kubectl get pods -n strimzi-operator -o wide
NAME                                       READY   STATUS    RESTARTS   AGE   IP             NODE                                NOMINATED NODE   READINESS GATES
strimzi-cluster-operator-c8ccccb66-jdw78   1/1     Running   0          51s   10.244.1.207   aks-nodepool1-12807488-vmss000001   <none>           <none>
strimzi-cluster-operator-c8ccccb66-m4tgz   1/1     Running   0          51s   10.244.2.55    aks-nodepool1-12807488-vmss000002   <none>           <none>
strimzi-cluster-operator-c8ccccb66-w8t7s   1/1     Running   0          51s   10.244.0.47    aks-nodepool1-12807488-vmss000000   <none>           <none>

$ kubectl get cm -n strimzi-operator

# check storageclass
$ kubectl get sc

$ kubectl apply -f kafka.yaml

```

View the cluster:

```bash
$ kubectl get pods -n kafka
NAME                                                 READY   STATUS    RESTARTS   AGE   IP             NODE                                NOMINATED NODE   READINESS GATES
kafka-aks-cluster-broker-0                           1/1     Running   0          85s   10.244.2.165   aks-nodepool1-12807488-vmss000002   <none>           <none>
kafka-aks-cluster-broker-1                           1/1     Running   0          85s   10.244.0.241   aks-nodepool1-12807488-vmss000000   <none>           <none>
kafka-aks-cluster-broker-2                           1/1     Running   0          85s   10.244.1.240   aks-nodepool1-12807488-vmss000001   <none>           <none>
kafka-aks-cluster-controller-3                       1/1     Running   0          85s   10.244.2.26    aks-nodepool1-12807488-vmss000002   <none>           <none>
kafka-aks-cluster-controller-4                       1/1     Running   0          84s   10.244.0.35    aks-nodepool1-12807488-vmss000000   <none>           <none>
kafka-aks-cluster-controller-5                       1/1     Running   0          84s   10.244.1.4     aks-nodepool1-12807488-vmss000001   <none>           <none>
kafka-aks-cluster-entity-operator-56864b86bc-n2wjj   2/2     Running   0          22s   10.244.2.209   aks-nodepool1-12807488-vmss000002   <none>           <none>

$ kubectl get pvc -n kafka

NAME                                    STATUS   VOLUME                                     CAPACITY   ACCESS MODES   STORAGECLASS   VOLUMEATTRIBUTESCLASS   AGE
data-0-kafka-aks-cluster-broker-0       Bound    pvc-ef1796f9-6f50-4ceb-82a0-c2fac36cfbba   5Gi        RWO            default        <unset>                 8m51s
data-0-kafka-aks-cluster-broker-1       Bound    pvc-d4c2b19b-4768-4096-9184-a4d69605c3f3   5Gi        RWO            default        <unset>                 8m51s
data-0-kafka-aks-cluster-broker-2       Bound    pvc-37715552-6297-4783-9491-68c84277dbe9   5Gi        RWO            default        <unset>                 8m51s
data-0-kafka-aks-cluster-controller-3   Bound    pvc-54b25856-b171-4022-b5f4-eb454635dc2c   1Gi        RWO            default        <unset>                 8m51s
data-0-kafka-aks-cluster-controller-4   Bound    pvc-1df51e3a-141f-4181-bb61-94fc9fd228db   1Gi        RWO            default        <unset>                 8m51s
data-0-kafka-aks-cluster-controller-5   Bound    pvc-669335fe-15ef-43c3-b849-4909e02125de   1Gi        RWO            default        <unset>                 8m51s
data-1-kafka-aks-cluster-broker-0       Bound    pvc-681f0e15-9887-4127-9693-d76e1371bf19   5Gi        RWO            default        <unset>                 8m51s
data-1-kafka-aks-cluster-broker-1       Bound    pvc-6e1e5173-fa2a-4cb2-a64d-e4a1f8bcc0cf   5Gi        RWO            default        <unset>                 8m51s
data-1-kafka-aks-cluster-broker-2       Bound    pvc-72f0bcbc-5938-470e-84e2-851ecafc8e76   5Gi        RWO            default        <unset>                 8m51s


$ kubectl get svc -n kafka
NAME                                TYPE        CLUSTER-IP    EXTERNAL-IP   PORT(S)                               AGE
kafka-aks-cluster-kafka-bootstrap   ClusterIP   10.0.54.173   <none>        9091/TCP,9092/TCP                     28s
kafka-aks-cluster-kafka-brokers     ClusterIP   None          <none>        9090/TCP,9091/TCP,8443/TCP,9092/TCP   28s

$ kubectl get secret -n kafka
```

TODO:

Producer: (topic will be automatically created)
```bash
$ kubectl -n kafka run kafka-producer -ti --image=quay.io/strimzi/kafka:0.31.1-kafka-3.2.3 --rm=true --restart=Never -- bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic
```

Consumer:
```bash
$ kubectl -n kafka run kafka-consumer -ti --image=quay.io/strimzi/kafka:0.31.1-kafka-3.2.3 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic my-topic --from-beginning
```

Peek into the configs:
```bash
$ kubectl get cm my-cluster-kafka-0 -n kafka -o yaml
```

External access: 
```bash
$ kubectl apply -f kafka-loadbalancer.yaml -n kafka
$ # get the external ip from the `<cluster-name>-kafka-external-bootstrap` service
$ kafka-topics --bootstrap-server <external-ip>:9094 --list 
$ kafka-console-producer --bootstrap-server <external-ip>:9094 --topic my-topic
$ kafka-console-consumer --bootstrap-server <external-ip>:9094 --topic my-topic --from-beginning
```


### Cleanup 

```bash
$ az group delete -g $RG
```