#include <jni.h>
#include <math.h>
#include <stdio.h>


JNIEXPORT jdouble JNICALL Java_com_example_integration_JniIntegrator_integrateNative
  (JNIEnv *env, jobject obj, jdouble a, jdouble b, jint n, jint method)
{
    double A = (double)a;
    double B = (double)b;
    int N = (int)n;
    if (N <= 0) return 0.0;

    double h = (B - A) / N;
    double sum = 0.0;

    auto f = [&](double x)->double {
        if (method == 0) return sin(x);
        if (method == 1) return cos(x);
        return x * x;
    };

    double fa = f(A);
    double fb = f(B);
    sum = 0.5 * (fa + fb);
    for (int i = 1; i < N; ++i) {
        double x = A + i * h;
        sum += f(x);
    }
    double result = sum * h;
    return (jdouble)result;
}

