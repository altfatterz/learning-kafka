package com.example.consumerprotobuf;

import com.example.model.Customer.CustomerOuterClass;

public class ConsumerApp {

    public static void main(String[] args) {

        CustomerOuterClass.Customer customer = CustomerOuterClass.Customer.newBuilder()
                .setId(1)
                .build();

    }

}
