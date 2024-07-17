#!/usr/bin/env bash
k3d images import confluentinc/cp-server:7.6.1 -c confluent
k3d images import confluentinc/cp-schema-registry:7.6.1 -c confluent
k3d images import confluentinc/cp-server-connect:7.6.1 -c confluent
k3d images import confluentinc/cp-enterprise-control-center:7.6.1 -c confluent
k3d images import confluentinc/cp-kafka-rest:7.6.1 -c confluent
k3d images import confluentinc/confluent-init-container:2.8.0 -c confluent
