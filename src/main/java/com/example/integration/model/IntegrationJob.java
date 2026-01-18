package com.example.integration.model;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "integration_job")
public class IntegrationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String functionName;
    private double a;
    private double b;
    private int n;
    private double result;
    private Instant createdAt;

    public IntegrationJob() {}

    public IntegrationJob(String functionName, double a, double b, int n) {
        this.functionName = functionName;
        this.a = a;
        this.b = b;
        this.n = n;
        this.createdAt = Instant.now();
    }


    public Long getId() { return id; }
    public String getFunctionName() { return functionName; }
    public void setFunctionName(String functionName) { this.functionName = functionName; }
    public double getA() { return a; }
    public void setA(double a) { this.a = a; }
    public double getB() { return b; }
    public void setB(double b) { this.b = b; }
    public int getN() { return n; }
    public void setN(int n) { this.n = n; }
    public double getResult() { return result; }
    public void setResult(double result) { this.result = result; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}