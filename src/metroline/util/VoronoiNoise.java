package metroline.util;

import java.util.Random;

public class VoronoiNoise {
    private final int[] permutation;
    private final float[] pointsX;
    private final float[] pointsY;
    private static final int POINT_COUNT = 256;

    public VoronoiNoise(long seed) {
        Random rand = new Random(seed);

        // Инициализация точек
        pointsX = new float[POINT_COUNT];
        pointsY = new float[POINT_COUNT];

        for (int i = 0; i < POINT_COUNT; i++) {
            pointsX[i] = rand.nextFloat();
            pointsY[i] = rand.nextFloat();
        }

        // Инициализация перестановок
        permutation = new int[POINT_COUNT * 2];
        int[] p = new int[POINT_COUNT];
        for (int i = 0; i < POINT_COUNT; i++) {
            p[i] = i;
        }

        // Перемешивание
        for (int i = 0; i < POINT_COUNT; i++) {
            int j = rand.nextInt(POINT_COUNT - i) + i;
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
            permutation[i] = permutation[i + POINT_COUNT] = p[i];
        }
    }

    public float evaluate(float x, float y) {
        // Определяем ячейку
        int xi = (int)x;
        int yi = (int)y;

        // Дробные части
        float xf = x - xi;
        float yf = y - yi;

        // Находим ближайшую точку
        float minDist = Float.MAX_VALUE;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cellX = (xi + dx) & (POINT_COUNT - 1);
                int cellY = (yi + dy) & (POINT_COUNT - 1);

                int index = permutation[permutation[cellX] + cellY];
                float vecX = dx - xf + pointsX[index];
                float vecY = dy - yf + pointsY[index];

                float dist = vecX * vecX + vecY * vecY;

                if (dist < minDist) {
                    minDist = dist;
                }
            }
        }

        // Нормализуем расстояние
        return (float)Math.sqrt(minDist) * 0.7f; // Эмпирически подобранный коэффициент
    }
}
