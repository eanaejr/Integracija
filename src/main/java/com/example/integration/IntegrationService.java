package com.example.integration;

import com.example.integration.model.IntegrationJob;
import com.example.integration.repo.IntegrationRepository;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.stream.Collectors;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

//koristi zaseban chunkExecutor kako bi izbjegli deadlock prilikom submitanja podzadataka
//poziva repo.save(job) nakon što izračun završi (da bi job.getId() bio popunjen)

public class IntegrationService {

    private final ExecutorService executor;
    private final IntegrationRepository repo;
    private final JniIntegrator integrator;

    public IntegrationService(int threads) {
        this.executor = Executors.newFixedThreadPool(threads);
        this.repo = new IntegrationRepository();
        this.integrator = new JniIntegrator();
    }

    private double integrateCustom(double a, double b, int n, String expr, int algoId) {
        if (n <= 0) return 0.0;

        Expression e = new ExpressionBuilder(expr)
                .variable("x")
                .build();

        if (algoId == 1) { // Simpson
            if (n % 2 != 0) n++;
            double h = (b - a) / n;
            double sum = eval(e, a) + eval(e, b);

            for (int i = 1; i < n; i += 2)
                sum += 4 * eval(e, a + i * h);

            for (int i = 2; i < n; i += 2)
                sum += 2 * eval(e, a + i * h);

            return (h / 3.0) * sum;
        }

        // Trapez
        double h = (b - a) / n;
        double sum = 0.5 * (eval(e, a) + eval(e, b));
        for (int i = 1; i < n; i++)
            sum += eval(e, a + i * h);

        return sum * h;
    }

    private double eval(Expression e, double x) {
        return e.setVariable("x", x).evaluate();
    }


    public Future<IntegrationJob> submit(String functionName, double a, double b, int n, int functionId, int algoId, boolean preferNative, String customExpr) {
        final IntegrationJob job = new IntegrationJob(functionName, a, b, n);
        return executor.submit(() -> {

            if (customExpr != null) {
                double result = integrateCustom(a, b, n, customExpr, algoId);
                job.setResult(result);
                return repo.save(job);
            }

            final int workers = ((ThreadPoolExecutor) executor).getCorePoolSize();
            final int chunks = Math.max(1, workers);
            final double len = b - a;
            final double chunkSize = len / chunks;

            final int baseN = n / chunks;
            final int remainder = n % chunks;

            // Koristimo zaseban executor za chunkove kako bi izbjegli deadlock
            ExecutorService chunkExecutor = Executors.newFixedThreadPool(chunks);
            CompletionService<Double> cs = new ExecutorCompletionService<>(chunkExecutor);

            final AtomicInteger submitted = new AtomicInteger(0);

            for (int i = 0; i < chunks; i++) {
                final int chunkIndex = i;
                final double startX = a + chunkIndex * chunkSize;
                final double endX = (chunkIndex == chunks - 1) ? b : (startX + chunkSize);

                final int chunkN = baseN + (chunkIndex < remainder ? 1 : 0);

                cs.submit(() -> {
                    double partial = integrator.integrate(startX, endX, chunkN, functionId, algoId, preferNative);
                    return partial;
                });
                submitted.incrementAndGet();
            }

            double sum = 0;

            try {
                for (int i = 0; i < submitted.get(); i++) {
                    Future<Double> f = cs.take();
                    sum += f.get();
                }
            } finally {
                chunkExecutor.shutdown();
                try {
                    if (!chunkExecutor.awaitTermination(1, TimeUnit.MINUTES)) {
                        chunkExecutor.shutdownNow();
                    }
                } catch (InterruptedException ie) {
                    chunkExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            // postavlja rezultat i sprema u bazu prije vraćanja
            job.setResult(sum);
            IntegrationJob saved = repo.save(job);
            return saved;
        });
    }

    //Dohvati zandjih n rezultata
    public List<IntegrationJob> getLastJobs(int n) {
        List<IntegrationJob> all = repo.listAll();
        if (all == null || all.isEmpty()) return Collections.emptyList();
        return all.stream()
                .sorted(Comparator.comparing(IntegrationJob::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(n)
                .collect(Collectors.toList());
    }

    public void shutdown() {
        executor.shutdown();
        repo.close();
    }
}