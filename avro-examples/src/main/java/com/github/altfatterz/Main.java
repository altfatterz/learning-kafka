package com.github.altfatterz;

import com.github.altfatterz.avro.Account;
import com.github.altfatterz.avro.AccountType;
import com.github.altfatterz.avro.Customer;

import java.util.Arrays;
import java.util.HashMap;

public class Main {

    public static void main(String[] args) {
        Customer customer = Customer.newBuilder()
                .setFirstName("John")
                .setLastName("Doe")
                .setAccounts(Arrays.asList(
                        Account.newBuilder()
                                .setIban("CH93 0076 2011 6238 5295 7")
                                .setType(AccountType.CHECKING)
                                .build(),
                        Account.newBuilder()
                                .setIban("CH93 0076 2011 6238 5295 8")
                                .setType(AccountType.SAVING)
                                .build()
                ))
                .setSettings(new HashMap<String, Boolean>() {{
                    put("e-billing-enabled", true);
                    put("push-notification-enabled", false);
                }})
                .setSignupTimestamp(System.currentTimeMillis())
                .build();

        System.out.println(customer);
    }
}
