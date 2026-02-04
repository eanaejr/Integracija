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

        // osi
        g2.setColor(Color.GRAY);
        //g2.drawLine(margin, h - margin, w - margin, h - margin); // x
        
        
        
        g2.setColor(Color.BLACK);
        g2.drawString(String.format("%.2f", a), margin - 10, h - margin + 15);
        g2.drawString(String.format("%.2f", b),w - margin - 10, h - margin + 15);

        
        //g2.drawLine(margin, margin, margin, h - margin);        // y

        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double[] xs = new double[samples];
        double[] ys = new double[samples];

        for (int i = 0; i < samples; i++) {
            double x = a + i * (b - a) / (samples - 1);
            double y = function.applyAsDouble(x);
            xs[i] = x;
            ys[i] = y;
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        if (minY == maxY) {
            minY -= 1;
            maxY += 1;
        }

        // skaliranje
        double scaleX = (w - 2.0 * margin) / (b - a);
        double scaleY = (h - 2.0 * margin) / (maxY - minY);
        
        Integer zeroX = null;
        Integer zeroY = null;
        
        g2.setColor(Color.GRAY);

        // x-os
        int xAxisY = (zeroY != null) ? zeroY : (h - margin);
        g2.drawLine(margin, xAxisY, w - margin, xAxisY);

        // y-os
        int yAxisX = (zeroX != null) ? zeroX : margin;
        g2.drawLine(yAxisX, margin, yAxisX, h - margin);
        
        int labelY = xAxisY + 15;

        g2.setColor(Color.BLACK);
        g2.drawString(String.format("%.2f", a), margin - 10, labelY);
        g2.drawString(String.format("%.2f", b),
                w - margin - 10, labelY);
        
        if (zeroX != null && zeroY != null) {
            g2.drawString("0", zeroX - 8, zeroY + 15);
        }


        if (a <= 0 && b >= 0) {
            zeroX = (int) (margin + (-a) * scaleX);
        }

        if (minY <= 0 && maxY >= 0) {
            zeroY = (int) (h - margin - (-minY) * scaleY);
        }

        // obojaj površinu ispod grafa
        g2.setColor(new Color(100, 150, 255, 80));
        Polygon area = new Polygon();
        area.addPoint(margin, h - margin);

        for (int i = 0; i < samples; i++) {
            int px = (int) (margin + (xs[i] - a) * scaleX);
            int py = (int) (h - margin - (ys[i] - minY) * scaleY);
            area.addPoint(px, py);
        }

        area.addPoint(margin + (int)((b - a) * scaleX), h - margin);
        g2.fill(area);

        // linija funkcije
        g2.setColor(Color.BLUE);
        for (int i = 0; i < samples - 1; i++) {
            int x1 = (int) (margin + (xs[i] - a) * scaleX);
            int y1 = (int) (h - margin - (ys[i] - minY) * scaleY);
            int x2 = (int) (margin + (xs[i+1] - a) * scaleX);
            int y2 = (int) (h - margin - (ys[i+1] - minY) * scaleY);
            g2.drawLine(x1, y1, x2, y2);
        }
        // dodavanje nule na grafu
        if (a < 0 && b > 0) {
            if (zeroX != null && zeroY != null) {
                g2.drawString("0", zeroX - 5, zeroY + 15);
            }

        }

    }
}
