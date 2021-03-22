package com.example;

import com.example.model.Customer.CustomerOuterClass.Customer;

public class ProducerProtobufApplication {

    public static void main(String[] args) {
        Customer customer = Customer.newBuilder()
                .setId(1)
                .setFirstName("John")
                .setLastName("Doe")
                .setEmail("johndoe@gmail.com")
                .setActive(true)
                .build();

    }

}
