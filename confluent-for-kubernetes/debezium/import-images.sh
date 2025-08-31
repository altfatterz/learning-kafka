#!/usr/bin/env bash
docker exec k3d-confluent-server-0 crictl pull confluentinc/cp-server:7.6.1
docker exec k3d-confluent-server-0 crictl pull confluentinc/cp-schema-registry:7.6.1
docker exec k3d-confluent-server-0 crictl pull confluentinc/cp-server-connect:7.6.1
docker exec k3d-confluent-server-0 crictl pull confluentinc/cp-enterprise-control-center:7.6.1
docker exec k3d-confluent-server-0 crictl pull confluentinc/cp-kafka-rest:7.6.1
docker exec k3d-confluent-server-0 crictl pull confluentinc/confluent-init-container:2.8.0
