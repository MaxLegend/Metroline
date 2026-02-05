package metroline.core.soundengine;

import metroline.core.soundengine.decoder.bit.Bitstream;
import metroline.core.soundengine.decoder.exceptions.DecoderException;
import metroline.core.soundengine.decoder.header.Decoder;
import metroline.core.soundengine.decoder.header.Header;
import metroline.core.soundengine.decoder.output.OutputBuffer;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Потоковый источник звука для декодирования MP3 на лету.
 */
public class StreamingSoundSource implements Runnable {

    private final InputStream inputStream;
    private final AudioFormat targetFormat;
    private final BlockingQueue<short[]> audioBufferQueue;
    private final AtomicBoolean isRunning;
    private Thread decoderThread;
    private AudioFormat sourceFormat;

    private int samplePosition;
    private short[] currentBuffer;
    private int currentBufferPosition;
    private boolean playing;
    private boolean loop;
    private float volume;

    public StreamingSoundSource(InputStream inputStream, AudioFormat targetFormat) throws IOException {
        this.inputStream = inputStream;
        this.targetFormat = targetFormat;
        this.audioBufferQueue = new ArrayBlockingQueue<>(10); // Буфер на 10 кадров
        this.isRunning = new AtomicBoolean(false);
        this.volume = 1.0f;
        this.playing = false;
    }

    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            decoderThread = new Thread(this, "MP3 Stream Decoder");
            decoderThread.setDaemon(true);
            decoderThread.start();
        }
    }

    public void stop() {
        isRunning.set(false);
        audioBufferQueue.clear();
        if (decoderThread != null) {
            try {
                decoderThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        try {
            Bitstream bitstream = new Bitstream(inputStream);
            Decoder decoder = new Decoder();
            CustomOutputBuffer outputBuffer = null;

            while (isRunning.get()) {
                Header frameHeader = bitstream.readFrame();
                if (frameHeader == null) {
                    if (loop) {
                        bitstream.close();
                        inputStream.reset();
                        bitstream = new Bitstream(inputStream);
                        continue;
                    } else {
                        break;
                    }
                }

                int sampleRate = frameHeader.frequency();
                int channels = (frameHeader.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;

                if (sourceFormat == null) {
                    sourceFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            sampleRate,
                            16,
                            channels,
                            channels * 2,
                            sampleRate,
                            false
                    );
                    outputBuffer = new CustomOutputBuffer(channels, false);
                    decoder.setOutputBuffer(outputBuffer);
                }

                try {
                    decoder.decodeFrame(frameHeader, bitstream);
                    byte[] frameBytes = outputBuffer.getBuffer();
                    short[] frameShorts = SoundEngineUtils.bytesToShorts(frameBytes);

                    // Конвертируем формат если нужно
                    if (!sourceFormat.matches(targetFormat)) {
                        frameShorts = SoundEngineUtils.convertAudioFormat(frameShorts, sourceFormat, targetFormat);
                    }

                    // Добавляем в очередь для воспроизведения
                    audioBufferQueue.put(frameShorts);
                    outputBuffer.reset();

                } catch (DecoderException | InterruptedException e) {
                    break;
                }
                bitstream.closeFrame();
            }

            bitstream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int read(short[] mixerBuffer, int samplesRequested) {
        if (!playing) return 0;

        int samplesRead = 0;
        while (samplesRead < samplesRequested && isRunning.get()) {
            if (currentBuffer == null || currentBufferPosition >= currentBuffer.length) {
                try {
                    currentBuffer = audioBufferQueue.poll();
                    currentBufferPosition = 0;
                    if (currentBuffer == null) {
                        break; // Нет данных
                    }
                } catch (Exception e) {
                    break;
                }
            }

            int samplesToCopy = Math.min(currentBuffer.length - currentBufferPosition,
                    samplesRequested - samplesRead);

            for (int i = 0; i < samplesToCopy; i++) {
                mixerBuffer[samplesRead + i] = (short) (currentBuffer[currentBufferPosition + i] * volume);
            }

            samplesRead += samplesToCopy;
            currentBufferPosition += samplesToCopy;
        }

        return samplesRead;
    }

    public void play() {
        this.playing = true;
        this.samplePosition = 0;
        start();
    }

    public void stopPlayback() {
        this.playing = false;
        stop();
    }

    public void setLooping(boolean loop) {
        this.loop = loop;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public boolean isPlaying() {
        return playing;
    }

    // Внутренний класс CustomOutputBuffer аналогичен классу из Sound
    private static class CustomOutputBuffer extends OutputBuffer {
        private final byte[] buffer;
        private int bufferIndex;

        public CustomOutputBuffer(int numChannels, boolean isBigEndian) {
            this.buffer = new byte[OBUFFERSIZE * 2];
            this.bufferIndex = 0;
        }

        @Override
        public void append(int channel, short value) {
            buffer[bufferIndex++] = (byte) (value & 0xFF);
            buffer[bufferIndex++] = (byte) ((value >> 8) & 0xFF);
        }

        @Override
        public void appendSamples(int channel, float[] f) {
            for (float sample : f) {
                append(channel, clip(sample));
            }
        }

        @Override
        public void write_buffer(int val) {}

        @Override
        public void writeBuffer(int val) {

        }

        @Override
        public void close() {}
        @Override
        public void clear_buffer() { reset(); }

        @Override
        public void clearBuffer() {

        }

        @Override
        public void set_stop_flag() {}

        @Override
        public void setStopFlag() {

        }

        public byte[] getBuffer() { return buffer; }
        public int getBufferIndex() { return bufferIndex; }

        public void reset() {
            bufferIndex = 0;
        }
    }
}
