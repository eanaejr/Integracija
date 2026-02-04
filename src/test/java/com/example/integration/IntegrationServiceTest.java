package com.example.integration;

import com.example.integration.model.IntegrationChunk;
import com.example.integration.model.IntegrationJob;
import com.example.integration.repo.IntegrationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class IntegrationServiceTest {

    private IntegrationService service;
    private IntegrationRepository mockRepo;
    private final AtomicLong idSeq = new AtomicLong(100L);

    @BeforeEach
    public void setUp() {
        mockRepo = mock(IntegrationRepository.class);

        when(mockRepo.save(any(IntegrationJob.class))).thenAnswer(invocation -> {
            IntegrationJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                setPrivateId(job, idSeq.getAndIncrement());
            }
            return job;
        });

        when(mockRepo.saveChunk(any(IntegrationChunk.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service = new IntegrationService(4, mockRepo);
    }

    @AfterEach
    public void tearDown() {
        if (service != null) service.shutdown();
    }

    @Test
    public void testJobIdPropagationToChunks() throws Exception {
        // koristimo manji n da test brže završi
        Future<IntegrationJob> future = service.submit("x^2", 0.0, 10.0, 200, 2, 0, false, null);

        // čekamo s timeoutom da test ne visi (10 sekundi dovoljno za lokalni run)
        IntegrationJob result = future.get(10, TimeUnit.SECONDS);
        assertNotNull(result);
        assertNotNull(result.getId());

        ArgumentCaptor<IntegrationChunk> chunkCaptor = ArgumentCaptor.forClass(IntegrationChunk.class);
        verify(mockRepo, atLeastOnce()).saveChunk(chunkCaptor.capture());

        List<IntegrationChunk> capturedChunks = chunkCaptor.getAllValues();
        assertFalse(capturedChunks.isEmpty());

        for (IntegrationChunk chunk : capturedChunks) {
            assertEquals(result.getId(), chunk.getJobId(), "Chunk mora imati isti jobId kao spremljeni job");
        }
    }

    @Test
    public void testIntegrationXSquaredResult() throws Exception {
        Future<IntegrationJob> future = service.submit("x^2", 0.0, 1.0, 200, 2, 0, false, null);
        IntegrationJob r = future.get(10, TimeUnit.SECONDS);
        assertEquals(1.0 / 3.0, r.getResult(), 1e-4);
    }

    // Pomocna metoda
    private static void setPrivateId(IntegrationJob job, long id) {
        try {
            Field idField = IntegrationJob.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(job, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}