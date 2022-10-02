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
# Create a resource group
$ az group create --name $RG --location $LOCATION
# Create a basic AKS cluster 
$ az aks create \
    --resource-group $RG \
    --name $AKS_CLUSTER_NAME \
    --node-count 2 \
    --generate-ssh-keys
# View created cluster   
$ az aks list -o table    
# Configure Credentials
$ az aks get-credentials -g $RG --name $AKS_CLUSTER_NAME
$ k get nodes -o wide
```

```bash
# Create kafka namespace
$ kubectl create ns kafka
# Install strimzi operator
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

```bash
$ kubectl apply -f kafka-ephemeral-single.yaml
```

View the cluster:
```bash
$ kubectl get pods -n kafka
$ kubectl get svc -n kafka
$ kubectl get cm -n kafka
$ kubectl get secret -n kafka
```

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
$ kafka-console-producer --bootstrap-server 20.91.172.51:9094 --topic my-topic
$ kafka-console-consumer --bootstrap-server 20.91.172.51:9094 --topic my-topic --from-beginning
```
