package com.example.integration.repo;

import com.example.integration.model.IntegrationJob;
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

    public void close() {
        sessionFactory.close();
    }
}