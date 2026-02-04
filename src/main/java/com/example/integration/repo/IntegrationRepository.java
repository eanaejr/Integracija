package com.example.integration.repo;

import com.example.integration.model.IntegrationJob;
import com.example.integration.model.IntegrationChunk;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.List;

public class IntegrationRepository {

    private final SessionFactory sessionFactory;

    public IntegrationRepository() {
        Configuration cfg = new Configuration().configure(); 
        sessionFactory = cfg.buildSessionFactory();
    }

    public IntegrationJob save(IntegrationJob job) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.saveOrUpdate(job);
            session.getTransaction().commit();
            return job;
        }
    }

    @SuppressWarnings("unchecked")
    public List<IntegrationJob> listAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from IntegrationJob").list();
        }
    }
    
    public IntegrationChunk saveChunk(IntegrationChunk chunk) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.save(chunk);
            session.getTransaction().commit();
            return chunk;
        }
    }

    @SuppressWarnings("unchecked")
    public List<IntegrationChunk> listChunksForJob(long jobId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from IntegrationChunk c where c.jobId = :jobId order by c.chunkIndex asc")
                    .setParameter("jobId", jobId)
                    .list();
        }
    }

    public void close() {
        sessionFactory.close();
    }
}