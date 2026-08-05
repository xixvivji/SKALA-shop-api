package com.skala.shopping.storefront.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class MemberRegistrationPropertiesTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void acceptsTheLargestJavaScriptSafeInitialPointBalance() {
        contextRunner
                .withPropertyValues("shopping.member.initial-points=30000000000000.00")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MemberRegistrationProperties.class).getInitialPoints())
                            .isEqualByComparingTo("30000000000000.00");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "-0.01",
            "0.001",
            "30000000000000.01",
            "100000000000000000.00"
    })
    void rejectsValuesOutsideJavaScriptSafeRangeOrScale(String initialPoints) {
        contextRunner
                .withPropertyValues("shopping.member.initial-points=" + initialPoints)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MemberRegistrationProperties.class)
    static class TestConfiguration {
    }
}
