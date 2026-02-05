package metroline.core.soundengine;

/**
 * Представляет собой проигрываемый экземпляр звука.
 * Управляет позицией воспроизведения, громкостью и состоянием.
 */

import metroline.util.MetroLogger;

/**
 * Представляет собой проигрываемый экземпляр звука.
 * Работает с short[] данными для эффективности.
 */
public class SoundSource {

    private final Sound sound;
    private int samplePosition; // Текущая позиция в samples
    private boolean playing;
    private boolean loop;
    private float volume;

    public SoundSource(Sound sound, boolean loop) {
        this.sound = sound;
        this.samplePosition = 0;
        this.playing = false;
        this.loop = loop;
        this.volume = 1.0f;
    }

    public SoundSource(Sound sound) {
        this(sound, false);
    }

    /**
     * Проигрывает звук, заполняя буфер микшера данными.
     */
    /**
     * Проигрывает звук, заполняя буфер микшера данными.
     * Возвращает количество прочитанных samples (не bytes!)
     */
    public int read(short[] outputBuffer, int samplesRequested) {
        if (!playing) return 0;

        short[] sourceData = sound.getPcmData();
        if (sourceData == null || sourceData.length == 0) {
            playing = false;
            return 0;
        }

        // Защита от переполнения
        if (samplePosition >= sourceData.length) {
            if (loop) {
                samplePosition = 0;
            } else {
                playing = false;
                MetroLogger.logInfo("SoundSource stopped: reached end, loop=false, key=" + sound.hashCode());
                return 0;
            }
        }

        int samplesToRead = Math.min(samplesRequested, sourceData.length - samplePosition);

        if (samplesToRead <= 0) {
            if (loop) {
                samplePosition = 0;
                samplesToRead = Math.min(samplesRequested, sourceData.length);
            } else {
                playing = false;
                MetroLogger.logInfo("SoundSource stopped: samplesToRead <= 0, key=" + sound.hashCode());
                return 0;
            }
        }

        // Копируем и применяем громкость
        for (int i = 0; i < samplesToRead; i++) {
            if (samplePosition + i >= sourceData.length) {
                // Экстренная остановка
                playing = false;
                MetroLogger.logWarning("SoundSource buffer overrun! Stopping. key=" + sound.hashCode());
                return i; // возвращаем, сколько успели прочитать
            }
            short sample = sourceData[samplePosition + i];
            outputBuffer[i] = (short) (sample * volume);
        }

        samplePosition += samplesToRead;

        // Дополнительная защита: если samplePosition вышла за пределы — останавливаем
        if (samplePosition >= sourceData.length) {
            if (!loop) {
                playing = false;
                MetroLogger.logInfo("SoundSource stopped: end of data, key=" + sound.hashCode());
            }
        }

        return samplesToRead;
    }


    public void play() {
        this.playing = true;
        this.samplePosition = 0;
    }

    public void stop() {
        this.playing = false;
        this.samplePosition = 0;
        MetroLogger.logInfo("SoundSource STOPPED: " + this);
    }

    public void pause() {
        this.playing = false;
    }
    public void resume() {
        this.playing = true;
    }
    public void setLooping(boolean loop) {
        this.loop = loop;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public float getVolume() {
        return volume;
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getSamplePosition() {
        return samplePosition;
    }

    public int getTotalSamples() {
        return sound.getSampleCount();
    }
}
