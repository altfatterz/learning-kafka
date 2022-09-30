With Docker Desktop you need to set the following: (revert to use cgroupv1 instead of cgroupv2 to work with systemd)
Set the `"deprecatedCgroupv1": true` in the `~/Library/Group\ Containers/group.com.docker/settings.json`

```bash
$ docker build --rm -t localbuild/ubi8 .
```

```bash
$ docker compose up -d
```

```bash
$ ansible-galaxy collection install confluent.platform:7.2.1
```

```bash
$ ansible-playbook -i hosts.yml confluent.platform.all --tags=zookeeper,kafka_broker
```

```bash
$ zookeeper-shell localhost:2181 ls /brokers/ids
$ kafka-topics --bootstrap-server localhost:9092 --list  
```

```bash
$ docker compose down -v
```

Problems:
1. after `docker compose down` the zookeeper and kafka are not started
2. accessing the cluster from outside? 

Resources

1. [https://docs.confluent.io/ansible/current/ansible-configure.html](https://docs.confluent.io/ansible/current/ansible-configure.html)
2. [https://github.com/docker/for-mac/issues/6073](https://github.com/docker/for-mac/issues/6073)
3. [https://www.cyberithub.com/solved-failed-to-download-metadata-for-repo-appstream/](https://www.cyberithub.com/solved-failed-to-download-metadata-for-repo-appstream/)
4 [https://www.cyberithub.com/migrate-centos-8-to-centos-stream-8-in-6-easy-steps/](https://www.cyberithub.com/migrate-centos-8-to-centos-stream-8-in-6-easy-steps/)
4. [Docker Deskop using cgroupvw](https://github.com/docker/for-mac/issues/6073#issuecomment-990718272)