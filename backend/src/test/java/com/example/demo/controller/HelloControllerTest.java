package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Simple tests for {@link HelloController} - verifies bean exists.
 */
@SpringBootTest
class HelloControllerTest {

    @Autowired
    private HelloController helloController;

    @Test
    void controllerBeanExists() {
        assertThat(helloController).isNotNull();
    }
}