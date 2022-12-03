```bash
# Start a k8s cluster with 1 agent node, 1 server node (control-plane), we disable the loadbalancer in front of the server nodes
$ k3d cluster create my-k8s-cluster --agents 1 --no-lb
# view our k8s cluster 
$ k3d cluster list
# kubectl is automatically will be set to the context
$ kubectl cluster-info
# verify that we have 1 agent nodes and 1 server node
$ kubectl get nodes -o wide
# check with docker that the nodes are running in a docker container
$ docker ps
# taint the server node that no workloads are scheduled on it
$ kubectl taint nodes k3d-my-k8s-cluster-server-0 key1=value1:NoSchedule
```

After creating the cluster automatically we should switch to the created `k3d-my-k8s-cluster` context.

```bash
$ kubectl config get-contexts
CURRENT   NAME                 CLUSTER              AUTHINFO                   NAMESPACE
*         k3d-my-k8s-cluster   k3d-my-k8s-cluster   admin@k3d-my-k8s-cluster
```

Let's create a `kafka` namespaces for our playground and set permanently save the namespace for all subsequent `kubectl`
commands in that context.

```bash
$ kubectl create ns kafka
$ kubectl config set-context --current --namespace=kafka 
```

If we retrieve the current context again the kafka namespace should be set

```bash
$ kubectl config get-contexts
CURRENT   NAME                 CLUSTER              AUTHINFO                   NAMESPACE
*         k3d-my-k8s-cluster   k3d-my-k8s-cluster   admin@k3d-my-k8s-cluster   kafka
```

### Install Keycloak

```bash
$ helm install keycloak codecentric/keycloakx --values ./values.yaml
export POD_NAME=$(kubectl get pods --namespace kafka -l "app.kubernetes.io/name=keycloakx,app.kubernetes.io/instance=keycloak" -o name)
echo "Visit http://127.0.0.1:8080 to use your application"
kubectl --namespace kafka port-forward "$POD_NAME" 8080
```

### Install Strimzi

In this `strimzi.yaml` file the `STRIMZI_FEATURE_GATES` was configured to `-UseStrimziPodSets`

```bash
$ kubectl create -f strimzi.yaml
```


### Create the `broker-oauth-secret` secret

```bash
$ export KAFKA_BROKER_CLIENT_SECRET=jLDzWKJOXQsCttNYjEGOhKcksdtESN58
$ kubectl delete secret broker-oauth-secret
$ kubectl create secret generic broker-oauth-secret --from-literal=secret=$KAFKA_BROKER_CLIENT_SECRET 
```

### Create the `keycloak-auth-secret` secret
```bash
$ echo "Q" | openssl s_client -showcerts -connect lemur-17.cloud-iam.com:443 > /dev/null | awk ' /BEGIN CERTIFICATE/,/END CERTIFICATE/ { print $0 } ' > keycloak.crt
$ kubectl delete secret keycloak-oauth-secret
$ kubectl create secret generic keycloak-oauth-secret --from-file=keycloak.crt
```

### Install a Kafka cluster

```bash
$ kubectl apply -f kafka-oauth-remote-keycloak.yaml
```

### Run an interactive pod:

```bash
$ kubectl run --restart=Never --image=quay.io/strimzi/kafka:0.32.0-kafka-3.3.1 producer-consumer -- /bin/sh -c "sleep 7200"
```

### Copy the configs to the interactive pod:

```bash
$ kubectl cp kafka-producer-security-config.properties producer-consumer:/tmp
$ kubectl cp kafka-consumer-security-config.properties producer-consumer:/tmp
```

### Create the truststore for the kafka clients

```bash
$ kubectl exec -it producer-consumer -- sh 
$ echo "Q" | openssl s_client -showcerts -connect my-cluster-kafka-bootstrap:9093 2> /dev/null | awk ' /BEGIN CERTIFICATE/,/END CERTIFICATE/ { print $0 } ' > /tmp/my-kafka-cluster.crt
$ echo "Q" | openssl s_client -showcerts -connect lemur-17.cloud-iam.com:443 2> /dev/null | awk ' /BEGIN CERTIFICATE/,/END CERTIFICATE/ { print $0 } ' > /tmp/keycloak.crt
$ keytool -keystore /tmp/truststore.p12 -storetype pkcs12 -alias keycloak -storepass storepass -import -file /tmp/keycloak.crt -noprompt
$ keytool -keystore /tmp/truststore.p12 -storetype pkcs12 -alias my-kafka-cluster -storepass storepass -import -file /tmp/my-kafka-cluster.crt -noprompt
$ keytool -list -keystore /tmp/truststore.p12 -storepass storepass 
```

### Run a producer and then a consumer within the interactive pod with security configuration:

```bash
$ kubectl exec -it producer-consumer -- sh
$ /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9093 --topic my-topic \
--producer.config=/tmp/kafka-producer-security-config.properties
```

```bash
$ kubectl exec -it producer-consumer -- sh
$ /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9093 --topic my-topic \
--from-beginning --group my-group --consumer.config=/tmp/kafka-consumer-security-config.properties
```

### Authorization

```bash
$ kubectl apply -f service-account-kafka-consumer.yaml
$ kubectl apply -f service-account-kafka-producer.yaml
```

```bash
$ kubectl get ku
NAME                             CLUSTER      AUTHENTICATION   AUTHORIZATION   READY
service-account-kafka-producer   my-cluster                    simple          True
service-account-kafka-consumer   my-cluster                    simple          True
```

```bash
$ kubectl logs -f my-cluster-entity-operator-6df799fffd-5q9mt -c user-operator
```


### Kafka bridge

### Create the `bridge-oauth-secret` secret

```bash
export KAFKA_BRIDGE_CLIENT_SECRET=snJ8TmaqHxAI3LBxVySX9ioC6gY0ljP3
kubectl delete secret bridge-oauth-secret
kubectl create secret generic bridge-oauth-secret --from-literal=secret=$KAFKA_BRIDGE_CLIENT_SECRET 
```


### Certificate commands

```bash
$ openssl pkcs12 -info -in ca.p12
$ openssl x509 -in /tmp/my-kafka-cluster.crt -text -noout
```

```bash
$ curl -X POST "https://lemur-17.cloud-iam.com/auth/realms/strimzi-keycloak-demo/protocol/openid-connect/token" -H 'Content-Type: application/x-www-form-urlencoded' -d "client_id=kafka-producer&client_secret=ZHgVdg0PC0c4Lh8yXKKInsKD8frhnNuI&grant_type=client_credentials"

{"access_token":"eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJhU0txSFB1UGlLSFB3SUwtX1luUmQ4RDJVU2VxRS1pN3ctWXdsWldFMzRrIn0.eyJleHAiOjE2Njk1MDAxODksImlhdCI6MTY2OTQ5OTg4OSwianRpIjoiMGUzOThiOWEtYmZlMy00OWJlLTkzMTgtMzFjMmM1NDhmZDU0IiwiaXNzIjoiaHR0cHM6Ly9sZW11ci0xNy5jbG91ZC1pYW0uY29tL2F1dGgvcmVhbG1zL3N0cmltemkta2V5Y2xvYWstZGVtbyIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI5OTU0Y2QzMS1jMjk4LTQ5MmYtOGExMS1lZmEyOWVlMTZmMzYiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJrYWZrYS1wcm9kdWNlciIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJkZWZhdWx0LXJvbGVzLXN0cmltemkta2V5Y2xvYWstZGVtbyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiY2xpZW50SG9zdCI6IjE3OC4yMzguMTc1LjE5OSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiY2xpZW50SWQiOiJrYWZrYS1wcm9kdWNlciIsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC1rYWZrYS1wcm9kdWNlciIsImNsaWVudEFkZHJlc3MiOiIxNzguMjM4LjE3NS4xOTkifQ.O5_VWSbEaV_pWinkqrhWNZIS31zEiBxOwOI3YA2jd5QoNkZP2QkOkLK61HcNMbCMK32u4Sq-_Cw6VQ2rbZed3g3A3bRctAI68gn1NCWNoH28XQ-OaRfAu1PwYpLHMhNHaUadHfEwaWGF785Qfd3xomOlx4tEDHxwaaFZuzg4Su5o9nO_KFEYU-cz94LrZBfWqMnhtzscWrRHbxcnqYCSJLa-o0MIsulgRooqPRI5C8X692fhsJmlskjIkm3JYDFhuHfSewo54FTXTIUACzlnHOt13-QuO9xCA41Q5U5TtTK5GEYe6IbVWJwoyLVLmtuJwgs1SfJIyNa6-Pky4BUdpw","expires_in":300,"refresh_expires_in":0,"token_type":"Bearer","not-before-policy":0,"scope":"profile email"} 
```

```json
{
  "exp": 1669500189,
  "iat": 1669499889,
  "jti": "0e398b9a-bfe3-49be-9318-31c2c548fd54",
  "iss": "https://lemur-17.cloud-iam.com/auth/realms/strimzi-keycloak-demo",
  "aud": "account",
  "sub": "9954cd31-c298-492f-8a11-efa29ee16f36",
  "typ": "Bearer",
  "azp": "kafka-producer",
  "acr": "1",
  "realm_access": {
    "roles": [
      "offline_access",
      "default-roles-strimzi-keycloak-demo",
      "uma_authorization"
    ]
  },
  "resource_access": {
    "account": {
      "roles": [
        "manage-account",
        "manage-account-links",
        "view-profile"
      ]
    }
  },
  "scope": "profile email",
  "clientHost": "178.238.175.199",
  "email_verified": false,
  "clientId": "kafka-producer",
  "preferred_username": "service-account-kafka-producer",
  "clientAddress": "178.238.175.199"
}
```


### Authorization

In KeyCloak we have those 3 roles:

- `Realm Role`
- `Client Role`
- `Composite Role`

There are no `User Role` in KeyCloak.
Every `Realm` has one or multiple `Clients`. And every `Client` can have multiple `Users` attached to it.

`Realm Role`: It is a global role, belonging to that specific realm. You can access it from any client and map to any user. 
Ex Role: 'Global Admin, Admin'

`Client Role`: It is a role which belongs only to that specific client. You cannot access that role from a different client. You can only map it to the `Users` from that `client`. Ex Roles: 'Employee, Customer'

`Composite Role`: It is a role that has one or more roles (realm or client ones) associated to it.




Resources:

1. [https://strimzi.io/docs/operators/latest/full/configuring.html#assembly-oauth-authentication_str](https://strimzi.io/docs/operators/latest/full/configuring.html#assembly-oauth-authentication_str)
2. [https://medium.com/keycloak/secure-kafka-with-keycloak-sasl-oauth-bearer-f6230919af74](https://medium.com/keycloak/secure-kafka-with-keycloak-sasl-oauth-bearer-f6230919af74)
3. Certificate decoder [https://www.sslshopper.com/certificate-decoder.html](https://www.sslshopper.com/certificate-decoder.html)
4. [https://www.appsdeveloperblog.com/keycloak-client-credentials-grant-example/](https://www.appsdeveloperblog.com/keycloak-client-credentials-grant-example/)