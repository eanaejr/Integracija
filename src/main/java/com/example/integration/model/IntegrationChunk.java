/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.integration.model;

/**
 *
 * @author Klara
 */
import javax.persistence.*;

@Entity
@Table(name = "integration_chunk")
public class IntegrationChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;
    private int chunkIndex;
    private double startX;
    private double endX;
    private int n;
    private double partialResult;

    public IntegrationChunk() {}

    public IntegrationChunk(Long jobId, int chunkIndex, double startX, double endX, int n, double partialResult) {
        this.jobId = jobId;
        this.chunkIndex = chunkIndex;
        this.startX = startX;
        this.endX = endX;
        this.n = n;
        this.partialResult = partialResult;
    }

    public Long getId() { return id; }
    public Long getJobId() { return jobId; }
    public int getChunkIndex() { return chunkIndex; }
    public double getStartX() { return startX; }
    public double getEndX() { return endX; }
    public int getN() { return n; }
    public double getPartialResult() { return partialResult; }
}
