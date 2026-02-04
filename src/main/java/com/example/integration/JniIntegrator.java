package com.example.integration;

public class JniIntegrator {

    private static final boolean LIB_LOADED;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("integrator");
            loaded = true;
        }
        catch (UnsatisfiedLinkError ignored) {//ako nema native koda koristi javu
        }
        LIB_LOADED = loaded;
    }

    private final boolean nativeAvailable;

    public JniIntegrator() {
        this.nativeAvailable = LIB_LOADED;
    }

    public native double integrateNative(double a, double b, int n, int functionId, int algoId);

    public boolean isNativeAvailable() {
        return nativeAvailable;
    }

    public double integrate(double a, double b, int n, int functionId, int algoId, boolean preferNative) {
        if (preferNative && nativeAvailable) {
            try {
                return integrateNative(a, b, n, functionId, algoId);
            }
            catch (UnsatisfiedLinkError ignored) {//ako native kod ne radi nastavi dalje na javu}
            }
        }
        return integrateJava(a, b, n, functionId, algoId);
    }
    private static double f(double x, int functionId) {
        return switch (functionId) {
            case 0 -> Math.sin(x);
            case 1 -> Math.cos(x);
            case 2 -> x * x;
            case 3 -> Math.tan(x);
            case 4 -> Math.exp(x);
            case 5 -> Math.sqrt(x);
            case 6 -> 1.0 / x;
            case 7 -> Math.log(x);     // log = ln
            case 8 -> Math.log10(x);   // log10
            default -> 0.0;
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

        // integriranje pomocu trapezne formule
        final double h = (b - a) / n;
        double sum = 0.5 * (f(a, functionId) + f(b, functionId));
        for (int i = 1; i < n; i++) {
            sum += f(a + i * h, functionId);
        }
        return sum * h;
    }
}
