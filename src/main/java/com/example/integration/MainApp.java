package com.example.integration;

import com.example.integration.model.IntegrationJob;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainApp {

    private final IntegrationService service;

    public MainApp() {
        this.service = new IntegrationService(3);
    }

    public void start() {
        SwingUtilities.invokeLater(this::buildAndShow);
    }

    private void buildAndShow() {
        JFrame frame = new JFrame("Numerička integracija - Skeleton");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 220);
        frame.setLayout(new BorderLayout());

        JPanel input = new JPanel(new GridLayout(5, 2, 8, 8));
        JLabel lblFunc = new JLabel("Funkcija:");
        JComboBox<String> cmbFunc = new JComboBox<>(new String[]{"sin(x)", "cos(x)", "x^2"});
        JLabel lblAlgo = new JLabel("Metoda:");
        JComboBox<String> cmbAlgo = new JComboBox<>(new String[]{"Trapezna metoda", "Simpsonova metoda"});
        JLabel lblA = new JLabel("a:");
        JTextField txtA = new JTextField("0");
        JLabel lblB = new JLabel("b:");
        JTextField txtB = new JTextField("1");
        JLabel lblN = new JLabel("n (subintervals):");
        JTextField txtN = new JTextField("1000000");
        JCheckBox chkNative = new JCheckBox("Koristi JNI (ako je dostupno)");
        JButton btnStart = new JButton("Pokreni integraciju");
        JLabel lblStatus = new JLabel("Status: idle");
        JLabel lblResult = new JLabel("Rezultat: -");

        input.add(lblFunc);
        input.add(cmbFunc);
        input.add(lblAlgo);
        input.add(cmbAlgo);
        input.add(lblA);
        input.add(txtA);
        input.add(lblB);
        input.add(txtB);
        input.add(lblN);
        input.add(txtN);
        input.add(chkNative);
        input.add(btnStart);
        input.add(lblStatus);

        frame.add(input, BorderLayout.CENTER);
        frame.add(lblResult, BorderLayout.SOUTH);

        btnStart.addActionListener((ActionEvent e) -> {
            btnStart.setEnabled(false);
            lblStatus.setText("Status: radi...");
            String func = (String) cmbFunc.getSelectedItem();
            double a = Double.parseDouble(txtA.getText());
            double b = Double.parseDouble(txtB.getText());
            int n = Integer.parseInt(txtN.getText());
            int functionId = cmbFunc.getSelectedIndex();
            int algoId = cmbAlgo.getSelectedIndex();
            boolean preferNative = chkNative.isSelected();

            Future<IntegrationJob> fut = service.submit(func, a, b, n, functionId, algoId, preferNative);

            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    IntegrationJob job = fut.get();
                    SwingUtilities.invokeLater(() -> {
                        lblResult.setText(String.format("Rezultat: %.12f (spremno u DB, id=%d)", job.getResult(), job.getId()));
                        lblStatus.setText("Status: gotov");
                        btnStart.setEnabled(true);
                    });
                } catch (InterruptedException | ExecutionException ex) {
                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText("Status: greška - " + ex.getMessage());
                        btnStart.setEnabled(true);
                    });
                }
            });
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new MainApp().start();
    }
}