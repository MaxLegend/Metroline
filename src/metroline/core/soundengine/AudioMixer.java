package metroline.core.soundengine;

import metroline.util.MetroLogger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Менеджер аудио для воспроизведения звуков в отдельном потоке
 */
public class AudioMixer implements Runnable {

    private final BlockingQueue<AudioTask> audioQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread audioThread;
    private SourceDataLine outputLine;
    private final AudioFormat outputFormat;

    public AudioMixer(AudioFormat format) throws LineUnavailableException {
        this.outputFormat = format;
        setupOutputLine();
    }
    /**
     * Пишет данные напрямую в аудио-линию.
     * Использовать ТОЛЬКО из EDT или для коротких звуков.
     */
    public void writeToLine(byte[] buffer, int offset, int length) {
        if (outputLine != null && outputLine.isOpen()) {
            outputLine.write(buffer, offset, length);
        }
    }
    private void setupOutputLine() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, outputFormat);
        outputLine = (SourceDataLine) javax.sound.sampled.AudioSystem.getLine(info);
        outputLine.open(outputFormat);
    }

    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            audioThread = new Thread(this, "Audio Manager Thread");
            audioThread.setDaemon(true); // Важно: daemon thread!
            outputLine.start();
            audioThread.start();
            MetroLogger.logInfo("Audio Manager started");
        }
    }

    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            // Сигнализируем о остановке
            audioQueue.offer(new AudioTask(null, 0, false));

            try {
                if (audioThread != null) {
                    audioThread.join(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            outputLine.drain();
            outputLine.close();
            MetroLogger.logInfo("Audio Manager stopped");
        }
    }

    public void playSound(Sound sound, float volume, boolean loop) {
        if (sound != null && isRunning.get()) {
            SoundSource source = new SoundSource(sound, loop);
            source.setVolume(volume);
            source.play();
            playSoundSource(source, volume, loop); // <-- используем уже существующий метод
        }
    }

    public void playSound(Sound sound) {
        playSound(sound, 1.0f, false);
    }

    public void playSound(Sound sound, float volume) {
        playSound(sound, volume, false);
    }

    public void stopAllSounds() {
        audioQueue.clear();
    }

    @Override
    public void run() {
        MetroLogger.logInfo("Audio thread started with format: " + outputFormat);

        while (isRunning.get()) {
            try {
                AudioTask task = audioQueue.take();

                if (task.sound == null && task.source == null) {
                    continue; // команда остановки
                }

                if (task.sound != null) {
                    playSoundTask(task); // старый способ — можно удалить позже
                } else if (task.source != null) {
                    playSoundSourceTask(task); // <-- НОВЫЙ метод
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error playing audio: " + e.getMessage());
                e.printStackTrace();
            }
        }

        audioQueue.clear();
        MetroLogger.logInfo("Audio thread finished");
    }
    private void playSoundSourceTask(AudioTask task) {
        SoundSource source = task.source;
        source.setVolume(task.volume);
        source.setLooping(task.loop);
        source.play();

        short[] buffer = new short[4096];
        boolean playing = true;

        while (playing && isRunning.get()) {
            int samplesRead = source.read(buffer, buffer.length);
            if (samplesRead > 0) {
                byte[] byteBuffer = SoundEngineUtils.shortsToBytes(buffer);
                outputLine.write(byteBuffer, 0, samplesRead * 2);
            } else {
                playing = source.isPlaying(); // важно: если loop=true, source может снова стать playing
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    private void playStreamingSource(StreamingSoundSource source, float volume, boolean loop) {
        source.setVolume(volume);
        source.setLooping(loop);
        source.play();

        short[] buffer = new short[4096];
        boolean playing = true;

        while (playing && isRunning.get()) {
            int samplesRead = source.read(buffer, buffer.length);
            if (samplesRead > 0) {
                byte[] byteBuffer = SoundEngineUtils.shortsToBytes(buffer);
                outputLine.write(byteBuffer, 0, samplesRead * 2);
            } else {
                playing = source.isPlaying(); // Проверяем, может stream еще декодирует
            }

            try { Thread.sleep(1); } catch (InterruptedException e) { break; }
        }
    }
    private void playSoundTask(AudioTask task) {
        Sound sound = task.sound;
        short[] pcmData = sound.getPcmData();
        int position = 0; // позиция в samples
        boolean playing = true;
        int bufferSize = 2048; // Должно быть кратно количеству каналов
        if (sound.getFormat().getChannels() == 2) {
            bufferSize = 2048; // 1024 стерео-фрейма (2048 samples)
        }
        MetroLogger.logInfo("Playing sound: " + pcmData.length + " samples");

        while (playing && isRunning.get()) {
            int samplesToWrite = Math.min(bufferSize, pcmData.length - position);

            if (samplesToWrite <= 0) {
                if (task.loop) {
                    position = 0;
                    samplesToWrite = Math.min(2048, pcmData.length);
                } else {
                    playing = false;
                    break;
                }
            }

            if (samplesToWrite > 0) {
                try {
                    // samplesToWrite - количество SAMPLES, а не байт!
                    byte[] buffer = applyVolume(pcmData, position, samplesToWrite, task.volume);
                    outputLine.write(buffer, 0, buffer.length);
                    position += samplesToWrite; // Увеличиваем на количество samples
                } catch (Exception e) {
                    System.err.println("Error writing to audio line: " + e.getMessage());
                    playing = false;
                }
            }


        }
    }
    public void playSoundSource(SoundSource source, float volume, boolean loop) {
        if (source == null || !isRunning.get()) return;

        // Добавляем задачу в очередь — пусть фоновый поток её обработает
        audioQueue.offer(new AudioTask(source, volume, loop));
    }

    private byte[] applyVolume(short[] audioData, int offset, int samplesCount, float volume) {
        // samplesCount - количество samples (не байт!)
        // Для стерео: samplesCount должен быть четным числом
        byte[] result = new byte[samplesCount * 2]; // 2 байта на sample

        for (int i = 0; i < samplesCount; i++) {
            if (offset + i >= audioData.length) {
                break;
            }

            short original = audioData[offset + i];
            short adjusted = (short) (original * volume);

            // Little-endian
            result[i * 2] = (byte) (adjusted & 0xFF);
            result[i * 2 + 1] = (byte) ((adjusted >> 8) & 0xFF);
        }

        return result;
    }

    public int getQueueSize() {
        return audioQueue.size();
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Внутренний класс для представления аудио-задачи
     */
    private static class AudioTask {
        final Sound sound;          // для старого API (можно удалить позже)
        final SoundSource source;   // <-- НОВОЕ поле
        final float volume;
        final boolean loop;

        // Конструктор для SoundSource
        AudioTask(SoundSource source, float volume, boolean loop) {
            this.sound = null;
            this.source = source;
            this.volume = Math.max(0, Math.min(1, volume));
            this.loop = loop;
        }
    }
}
