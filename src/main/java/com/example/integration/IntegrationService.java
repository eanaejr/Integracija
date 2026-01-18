package com.example.integration;

import com.example.integration.model.IntegrationJob;
import com.example.integration.repo.IntegrationRepository;

import java.util.concurrent.*;


public class IntegrationService {

    private final ExecutorService executor;
    private final IntegrationRepository repo;
    private final JniIntegrator integrator;

    public IntegrationService(int threads) {
        this.executor = Executors.newFixedThreadPool(threads);
        this.repo = new IntegrationRepository();
        this.integrator = new JniIntegrator();
    }

    
    public Future<IntegrationJob> submit(String functionName, double a, double b, int n, int functionMethod) {
        IntegrationJob job = new IntegrationJob(functionName, a, b, n);
        return executor.submit(() -> {
            double result = integrator.integrate(a, b, n, functionMethod);
            job.setResult(result);
            repo.save(job);
            return job;
        });
    }

    public void shutdown() {
        executor.shutdown();
        repo.close();
    }
}