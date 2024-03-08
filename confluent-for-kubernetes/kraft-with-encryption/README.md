# Setup a Kafka cluster in Kraft mode and enable encryption using cert-manager 

### The `tls.key` and `tls.crt` where generated via 

```bash
$ openssl genrsa -out security/tls.key 2048
$ openssl req -x509 -new -nodes -key security/tls.key -sha256 -days 1825 -out security/tls.crt \
  -subj '/CN=ca1.mimacom.com/OU=development/O=mimacom/L=Zurich/C=CH' 
```

### View certificate:

```bash
$ openssl x509 -in security/tls.crt -text -noout
```

### Create k8s cluster and namespace

```bash
$ k3d cluster create confluent
$ kubectl cluster-info
$ kubectl create ns confluent
```

### Set the `confluent` namespace current
```bash  
$ kubectl config set-context --current --namespace confluent
```

### Install `cert-manager`

```bash
$ kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.2/cert-manager.yaml
```

### Check the running pods for `cert-manager`

```bash
$ kubectl get pods -n cert-manager

NAME                                      READY   STATUS    RESTARTS   AGE
cert-manager-cainjector-9795f664f-x6p88   1/1     Running   0          27s
cert-manager-594b84b49d-mdjdv             1/1     Running   0          27s
cert-manager-webhook-64d9455f47-b5rld     1/1     Running   0          27s
```

### Check the new CRDs

```bash
$ kubectl get crds | grep cert-manager
```

Cert Manager supports different modes for certificate authorities:
- Using a CA Issuer - https://cert-manager.io/docs/configuration/ca/
- Using a Self-Signed Issuer - https://cert-manager.io/docs/configuration/selfsigned/
- Using a Let's Encrypt Issuer - https://cert-manager.io/docs/configuration/acme/
- Using Vault - https://cert-manager.io/docs/configuration/vault/
- Using Venafi https://cert-manager.io/docs/configuration/venafi/

### Create the certificates with kustomize

```bash
$ kubectl apply --dry-run=client -k `pwd`/security -o yaml 
$ kubectl apply -k  `pwd`/security

secret/ca-key-pair created
certificate.cert-manager.io/ca-c3-cert created
certificate.cert-manager.io/ca-controller-cert created
certificate.cert-manager.io/ca-kafka-cert created
issuer.cert-manager.io/ca-issuer created
```

### Verify the created certificates / issuers

```bash
$ kubectl get certificates

NAME                 READY   SECRET              AGE
ca-controller-cert   True    controller-tls      26s
ca-c3-cert           True    controlcenter-tls   26s
ca-kafka-cert        True    kafka-tls           26s
```

```bash
$ kubectl get issuer

NAME        READY   AGE
ca-issuer   True    55s
```

### View the created secrets

```bash
$ kubectl get secret

NAME                TYPE                DATA   AGE
ca-key-pair         kubernetes.io/tls   2      77s
controller-tls      kubernetes.io/tls   3      77s
controlcenter-tls   kubernetes.io/tls   3      77s
kafka-tls           kubernetes.io/tls   3      75s
```

TODO -- from here 

```bash
$ kubectl get secret ca-key-pair -o jsonpath='{.data.ca\.crt}' | base64 -d > ca.crt
$ kubectl get secret kafka-tls -o jsonpath='{.data.tls\.crt}' | base64 -d > tls.crt
$ kubectl get secret kafka-tls -o jsonpath='{.data.tls\.key}' | base64 -d > tls.key
$ openssl x509 -in ca.crt -text -noout
$ openssl x509 -in tls.crt -text -noout
$ openssl x509 -in tls.key -text -noout
```

Better inspect with the [cmctl](https://cert-manager.io/docs/reference/cmctl/#renew tool
```bash
$ brew install cmctl
$ cmctl inspect secret kafka-tls
```







### Set up the Helm Chart:
```bash
$ helm repo add confluentinc https://packages.confluent.io/helm
$ helm repo update
```

Install Confluent For Kubernetes using Helm:
```bash
$ helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes --set kRaftEnabled=true
```

```bash
$ kubectl get crds | grep confluent

clusterlinks.platform.confluent.io            2024-02-12T20:03:58Z
confluentrolebindings.platform.confluent.io   2024-02-12T20:03:58Z
connectors.platform.confluent.io              2024-02-12T20:03:58Z
connects.platform.confluent.io                2024-02-12T20:03:58Z
controlcenters.platform.confluent.io          2024-02-12T20:03:58Z
kafkarestclasses.platform.confluent.io        2024-02-12T20:03:58Z
kafkarestproxies.platform.confluent.io        2024-02-12T20:03:58Z
kafkas.platform.confluent.io                  2024-02-12T20:03:58Z
kafkatopics.platform.confluent.io             2024-02-12T20:03:58Z
kraftcontrollers.platform.confluent.io        2024-02-12T20:03:58Z
ksqldbs.platform.confluent.io                 2024-02-12T20:03:58Z
schemaexporters.platform.confluent.io         2024-02-12T20:03:58Z
schemaregistries.platform.confluent.io        2024-02-12T20:03:59Z
schemas.platform.confluent.io                 2024-02-12T20:03:59Z
zookeepers.platform.confluent.io              2024-02-12T20:03:59Z
```

Check that the Confluent For Kubernetes operator pod comes up and is running:

```bash
$ kubectl get pods -n confluent

NAME                                  READY   STATUS    RESTARTS   AGE
confluent-operator-6c7bb75484-k294m   1/1     Running   0          22s
```

Deploy a 3 Controller and 3 broker cluster

```bash
$ kubectl apply -f kraft/kraft-broker-controller.yaml
```

Produce and consume from the topics:

```bash
$ kubectl exec -it kafka-0 -- bash
$ seq 5 | kafka-console-producer --topic demotopic --bootstrap-server kafka.confluent.svc.cluster.local:9092
$ kafka-console-consumer --from-beginning --topic demotopic --bootstrap-server  kafka.confluent.svc.cluster.local:9092
1
2
3
4
5
```

Install and `port-forward` control center

```bash
$ kubectl apply -f kraft/control-center.yaml
$ kubectl port-forward controlcenter-0 9021:9021
```

```bash
$ kubectl confluent dashboard controlcenter
```

Install the sample producer app and topic.

```bash
$ kubectl apply -f kraft/producer-app-data.yaml
```

Check the logs for the created demo and view the Controll Center demo how the messages are flowing in

```bash
$ kubectl logs -f elastic-0
```

Cleanup

```
$ kubectl delete -f kraft/producer-app-data.yaml
$ kubectl delete -f kraft/control-center.yaml
$ kubectl delete -f kraft/kraft-broker-controller.yaml

$ helm uninstall confluent-operator
$ kubectl delete namespace confluent

$ k3d cluster delete confluent
```


------------------------------------------------------------------------------------------------------------------------



Re-issuance triggered by user actions. By default, the private key won't be rotated automatically.
```bash
$ cmctl renew ca-kafka-cert
```

Using the setting `rotationPolicy: Always`, the private key Secret associated with a Certificate
object can be configured to be rotated as soon as an the Certificate is reissued


[wave](https://github.com/wave-k8s/wave) is a Secret controller that makes sure deployments get restarted
whenever a mounted Secret changes.

------------------------------------------------------------------------------------------------------------------------
