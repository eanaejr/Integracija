/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.integration.ui;

/**
 *
 * @author Klara
 */
import javax.swing.*;
import java.awt.*;
import java.util.function.DoubleUnaryOperator;

public class FunctionPlotPanel extends JPanel {

    private DoubleUnaryOperator function;
    private double a, b;
    private int samples = 500;

    public void setFunction(DoubleUnaryOperator function, double a, double b) {
        this.function = function;
        this.a = a;
        this.b = b;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (function == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int margin = 40;

        // --- sample funkciju ---
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double[] xs = new double[samples];
        double[] ys = new double[samples];

        for (int i = 0; i < samples; i++) {
            double x = a + i * (b - a) / (samples - 1);
            double y = function.applyAsDouble(x);

            xs[i] = x;
            ys[i] = y;

            if (Double.isFinite(y)) {
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }

        // fallback ako je sve NaN/Inf
        if (minY == Double.POSITIVE_INFINITY) {
            minY = -1;
            maxY = 1;
        }

        if (minY == maxY) {
            minY -= 1;
            maxY += 1;
        }

        // --- uključi 0 u raspon da osi uvijek prolaze kroz nulu ---
        double minX = Math.min(a, b);
        double maxX = Math.max(a, b);
        minX = Math.min(minX, 0.0);
        maxX = Math.max(maxX, 0.0);

        minY = Math.min(minY, 0.0);
        maxY = Math.max(maxY, 0.0);

        // sigurnost protiv 0 širine
        if (minX == maxX) { minX -= 1; maxX += 1; }
        if (minY == maxY) { minY -= 1; maxY += 1; }

        // --- skaliranje ---
        double scaleX = (w - 2.0 * margin) / (maxX - minX);
        double scaleY = (h - 2.0 * margin) / (maxY - minY);

        // funkcije mapiranja (bez lambdi)
        // px = margin + (x - minX) * scaleX
        // py = h - margin - (y - minY) * scaleY

        int zeroX = (int) Math.round(margin + (0.0 - minX) * scaleX);      // x=0
        int zeroY = (int) Math.round(h - margin - (0.0 - minY) * scaleY);  // y=0

        // --- osi ---
        g2.setColor(Color.GRAY);
        g2.drawLine(margin, zeroY, w - margin, zeroY); // x-os (y=0)
        g2.drawLine(zeroX, margin, zeroX, h - margin); // y-os (x=0)

        // 0 na sjecištu
        g2.setColor(Color.BLACK);
        g2.drawString("0", zeroX - 6, zeroY + 15);

        // oznake a i b (dolje)
        int bottomY = h - margin + 15;
        int aPx = (int) Math.round(margin + (a - minX) * scaleX);
        int bPx = (int) Math.round(margin + (b - minX) * scaleX);
        g2.drawString(String.format("%.2f", a), aPx - 10, bottomY);
        g2.drawString(String.format("%.2f", b), bPx - 10, bottomY);

        // --- sjenčanje: između funkcije i x-osi (y=0) ---
        g2.setColor(new Color(100, 150, 255, 80));
        Polygon area = new Polygon();

        // start na x-osi u x=a
        area.addPoint(aPx, zeroY);

        // točke krivulje
        for (int i = 0; i < samples; i++) {
            double y = ys[i];
            if (!Double.isFinite(y)) continue;

            int px = (int) Math.round(margin + (xs[i] - minX) * scaleX);
            int py = (int) Math.round(h - margin - (y - minY) * scaleY);
            area.addPoint(px, py);
        }

        // zatvori na x-osi u x=b
        area.addPoint(bPx, zeroY);

        g2.fill(area);

        // --- linija funkcije ---
        g2.setColor(Color.BLUE);

        int prevX = 0, prevY = 0;
        boolean hasPrev = false;

        for (int i = 0; i < samples; i++) {
            double y = ys[i];
            if (!Double.isFinite(y)) {
                hasPrev = false;
                continue;
            }

            int px = (int) Math.round(margin + (xs[i] - minX) * scaleX);
            int py = (int) Math.round(h - margin - (y - minY) * scaleY);

            if (hasPrev) {
                g2.drawLine(prevX, prevY, px, py);
            }
            prevX = px;
            prevY = py;
            hasPrev = true;
        }
    }
}