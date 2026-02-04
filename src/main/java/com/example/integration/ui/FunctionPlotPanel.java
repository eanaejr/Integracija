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

    private static final Color BG_COLOR = new Color(250, 251, 252);
    private static final Color AXIS_COLOR = new Color(180, 180, 180);
    private static final Color CURVE_COLOR = new Color(0, 122, 255);
    private static final Color TEXT_COLOR = new Color(50, 50, 50);

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
        if (function == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(BG_COLOR);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(TEXT_COLOR);



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
        if (minX == maxX) {
            minX -= 1;
            maxX += 1;
        }
        if (minY == maxY) {
            minY -= 1;
            maxY += 1;
        }

        // --- skaliranje ---
        double scaleX = (w - 2.0 * margin) / (maxX - minX);
        double scaleY = (h - 2.0 * margin) / (maxY - minY);

        // funkcije mapiranja (bez lambdi)
        // px = margin + (x - minX) * scaleX
        // py = h - margin - (y - minY) * scaleY
        int zeroX = (int) Math.round(margin + (0.0 - minX) * scaleX);      // x=0
        int zeroY = (int) Math.round(h - margin - (0.0 - minY) * scaleY);  // y=0

        // --- osi ---
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(AXIS_COLOR);
        g2.drawLine(margin, zeroY, w - margin, zeroY); // x-os (y=0)
        g2.drawLine(zeroX, margin, zeroX, h - margin); // y-os (x=0)

        g2.setColor(Color.BLACK);

        // NE crtaj posebnu 0 samo ako je 0 TOČNO a ili b (uz toleranciju)
        double eps = 1e-9;

        boolean zeroIsEndpoint = (Math.abs(a) < eps) || (Math.abs(b) < eps);

        // Crtaj "0" ako NIJE endpoint (znači: unutar intervala ili izvan intervala)
        if (!zeroIsEndpoint) {
            int zx = zeroX;
            int zy = zeroY;

            // pomak ako je preblizu rubovima
            if (zx < margin + 12) {
                zx = margin + 12;
            }
            if (zy > h - margin - 8) {
                zy = h - margin - 8;
            }

            g2.drawString("0", zx - 6, zy + 15);
        }

        // oznake a i b (dolje)
        int bottomY = h - margin + 15;
        int aPx = (int) Math.round(margin + (a - minX) * scaleX);
        int bPx = (int) Math.round(margin + (b - minX) * scaleX);

        // ako je a vrlo blizu osi y (zeroX), pomakni tekst malo da se ne poklopi s "0"
        int aLabelX = aPx - 10;
        if (Math.abs(aPx - zeroX) < 20) {
            aLabelX += 18;
        }

        int bLabelX = bPx - 10;
        if (Math.abs(bPx - zeroX) < 20) {
            bLabelX += 18;
        }

        g2.drawString(String.format("%.2f", a), aLabelX, bottomY);
        g2.drawString(String.format("%.2f", b), bLabelX, bottomY);

        // --- sjenčanje: između funkcije i x-osi (y=0) ---
        GradientPaint gp = new GradientPaint(
                0, zeroY, new Color(33, 150, 243, 120),
                0, h - margin, new Color(33, 150, 243, 10)
        );
        g2.setPaint(gp);

        Polygon area = new Polygon();

        // start na x-osi u x=a
        area.addPoint(aPx, zeroY);

        // točke krivulje
        for (int i = 0; i < samples; i++) {
            double y = ys[i];
            if (!Double.isFinite(y)) {
                continue;
            }

            int px = (int) Math.round(margin + (xs[i] - minX) * scaleX);
            int py = (int) Math.round(h - margin - (y - minY) * scaleY);
            area.addPoint(px, py);
        }

        // zatvori na x-osi u x=b
        area.addPoint(bPx, zeroY);

        g2.fill(area);

        // --- linija funkcije ---
        g2.setColor(CURVE_COLOR);
        g2.setStroke(new BasicStroke(2.5f));

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
