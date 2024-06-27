package com.example.productservice;

import java.math.BigDecimal;

record ProductPriceChangedEvent(String productCode, BigDecimal price) {}
