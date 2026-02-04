#include <jni.h>
#include <math.h>
#include <stdio.h>

static double f(double x, int functionId) {
    switch (functionId) {
        case 0: return sin(x);
        case 1: return cos(x);
        default: return x * x;
    }
}

static double integrate_trapezoid(double a, double b, int n, int functionId) {
    if (n <= 0) return 0.0;
    const double h = (b - a) / (double)n;

    double sum = 0.5 * (f(a, functionId) + f(b, functionId));
    for (int i = 1; i < n; ++i) {
        const double x = a + (double)i * h;
        sum += f(x, functionId);
    }
    return sum * h;
}

static double integrate_simpson(double a, double b, int n, int functionId) {
    if (n <= 0) return 0.0;
    if (n % 2 != 0) n += 1; //Simpson treba paran n

    const double h = (b - a) / (double)n;
    double sum = f(a, functionId) + f(b, functionId);

    //neparni
    for (int i = 1; i < n; i += 2) {
        const double x = a + (double)i * h;
        sum += 4.0 * f(x, functionId);
    }
    //parni
    for (int i = 2; i < n; i += 2) {
        const double x = a + (double)i * h;
        sum += 2.0 * f(x, functionId);
    }

    return (h / 3.0) * sum;
}

JNIEXPORT jdouble JNICALL Java_com_example_integration_JniIntegrator_integrateNative
  (JNIEnv *env, jobject obj, jdouble a, jdouble b, jint n, jint functionId, jint algoId)
{

    //test koji provjerava radi li JNI
    /*printf("JNI CALLED\n");
    fflush(stdout);*/


    (void)env; (void)obj;

    const double A = (double)a;
    const double B = (double)b;
    const int N = (int)n;
    const int FID = (int)functionId;
    const int ALG = (int)algoId;

    if (ALG == 1) {
        return (jdouble)integrate_simpson(A, B, N, FID);
    }
    return (jdouble)integrate_trapezoid(A, B, N, FID);
}

