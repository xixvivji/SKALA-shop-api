package com.skala.shopping;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(ShoppingApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }
}
