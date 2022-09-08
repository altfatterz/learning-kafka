#monitoring-demo

Build and Run

```bash
$ mvn clean install
$ java -jar target/*.jar  
```

Test:

```bash
$ http :8080/greeting
$ http :8080/actuator/prometheus
```

```bash
$ docker compose up -d
```

Prometheus: -> http://locahost:9090
Grafana -> http://localhost:3000

```bash
$ docker exec -it monitoring-demo bash
root@e31e269e70b6:/application# curl localhost:8080/greeting
```