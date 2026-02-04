package com.example.integration;

import com.example.integration.model.IntegrationJob;
import com.example.integration.model.IntegrationChunk;
import com.example.integration.repo.IntegrationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class IntegrationServiceAdditionalTest {

    private IntegrationService service;
    private IntegrationRepository mockRepo;

    @BeforeEach
    public void setUp() {
        mockRepo = Mockito.mock(IntegrationRepository.class);

        // simuliramo spremanje
        when(mockRepo.save(any(IntegrationJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mockRepo.saveChunk(any(IntegrationChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service = new IntegrationService(4, mockRepo);
    }

    @AfterEach
    public void tearDown() {
        service.shutdown();
    }

    @Test
    public void testZeroAndNegativeNProduceZeroResult() throws Exception {
        // n == 0
        Future<IntegrationJob> fut0 = service.submit("x^2", 0.0, 1.0, 0, 2, 0, false, null);
        IntegrationJob job0 = fut0.get();
        assertEquals(0.0, job0.getResult(), 0.0, "n == 0 should produce 0.0 result");

        // n < 0
        Future<IntegrationJob> futNeg = service.submit("x^2", 0.0, 1.0, -100, 2, 0, false, null);
        IntegrationJob jobNeg = futNeg.get();
        assertEquals(0.0, jobNeg.getResult(), 0.0, "n < 0 should produce 0.0 result");
    }

    @Test
    public void testInvalidCustomExpressionFailsFuture() {
        // namjerno nevaljan izraz
        String badExpr = "sin(";
        Future<IntegrationJob> fut = service.submit(badExpr, 0.0, 1.0, 100, -1, 0, false, badExpr);

        ExecutionException ex = assertThrows(ExecutionException.class, fut::get, "Future.get() should throw ExecutionException for invalid expression");
        assertNotNull(ex.getCause(), "Cause must be set");
    }

    @Test
    public void testCustomExpressionProducesNaNWhenUndefinedInsideInterval() throws Exception {
        // sqrt(x) na intervalu koji uključuje negativne vrijednosti
        String expr = "sqrt(x)";
        Future<IntegrationJob> fut = service.submit(expr, -1.0, 1.0, 1000, -1, 0, false, expr);
        IntegrationJob job = fut.get();
        assertTrue(Double.isNaN(job.getResult()) || Double.isInfinite(job.getResult()),
                "Ako je funkcija nedefinirana na dijelu intervala, rezultat numeričkog izračuna može biti NaN ili Infinite");
    }

    @Test
    public void testChunkedComputationMatchesSingleThreadReference() throws Exception {
        double a = 0.0;
        double b = 1.0;
        int n = 1000;
        int functionId = 2;
        int algoId = 0;

        Future<IntegrationJob> fut = service.submit("x^2", a, b, n, functionId, algoId, false, null);
        IntegrationJob job = fut.get();

        JniIntegrator integrator = new JniIntegrator();
        double reference = integrator.integrate(a, b, n, functionId, algoId, false);

        assertEquals(reference, job.getResult(), 1e-6, "Chunked result should equal single-thread reference within tolerance");
    }

    @Test
    public void testSubmitAfterShutdownIsRejected() {
        service.shutdown();
        assertThrows(RejectedExecutionException.class, () ->
                        service.submit("x^2", 0.0, 1.0, 100, 2, 0, false, null),
                "Submitting after shutdown should throw RejectedExecutionException");
    }
}