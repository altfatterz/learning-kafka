With Docker Desktop you need to set the following:
Set the `"deprecatedCgroupv1": true` in the `~/Library/Group\ Containers/group.com.docker/settings.json`

```bash
$ docker build --rm -t localbuild/ubi8 .
```

```bash
$ docker compose up -d`
```

```bash
$ ansible-galaxy collection install confluent.platform:7.2.1
```

```bash
$ ansible-playbook -i hosts.yml confluent.platform.all --tags=zookeeper
$ ansible-playbook -i hosts.yml confluent.platform.all --tags=kafka_broker
```

```bash
$ docker compose down -v
```

Resources

1. [https://docs.confluent.io/ansible/current/ansible-configure.html](https://docs.confluent.io/ansible/current/ansible-configure.html)
2. [https://github.com/docker/for-mac/issues/6073](https://github.com/docker/for-mac/issues/6073)
3. [https://www.cyberithub.com/solved-failed-to-download-metadata-for-repo-appstream/](https://www.cyberithub.com/solved-failed-to-download-metadata-for-repo-appstream/)
4 [https://www.cyberithub.com/migrate-centos-8-to-centos-stream-8-in-6-easy-steps/](https://www.cyberithub.com/migrate-centos-8-to-centos-stream-8-in-6-easy-steps/)
4. [Docker Deskop using cgroupvw](https://github.com/docker/for-mac/issues/6073#issuecomment-990718272)