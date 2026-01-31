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

    
    public Future<IntegrationJob> submit(String functionName, double a, double b, int n, int functionId, int algoId) {
        final IntegrationJob job = new IntegrationJob(functionName, a, b, n);
        return executor.submit(() -> {

            final int workers = ((ThreadPoolExecutor) executor).getCorePoolSize();
            final int chunks = Math.max(1, workers);
            final double len = b - a;
            final double chunkSize = len / chunks;

            final int baseN = n / chunks;
            final int remainder = n % chunks;

            CompletionService<Double> cs = new ExecutorCompletionService<>(executor);

            for (int i = 0; i < chunks; i++) {
                final int chunkIndex = i;
                final double startX = a + chunkIndex * chunkSize;
                final double endX = (chunkIndex == chunks - 1) ? b : (startX + chunkSize);

                final int chunkN = baseN + (chunkIndex < remainder ? 1 : 0);

                cs.submit(() -> {
                    double partial = integrator.integrate(startX, endX, chunkN, functionId, algoId);
                    return partial;
                });
            }

            double sum = 0;

            for (int i = 0; i < chunks; i++) {
                sum += cs.take().get();
            }

            job.setResult(sum);
            return job;
        });
    }

    public void shutdown() {
        executor.shutdown();
        repo.close();
    }
}