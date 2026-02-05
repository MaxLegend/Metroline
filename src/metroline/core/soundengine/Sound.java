package metroline.core.soundengine;


import metroline.core.soundengine.decoder.bit.Bitstream;
import metroline.core.soundengine.decoder.exceptions.BitstreamException;
import metroline.core.soundengine.decoder.exceptions.DecoderException;
import metroline.core.soundengine.decoder.exceptions.JavaLayerException;
import metroline.core.soundengine.decoder.header.Decoder;
import metroline.core.soundengine.decoder.header.Header;
import metroline.core.soundengine.decoder.output.OutputBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Класс, представляющий загруженный и декодированный звук.
 */
public class Sound {

    private short[] pcmData; // Декодированные PCM-данные как short[]
    private AudioFormat format; // Формат аудио

    public Sound(InputStream inputStream) throws IOException, JavaLayerException, UnsupportedAudioFileException {
        this(inputStream, null);
    }

    public Sound(short[] pcmData, AudioFormat format) {
        this.pcmData = pcmData;
        this.format = format;
    }

    // И приватный конструктор для конвертации:
    private Sound() {
        this.pcmData = null;
        this.format = null;
    }
    public Sound(InputStream inputStream, AudioFormat targetFormat) throws IOException, JavaLayerException, UnsupportedAudioFileException {
        // Определяем, WAV это или MP3 по сигнатуре (первые 4 байта)
        inputStream = new java.io.BufferedInputStream(inputStream);
        inputStream.mark(4);
        byte[] header = new byte[4];
        int read = inputStream.read(header);
        inputStream.reset();

        if (read == 4 && isWavHeader(header)) {
            loadFromWav(inputStream, targetFormat);
        } else {
            loadFromMp3(inputStream, targetFormat);
        }
    }
    private void loadFromWav(InputStream inputStream, AudioFormat targetFormat) throws IOException, UnsupportedAudioFileException {
        javax.sound.sampled.AudioInputStream audioStream = javax.sound.sampled.AudioSystem.getAudioInputStream(inputStream);

        AudioFormat sourceFormat = audioStream.getFormat();
        byte[] audioBytes = inputStreamToByteArray(audioStream);
        audioStream.close();

        short[] rawData = SoundEngineUtils.bytesToShorts(audioBytes);

        if (targetFormat != null && !sourceFormat.matches(targetFormat)) {
            this.pcmData = SoundEngineUtils.convertAudioFormat(rawData, sourceFormat, targetFormat);
            this.format = targetFormat;
        } else {
            this.pcmData = rawData;
            this.format = sourceFormat;
        }
    }

    private byte[] inputStreamToByteArray(java.io.InputStream is) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
    private void loadFromMp3(InputStream inputStream, AudioFormat targetFormat) throws IOException, JavaLayerException {
        ByteArrayOutputStream pcmOutputStream = new ByteArrayOutputStream();
        Bitstream bitstream = new Bitstream(inputStream);
        Decoder decoder = new Decoder();

        AudioFormat decodedFormat = null;
        boolean firstFrame = true;

        try {
            CustomOutputBuffer outputBuffer = null;

            while (true) {
                Header frameHeader = bitstream.readFrame();
                if (frameHeader == null) break;

                int sampleRate = frameHeader.frequency();
                int channels = (frameHeader.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;

                if (firstFrame) {
                    decodedFormat = new AudioFormat(
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
                    firstFrame = false;
                }

                try {
                    decoder.decodeFrame(frameHeader, bitstream);
                    byte[] frameData = outputBuffer.getBuffer();
                    pcmOutputStream.write(frameData, 0, outputBuffer.getBufferIndex());
                    outputBuffer.reset();
                } catch (DecoderException e) {
                    throw new JavaLayerException("Ошибка декодирования кадра", e);
                }
                bitstream.closeFrame();
            }

            byte[] byteData = pcmOutputStream.toByteArray();
            short[] rawData = SoundEngineUtils.bytesToShorts(byteData);

            if (targetFormat != null && !decodedFormat.matches(targetFormat)) {
                this.pcmData = SoundEngineUtils.convertAudioFormat(rawData, decodedFormat, targetFormat);
                this.format = targetFormat;
            } else {
                this.pcmData = rawData;
                this.format = decodedFormat;
            }

        } catch (BitstreamException e) {
            throw new IOException("Ошибка чтения битового потока MP3", e);
        } finally {
            try { bitstream.close(); } catch (BitstreamException e) {}
        }
    }
    private boolean isWavHeader(byte[] header) {
        // WAV начинается с "RIFF"
        return header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F';
    }
    public short[] getPcmData() {
        return pcmData;
    }

    public byte[] getPcmDataAsBytes() {
        return SoundEngineUtils.shortsToBytes(pcmData);
    }

    public AudioFormat getFormat() {
        return format;
    }

    public int getSampleCount() {
        return pcmData.length;
    }

    public int getDataLength() {
        return pcmData.length * 2; // 2 байта на sample
    }

    private static class CustomOutputBuffer extends OutputBuffer {
        private final byte[] buffer;
        private final int channels;
        private int[] channelSampleCount; // сколько сэмплов уже записано для каждого канала
        private int totalSamples; // общее количество записанных фреймов (пар L+R)

        public CustomOutputBuffer(int numChannels, boolean isBigEndian) {
            this.channels = numChannels;
            this.channelSampleCount = new int[channels];
            this.totalSamples = 0;
            // Выделяем буфер под максимум: OBUFFERSIZE сэмплов на канал * каналы * 2 байта
            this.buffer = new byte[OBUFFERSIZE * channels * 2];
        }

        @Override
        public void append(int channel, short value) {
            // Проверяем, что канал валидный
            if (channel < 0 || channel >= channels) {
                return;
            }

            // Получаем текущий индекс сэмпла для этого канала
            int sampleIndex = channelSampleCount[channel];

            // Рассчитываем позицию в interleaved буфере: sampleIndex * channels + channel
            int index = sampleIndex * channels + channel;
            int byteIndex = index * 2;

            if (byteIndex + 1 >= buffer.length) {
                return; // Буфер переполнен
            }

            // Записываем little-endian
            buffer[byteIndex] = (byte) (value & 0xFF);
            buffer[byteIndex + 1] = (byte) ((value >> 8) & 0xFF);

            // Увеличиваем счётчик сэмплов для этого канала
            channelSampleCount[channel]++;

            // Обновляем общее количество фреймов (минимальное среди каналов)
            int minSamples = channelSampleCount[0];
            for (int i = 1; i < channels; i++) {
                if (channelSampleCount[i] < minSamples) {
                    minSamples = channelSampleCount[i];
                }
            }
            totalSamples = minSamples;
        }

        @Override
        public void appendSamples(int channel, float[] f) {
            for (float sample : f) {
                append(channel, clip(sample));
            }
        }
        @Override public void writeBuffer(int val) {}
        @Override public void close() {}
        @Override public void clearBuffer() {}
        @Override
        public void setStopFlag() {}

        public byte[] getBuffer() {
            return buffer;
        }

        public int getBufferIndex() {
            // Возвращаем количество байт: фреймы * каналы * 2
            return totalSamples * channels * 2;
        }

        public int getSamplesDecoded() {
            return totalSamples; // количество полных фреймов (L+R пар)
        }

        public void reset() {
            for (int i = 0; i < channels; i++) {
                channelSampleCount[i] = 0;
            }
            totalSamples = 0;
        }
    }
}
