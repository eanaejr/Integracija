package com.example.integration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JniIntegratorTest {

    @Test
    public void testJavaFallbackAccuracy() {
        JniIntegrator integrator = new JniIntegrator();

        double res = integrator.integrate(0, 1, 1000, 2, 0, false);
        assertEquals(1.0/3.0, res, 1e-4);
    }
}