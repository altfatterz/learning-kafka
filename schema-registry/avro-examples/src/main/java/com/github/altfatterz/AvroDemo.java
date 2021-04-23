package com.github.altfatterz;

import com.github.altfatterz.avro.Account;
import com.github.altfatterz.avro.AccountType;
import com.github.altfatterz.avro.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;

public class AvroDemo {

    static final Logger logger = LoggerFactory.getLogger(AvroDemo.class);

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
                .setSignupTimestamp(Instant.now())
                .build();

        logger.info("Customer: {}", customer);
    }
}
