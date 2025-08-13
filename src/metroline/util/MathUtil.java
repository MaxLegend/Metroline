package metroline.util;

public class MathUtil {
    public static float round(float value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        float factor = (float) Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }
}
