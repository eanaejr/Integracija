package com.example.integration;

import com.example.integration.model.IntegrationChunk;
import com.example.integration.model.IntegrationJob;
import com.example.integration.repo.IntegrationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class IntegrationServiceTest {

    private IntegrationService service;
    private IntegrationRepository mockRepo;

    @BeforeEach
    public void setUp() {
        // "lazni repo", ne spaja se na pravu bazu.
        mockRepo = Mockito.mock(IntegrationRepository.class);

        // simuliramo rad baze
        when(mockRepo.save(any(IntegrationJob.class))).thenAnswer(invocation -> {
            IntegrationJob job = invocation.getArgument(0);
            return job;
        });

        when(mockRepo.saveChunk(any(IntegrationChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service = new IntegrationService(4, mockRepo);
    }

    @AfterEach
    public void tearDown() {
        service.shutdown();
    }

    @Test
    public void testIntegrationXSquaredJavaImplementation() throws ExecutionException, InterruptedException {
        Future<IntegrationJob> future = service.submit(
                "x^2",
                0.0,
                1.0,
                1000,
                2,
                0,
                false,
                null
        );

        IntegrationJob result = future.get();

        assertEquals(1.0 / 3.0, result.getResult(), 1e-4, "Integral x^2 od 0 do 1 mora biti cca 0.3333");

        verify(mockRepo, atLeastOnce()).save(any(IntegrationJob.class));
    }

    @Test
    public void testIntegrationSinSimpson() throws ExecutionException, InterruptedException {

        Future<IntegrationJob> future = service.submit(
                "sin(x)",
                0.0,
                Math.PI,
                1000,
                0,
                1,
                false,
                null
        );

        IntegrationJob result = future.get();
        assertEquals(2.0, result.getResult(), 1e-5, "Integral sin(x) od 0 do PI mora biti 2.0");
    }

    @Test
    public void testCustomExpressionExp4j() throws ExecutionException, InterruptedException {
        // Testiramo custom expression parser
        // Integral 2x od 0 do 2 =

        String expr = "2*x";

        Future<IntegrationJob> future = service.submit(
                expr,
                0.0,
                2.0,
                1000,
                -1,
                0,
                false,
                expr
        );

        IntegrationJob result = future.get();
        assertEquals(4.0, result.getResult(), 1e-4);
    }

    @Test
    public void testChunkingLogic() throws ExecutionException, InterruptedException {
        // Provjeravamo spremaju li se međurezultati (chunkovi)

        Future<IntegrationJob> future = service.submit(
                "x^2",
                0.0,
                10.0,
                1000,
                2,
                0,
                false,
                null
        );

        future.get();

        verify(mockRepo, atLeast(1)).saveChunk(any(IntegrationChunk.class));
    }

    @Test
    public void testZeroInterval() throws ExecutionException, InterruptedException {
        // Integral od a do a mora biti 0
        Future<IntegrationJob> future = service.submit("x^2", 5.0, 5.0, 100, 2, 0, false, null);
        assertEquals(0.0, future.get().getResult(), 1e-9);
    }
}