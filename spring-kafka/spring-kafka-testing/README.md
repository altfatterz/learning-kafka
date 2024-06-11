
`EmbeddedKafkaIntegrationTest`

```bash
2024-06-11T16:15:34.549+02:00  INFO 29746 --- [           main] c.e.springkafkatesting.KafkaProducer     : sending to topic 'test-demo' the payload 'Chuck Norris can spawn threads that complete before they are started.'
...

2024-06-11T16:15:36.412+02:00  INFO 29746 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : 85f24c0f-cd2a-495f-9286-709788fa5cc2: partitions assigned: [test-demo-0]
2024-06-11T16:15:36.454+02:00  INFO 29746 --- [ntainer#0-0-C-1] c.e.springkafkatesting.KafkaConsumer     : received payload='Chuck Norris can spawn threads that complete before they are started.'
```
