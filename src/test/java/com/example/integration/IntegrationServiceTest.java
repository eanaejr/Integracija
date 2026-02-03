package com.example.integration;

import com.example.integration.model.IntegrationJob;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;


public class IntegrationServiceTest {

    @Test
    public void testXSquare() throws Exception {
        IntegrationService s = new IntegrationService(3);
        Future<IntegrationJob> fut = s.submit("x^2", 0.0, 1.0, 1000000, 2, 0, false);
        IntegrationJob job = fut.get();
        double res = job.getResult();
        assertEquals(1.0/3.0, res, 1e-4);
        s.shutdown();
    }
}