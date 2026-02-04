package com.example.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JniIntegratorTest {

    private final JniIntegrator integrator = new JniIntegrator();

    @Test
    public void testIntegrateXSquared_trapezoid_java() {
        double res = integrator.integrate(0.0, 1.0, 200, 2, 0, false);
        assertEquals(1.0 / 3.0, res, 1e-4);
    }

    @Test
    public void testIntegrateSin_simpson_java() {
        double res = integrator.integrate(0.0, Math.PI, 200, 0, 1, false);
        assertEquals(2.0, res, 1e-4);
    }

    @Test
    public void testZeroInterval() {
        double res = integrator.integrate(1.0, 1.0, 10, 2, 0, false);
        assertEquals(0.0, res, 0.0);
    }
}