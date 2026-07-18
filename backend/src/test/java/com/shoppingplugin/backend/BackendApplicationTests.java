package com.shoppingplugin.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BackendApplicationTests {

    @Test
    void mainMethodExists() {
        assertDoesNotThrow(() -> BackendApplication.class.getMethod("main", String[].class));
    }
}
