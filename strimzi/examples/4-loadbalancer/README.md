!!!! demo does not work yet !!!

```bash
$ k3d cluster create mycluster -p "9096:80@agent:0,1" --agents 2
$ k3d cluster create mycluster --agents 2 --no-lb --k3s-arg "--disable=traefik@server:0" 
$ kubectl taint nodes k3d-mycluster-server-0 key1=value1:NoSchedule
```

In a separate terminal monitor the resources using:

```bash
$ kubectl get all --all-namespaces
```

### 3. Deploy [metallb](https://metallb.universe.tf/installation/)

```bash
$ kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.10.2/manifests/namespace.yaml
$ kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.10.2/manifests/metallb.yaml
```

```bash
$ kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.13.5/config/manifests/metallb-native.yaml
```


Copy the following to calculate the ingress address range:

```
cluster_name=`docker network list --format "{{ .Name}}" | grep k3d`
cidr_block=$(docker network inspect $cluster_name | jq '.[0].IPAM.Config[0].Subnet' | tr -d '"')
cidr_base_addr=${cidr_block%???}
ingress_first_addr=$(echo $cidr_base_addr | awk -F'.' '{print $1,$2,255,0}' OFS='.')
ingress_last_addr=$(echo $cidr_base_addr | awk -F'.' '{print $1,$2,255,255}' OFS='.')
ingress_range=$ingress_first_addr-$ingress_last_addr
echo $ingress_range
```

### 4. Configure metallb ingress address range

```bash
$ kubectl apply -f config.yaml
```



```bash
$ kubectl get pods --all-namespaces

kube-system      local-path-provisioner-7b7dc8d6f5-7czl8   1/1     Running   0          117s
kube-system      coredns-b96499967-xpzn8                   1/1     Running   0          117s
kube-system      metrics-server-668d979685-586wd           1/1     Running   0          117s
metallb-system   controller-5ddcfdc9fc-cwh7d               1/1     Running   0          34s
metallb-system   speaker-6shzw                             1/1     Running   0          34s
metallb-system   speaker-8gk2d                             1/1     Running   0          34s
metallb-system   speaker-zjrwp                             1/1     Running   0          34s
metallb-system   speaker-slk9b                             1/1     Running   0          34s
```

### 3. Install the Strimzi operator

```bash
$ kubectl create ns kafka
$ kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
```

### 5. Deploy Kafka with Loadbalancer config:

```bash
$ kubectl apply -f kafka-loadbalancer.yaml -n kafka
```


```bash
$ kubectl run -it --rm --restart=Never dnsutils2 --image=tutum/dnsutils  --command -- bash
$ nslookup my-cluster-kafka-brokers.kafka.svc.cluster.local
Server:		10.43.0.10
Address:	10.43.0.10#53

Name:	my-cluster-kafka-brokers.kafka.svc.cluster.local
Address: 10.42.0.4
Name:	my-cluster-kafka-brokers.kafka.svc.cluster.local
Address: 10.42.2.6
Name:	my-cluster-kafka-brokers.kafka.svc.cluster.local
Address: 10.42.2.5

$ nslookup my-cluster-kafka-bootstrap.kafka.svc.cluster.local
Server:		10.43.0.10
Address:	10.43.0.10#53

Name:	my-cluster-kafka-bootstrap.kafka.svc.cluster.local
Address: 10.43.38.124
```



Reference
1. [https://github.com/keunlee/k3d-metallb-starter-kit](https://github.com/keunlee/k3d-metallb-starter-kit)