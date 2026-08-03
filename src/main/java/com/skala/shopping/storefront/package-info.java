@ApplicationModule(
        displayName = "Storefront",
        allowedDependencies = {"auth", "common", "member", "order", "wallet"}
)
package com.skala.shopping.storefront;

import org.springframework.modulith.ApplicationModule;
