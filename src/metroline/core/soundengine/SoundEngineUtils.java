package metroline.core.soundengine;

import javax.sound.sampled.AudioFormat;

/**
 * Утилиты для аудио конвертации и ресемплинга
 */
public class SoundEngineUtils {

    public static short[] convertToMono(short[] stereoData) {
        short[] monoData = new short[stereoData.length / 2];
        for (int i = 0; i < monoData.length; i++) {
            int left = stereoData[i * 2] & 0xFFFF;
            int right = stereoData[i * 2 + 1] & 0xFFFF;
            monoData[i] = (short) ((left + right) / 2);
        }
        return monoData;
    }

    public static short[] convertToStereo(short[] monoData) {
        short[] stereoData = new short[monoData.length * 2];
        for (int i = 0; i < monoData.length; i++) {
            stereoData[i * 2] = monoData[i];
            stereoData[i * 2 + 1] = monoData[i];
        }
        return stereoData;
    }

    public static short[] resample(short[] audioData, int originalSampleRate, int targetSampleRate) {
        if (originalSampleRate == targetSampleRate) {
            return audioData;
        }

        double ratio = (double) targetSampleRate / originalSampleRate;
        int newLength = (int) (audioData.length * ratio);
        short[] resampled = new short[newLength];

        for (int i = 0; i < newLength; i++) {
            double originalIndex = i / ratio;
            int index1 = (int) Math.floor(originalIndex);
            int index2 = Math.min(index1 + 1, audioData.length - 1);

            double fraction = originalIndex - index1;
            double sample1 = audioData[index1];
            double sample2 = audioData[index2];

            resampled[i] = (short) (sample1 + fraction * (sample2 - sample1));
        }

        return resampled;
    }

    public static short[] convertAudioFormat(short[] audioData, AudioFormat sourceFormat, AudioFormat targetFormat) {
        // Конвертируем каналы если нужно
        short[] converted = audioData;

        if (sourceFormat.getChannels() == 1 && targetFormat.getChannels() == 2) {
            converted = convertToStereo(converted);
        } else if (sourceFormat.getChannels() == 2 && targetFormat.getChannels() == 1) {
            converted = convertToMono(converted);
        }

        // Ресемплинг если нужно
        if ((int) sourceFormat.getSampleRate() != (int) targetFormat.getSampleRate()) {
            converted = resample(converted, (int) sourceFormat.getSampleRate(), (int) targetFormat.getSampleRate());
        }

        return converted;
    }

    public static short[] bytesToShorts(byte[] byteData) {
        short[] shortData = new short[byteData.length / 2];
        for (int i = 0; i < shortData.length; i++) {
            shortData[i] = (short) ((byteData[i * 2] & 0xFF) | ((byteData[i * 2 + 1] & 0xFF) << 8));
        }
        return shortData;
    }

    public static byte[] shortsToBytes(short[] shortData) {
        byte[] byteData = new byte[shortData.length * 2];
        for (int i = 0; i < shortData.length; i++) {
            byteData[i * 2] = (byte) (shortData[i] & 0xFF);
            byteData[i * 2 + 1] = (byte) ((shortData[i] >> 8) & 0xFF);
        }
        return byteData;
    }
}
