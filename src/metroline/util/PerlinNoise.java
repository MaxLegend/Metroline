package metroline.util;

import java.util.Random;

public class PerlinNoise {
    private final int[] permutation;
    private final float[] gradientsX;
    private final float[] gradientsY;
    private final float[] gradientsZ;
    private static final int GRADIENT_COUNT = 256;

    public PerlinNoise(long seed) {
        Random rand = new Random(seed);

        // Инициализация градиентов
        gradientsX = new float[GRADIENT_COUNT];
        gradientsY = new float[GRADIENT_COUNT];
        gradientsZ = new float[GRADIENT_COUNT];

        for (int i = 0; i < GRADIENT_COUNT; i++) {
            float angle = (float)(rand.nextFloat() * Math.PI * 2);
            gradientsX[i] = (float)Math.cos(angle);
            gradientsY[i] = (float)Math.sin(angle);
            gradientsZ[i] = rand.nextFloat() * 2 - 1;

            // Нормализация
            float length = (float)Math.sqrt(
                    gradientsX[i] * gradientsX[i] +
                            gradientsY[i] * gradientsY[i] +
                            gradientsZ[i] * gradientsZ[i]
            );
            gradientsX[i] /= length;
            gradientsY[i] /= length;
            gradientsZ[i] /= length;
        }

        // Инициализация перестановок
        permutation = new int[GRADIENT_COUNT * 2];
        int[] p = new int[GRADIENT_COUNT];
        for (int i = 0; i < GRADIENT_COUNT; i++) {
            p[i] = i;
        }

        // Перемешивание
        for (int i = 0; i < GRADIENT_COUNT; i++) {
            int j = rand.nextInt(GRADIENT_COUNT - i) + i;
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
            permutation[i] = permutation[i + GRADIENT_COUNT] = p[i];
        }
    }

    public float noise(float x, float y, float z) {
        // Определяем куб, в котором находится точка
        int xi = (int)x & (GRADIENT_COUNT - 1);
        int yi = (int)y & (GRADIENT_COUNT - 1);
        int zi = (int)z & (GRADIENT_COUNT - 1);

        // Дробные части координат
        float xf = x - (int)x;
        float yf = y - (int)y;
        float zf = z - (int)z;

        // Квадратичное сглаживание
        float u = fade(xf);
        float v = fade(yf);
        float w = fade(zf);

        // Вычисляем индексы градиентов
        int aaa = permutation[permutation[permutation[xi] + yi] + zi];
        int aba = permutation[permutation[permutation[xi] + yi + 1] + zi];
        int aab = permutation[permutation[permutation[xi] + yi] + zi + 1];
        int abb = permutation[permutation[permutation[xi] + yi + 1] + zi + 1];
        int baa = permutation[permutation[permutation[xi + 1] + yi] + zi];
        int bba = permutation[permutation[permutation[xi + 1] + yi + 1] + zi];
        int bab = permutation[permutation[permutation[xi + 1] + yi] + zi + 1];
        int bbb = permutation[permutation[permutation[xi + 1] + yi + 1] + zi + 1];

        // Линейная интерполяция
        float x1 = lerp(
                grad(aaa, xf, yf, zf),
                grad(baa, xf - 1, yf, zf),
                u
        );
        float x2 = lerp(
                grad(aba, xf, yf - 1, zf),
                grad(bba, xf - 1, yf - 1, zf),
                u
        );
        float y1 = lerp(x1, x2, v);

        x1 = lerp(
                grad(aab, xf, yf, zf - 1),
                grad(bab, xf - 1, yf, zf - 1),
                u
        );
        x2 = lerp(
                grad(abb, xf, yf - 1, zf - 1),
                grad(bbb, xf - 1, yf - 1, zf - 1),
                u
        );
        float y2 = lerp(x1, x2, v);

        return (lerp(y1, y2, w) + 1) / 2; // Приводим к диапазону [0, 1]
    }

    private float fade(float t) {
        // 6t^5 - 15t^4 + 10t^3
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private float grad(int hash, float x, float y, float z) {
        // Используем предвычисленные градиенты
        int h = hash & (GRADIENT_COUNT - 1);
        return x * gradientsX[h] + y * gradientsY[h] + z * gradientsZ[h];
    }

    // Методы для генерации фрактального шума
    public float fractalNoise(float x, float y, float z, int octaves, float persistence) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;
        float maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }
}
