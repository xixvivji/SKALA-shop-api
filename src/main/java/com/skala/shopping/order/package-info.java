@ApplicationModule(
        displayName = "Order",
        allowedDependencies = {"catalog", "common", "inventory", "wallet"}
)
package com.skala.shopping.order;

import org.springframework.modulith.ApplicationModule;
