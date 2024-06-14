#monitoring-demo

Build and Run

```bash
$ mvn spring-boot:build-image
$ java -jar target/*.jar  
```

The docker image will be done via buildpack.

Test:

```bash
$ http :8080/greeting
$ http :8080/actuator/metrics
$ http :8080/actuator/prometheus
```

```bash
$ docker compose up -d
```

### Prometheus: 

http://locahost:9090

Target: http://localhost:9090/targets

You should see `monitoring-demo` with status `up` and endpoint http://monitoring-demo:8080/actuator/prometheus

Search: http://localhost:9090/graph

### Grafana 

http://localhost:3000

Add the `prometheus` datasource with http://localhost:9090

References:
1. https://grafana.com/blog/2022/04/26/set-up-and-observe-a-spring-boot-application-with-grafana-cloud-prometheus-and-opentelemetry/#intro-to-the-spring-boot-application