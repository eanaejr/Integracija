package com.example.integration;

import com.example.integration.repo.IntegrationRepository;
import com.example.integration.model.IntegrationChunk;
import com.example.integration.model.IntegrationJob;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationRepositoryIT {

    private IntegrationRepository repo;
    private Path dbPath = Path.of("target", "test-integration.db");

    @BeforeAll
    public void beforeAll() throws Exception {
        // koristit test hibernate.cfg.xml (src/test/resources)
        repo = new IntegrationRepository();
    }

    @AfterAll
    public void afterAll() throws Exception {
        if (repo != null) repo.close();
        try { Files.deleteIfExists(dbPath); } catch (Exception ignored) {}
    }

    @Test
    public void testSaveJobAndChunk_andList() {
        IntegrationJob job = new IntegrationJob("x^2", 0.0, 1.0, 100);
        IntegrationJob savedJob = repo.save(job);

        assertNotNull(savedJob);
        assertNotNull(savedJob.getId(), "save() bi trebao postaviti ID");

        IntegrationChunk chunk = new IntegrationChunk(savedJob.getId(), 0, 0.0, 0.5, 50, 0.333);
        IntegrationChunk savedChunk = repo.saveChunk(chunk);

        assertNotNull(savedChunk);
        assertNotNull(savedChunk.getJobId());
        assertEquals(savedJob.getId(), savedChunk.getJobId());

        List<IntegrationJob> jobs = repo.listAll();
        assertTrue(jobs.stream().anyMatch(j -> j.getId().equals(savedJob.getId())));

        List<IntegrationChunk> chunks = repo.listChunksForJob(savedJob.getId());
        assertEquals(1, chunks.size());
        assertEquals(savedChunk.getPartialResult(), chunks.get(0).getPartialResult());
    }
}