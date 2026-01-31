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

    private static double f(double x, int functionId) {
        return switch (functionId) {
            case 0 -> Math.sin(x);
            case 1 -> Math.cos(x);
            default -> x * x;
        };
    }

    private static double integrateJava(double a, double b, int n, int functionId, int algoId) {
        if (n <= 0) return 0.0;

        //Simpsonova metoda
        if (algoId == 1) {
            if (n % 2 != 0) n += 1;
            final double h = (b - a) / n;
            double sum = f(a, functionId) + f(b, functionId);

            for (int i = 1; i < n; i += 2) {
                sum += 4.0 * f(a + i * h, functionId);
            }
            for (int i = 2; i < n; i += 2) {
                sum += 2.0 * f(a + i * h, functionId);
            }
            return (h / 3.0) * sum;
        }

        //integracije pomocu trapezne formule
        final double h = (b - a) / n;
        double sum = 0.5 * (f(a, functionId) + f(b, functionId));
        for (int i = 1; i < n; i++) {
            sum += f(a + i * h, functionId);
        }
        return sum * h;
    }

    public double integrate(double a, double b, int n, int functionId, int algoId) {
        if (nativeAvailable) {
            try {
                return integrateNative(a, b, n, algoId);
            } catch (UnsatisfiedLinkError e) {
                // padamo na Java fallback
            }
        }
        return integrateJava(a, b, n, functionId, algoId);
    }
}