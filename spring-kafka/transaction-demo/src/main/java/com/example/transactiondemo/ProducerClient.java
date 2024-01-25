package com.example.transactiondemo;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProducerClient {

    private final Producer producer;

    public ProducerClient(Producer producer) {
        this.producer = producer;
    }

    @PostMapping("/messages1")
    public void send(@RequestBody String message) {
        producer.process1(message);
    }

    @PostMapping("/messages2")
    public void send2(@RequestBody String message) throws InterruptedException {
        producer.process2(message);
    }


}
