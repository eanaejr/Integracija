package com.example.integration;

public class JniIntegrator {

    private boolean nativeAvailable = false;

    static {
        try {
            System.loadLibrary("integrator");
        } catch (UnsatisfiedLinkError e) {
        }
    }

    public JniIntegrator() {
        try {
            nativeAvailable = false;
        } catch (Throwable t) {
            nativeAvailable = false;
        }
    }

    public native double integrateNative(double a, double b, int n, int method);

    private double integrateJava(double a, double b, int n, int method) {
        if (n <= 0) return 0.0;
        double h = (b - a) / n;
        double sum = 0.0;
        double fa = f(a, method);
        double fb = f(b, method);
        sum = 0.5 * (fa + fb);
        for (int i = 1; i < n; i++) {
            double x = a + i * h;
            sum += f(x, method);
        }
        return sum * h;
    }

    private double f(double x, int method) {
        switch (method) {
            case 0: return Math.sin(x);
            case 1: return Math.cos(x);
            default: return x * x;
        }
    }

    public double integrate(double a, double b, int n, int method) {
        if (nativeAvailable) {
            try {
                return integrateNative(a, b, n, method);
            } catch (UnsatisfiedLinkError e) {
                // padamo na Java fallback
            }
        }
        return integrateJava(a, b, n, method);
    }
}