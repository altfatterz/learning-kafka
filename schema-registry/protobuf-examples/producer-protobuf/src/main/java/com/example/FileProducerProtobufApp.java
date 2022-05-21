package com.example;

import com.example.model.Customer.CustomerOuterClass.Customer;
import com.example.model.Customer.CustomerOuterClass.Customer.PhoneNumber;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.example.model.Customer.CustomerOuterClass.Customer.PhoneType.MOBILE;

public class FileProducerProtobufApp {

    public static void main(String[] args) {
        Customer customer = Customer.newBuilder()
                .setId(1)
                .setFirstName("John")
                .setLastName("Doe")
                .setEmail("johndoe@gmail.com")
                .addPhones(PhoneNumber.newBuilder()
                        .setType(MOBILE)
                        .setNumber("0761234678")
                        .build())
                .build();

        String path = "schema-registry/protobuf-examples/producer-protobuf/customer.proto";

        try (FileOutputStream output = new FileOutputStream(path)) {
            customer.writeDelimitedTo(output);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Successfully wrote customer.V1.proto");

        try (FileInputStream input = new FileInputStream(path)) {
            while (true) {
                Customer c = Customer.parseDelimitedFrom(input);
                if (c == null) break;
                System.out.printf("read from file: \n%s", c);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
