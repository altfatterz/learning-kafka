
Having Keycloak running in a simple docker container? 
https://www.keycloak.org/getting-started/getting-started-docker

```bash
$ docker run -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:20.0.1 start-dev
```

```bash
$ docker run -p 8443:8443 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:20.0.1 start --optimized --hostname=test.keycloak.org
```

```bash
$ openssl req -subj '/CN=test.keycloak.org/O=Test Keycloak./C=US' -newkey rsa:2048 -nodes -keyout key.pem -x509 -days 365 -out certificate.pem
```

```bash
$ docker run -p 8443:8443 -v $(pwd):/tmp -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin -e KC_DB=dev-mem quay.io/keycloak/keycloak:20.0.1 start --optimized --hostname=test.keycloak.org --https-certificate-file=/tmp/certificate.pem --https-certificate-key-file=/tmp/key.pem
```


------------------------------------------------------------------------------------------------------------------------

```bash
$ openssl req -subj '/CN=test.keycloak.org/O=Test Keycloak./C=US' -newkey rsa:2048 -nodes -keyout server.key.pem -x509 -days 3650 -out server.crt.pem
$ docker run \
  -e KC_HOSTNAME=test.keycloak.org \
  -e KC_DB=dev-mem \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=password \
  -e KC_HTTPS_CERTIFICATE_FILE=/tmp/server.crt.pem \
  -e KC_HTTPS_CERTIFICATE_KEY_FILE=/tmp/server.key.pem \
  -v $(pwd):/tmp \
  -p 8443:8443 \
  quay.io/keycloak/keycloak:20.0.1 start --optimized 
```


error: https://stackoverflow.com/questions/49859066/keycloak-docker-https-required

------------------------------------------------------------------------------------------------------------------------


Configuring TLS: https://www.keycloak.org/server/enabletls


access via http://<minikube-ip>:8080/


another approach
https://gist.github.com/rafi/ec8c584e3a3a3c898903a714c2d62976
