/*
 * 11/19/04	 1.0 moved to LGPL.
 *
 * 12/12/99  Initial Version based on FileObuffer.	mdm@techie.com.
 *
 * FileObuffer:
 * 15/02/99  Java Conversion by E.B ,javalayer@javazoom.net
 *
 *-----------------------------------------------------------------------
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */

package metroline.core.soundengine.decoder.output;

/**
 *=======================================================================*
 THIS CLASS FULLY SAFE REFACTORING BY M.A. Vavaev (@Tesmio) 09 Sep 2025
 *=======================================================================*
 */

/**
 * A fixed-size output buffer that stores decoded PCM samples in a short array.
 * <p>
 * This implementation writes samples directly into an internal buffer for efficiency.
 * It supports multi-channel output by interleaving samples (e.g., LRLRLR... for stereo).
 * </p>
 * <p>
 * Note: The {@link #appendSamples(int, float[])} method is overridden to write directly
 * to the buffer without calling {@link #append(int, short)}, for performance reasons.
 * </p>
 */
public class SampleBuffer extends OutputBuffer {

    /**
     * Internal buffer storing 16-bit PCM samples.
     */
    private final short[] buffer;

    /**
     * Per-channel write pointers. Index indicates next write position for each channel.
     */
    private final int[] bufferp;

    /**
     * Number of audio channels (1 for mono, 2 for stereo).
     */
    private final int channels;

    /**
     * Sample rate in Hz (e.g., 44100, 48000).
     */
    private final int frequency;

    /**
     * Constructs a new SampleBuffer with specified sample rate and channel count.
     *
     * @param sampleFrequency    sample rate in Hz
     * @param numberOfChannels   number of channels (1 or 2)
     * @throws IllegalArgumentException if numberOfChannels > {@link OutputBuffer#MAXCHANNELS}
     */
    public SampleBuffer(int sampleFrequency, int numberOfChannels) {
        if (numberOfChannels > MAXCHANNELS) {
            throw new IllegalArgumentException("Unsupported channel count: " + numberOfChannels);
        }
        this.buffer = new short[OBUFFERSIZE];
        this.bufferp = new int[MAXCHANNELS]; // MAXCHANNELS = 2
        this.channels = numberOfChannels;
        this.frequency = sampleFrequency;

        // Initialize write pointers: for channel i, start at index i
        for (int i = 0; i < numberOfChannels; ++i) {
            this.bufferp[i] = i; // NOT (short)i — stored in int[], so cast is meaningless
        }
    }

    /**
     * Returns the number of audio channels.
     *
     * @return number of channels (1 or 2)
     */
    public int getChannelCount() {
        return this.channels;
    }

    /**
     * Returns the sample frequency in Hz.
     *
     * @return sample rate (e.g., 44100)
     */
    public int getSampleFrequency() {
        return this.frequency;
    }

    /**
     * Returns the internal sample buffer.
     * <p>
     * <strong>Warning:</strong> Modifying this buffer externally may corrupt output.
     * </p>
     *
     * @return the internal short array buffer
     */
    public short[] getBuffer() {
        return this.buffer;
    }

    /**
     * Returns the current write position for channel 0.
     * <p>
     * <strong>Note:</strong> This is not the buffer's total length, but the next write index
     * for the left channel (or mono channel). Useful for determining how many samples have been written.
     * </p>
     *
     * @return the current write pointer for channel 0
     */
    public int getBufferLength() {
        return this.bufferp[0];
    }

    /**
     * Appends a single 16-bit PCM sample to the specified channel.
     * <p>
     * Samples are written in interleaved format (e.g., for stereo: L, R, L, R...).
     * </p>
     *
     * @param channel the target channel (0 = left/mono, 1 = right)
     * @param value   the 16-bit PCM sample
     */
    @Override
    public void append(int channel, short value) {
        this.buffer[this.bufferp[channel]] = value;
        this.bufferp[channel] += this.channels;
    }

    /**
     * Accepts 32 floating-point samples and writes them directly to the buffer.
     * <p>
     * <strong>Performance Note:</strong> This method bypasses {@link #append(int, short)}
     * and writes directly to the buffer for efficiency. It clips samples to 16-bit range.
     * </p>
     *
     * @param channel the target channel
     * @param samples array of exactly 32 floating-point samples
     */
    @Override
    public void appendSamples(int channel, float[] samples) {
        int pos = this.bufferp[channel];
        final int step = this.channels;

        for (int i = 0; i < 32; i++) {
            float fs = samples[i];
            fs = (fs > 32767.0f) ? 32767.0f :
                    (fs < -32768.0f) ? -32768.0f : fs;
            this.buffer[pos] = (short) fs;
            pos += step;
        }

        this.bufferp[channel] = pos;
    }

    // ================ Compatibility =================

    /**
     * Writes buffered samples to output (stub implementation).
     * <p>
     * <strong>Deprecated:</strong> Use {@link #writeBuffer(int)} instead.
     * </p>
     *
     * @param val ignored
     * @deprecated Use {@link #writeBuffer(int)}
     */
    @Deprecated
    @Override
    public final void write_buffer(int val) {
        writeBuffer(val);
    }

    /**
     * Writes buffered samples to output (stub implementation — does nothing).
     *
     * @param val ignored
     */
    @Override
    public void writeBuffer(int val) {
        // Stub — no operation in this implementation
    }

    /**
     * Closes the buffer (stub implementation — does nothing).
     */
    @Override
    public void close() {
        // Stub — no resources to release
    }

    /**
     * Clears the buffer by resetting write pointers to start.
     * <p>
     * <strong>Deprecated:</strong> Use {@link #clearBuffer()} instead.
     * </p>
     *
     * @deprecated Use {@link #clearBuffer()}
     */
    @Deprecated
    @Override
    public final void clear_buffer() {
        clearBuffer();
    }

    /**
     * Clears the buffer by resetting write pointers to start.
     */
    @Override
    public void clearBuffer() {
        for (int i = 0; i < this.channels; ++i) {
            this.bufferp[i] = i;
        }
    }

    /**
     * Sets stop flag (stub implementation — does nothing).
     * <p>
     * <strong>Deprecated:</strong> Use {@link #setStopFlag()} instead.
     * </p>
     *
     * @deprecated Use {@link #setStopFlag()}
     */
    @Deprecated
    @Override
    public final void set_stop_flag() {
        setStopFlag();
    }

    /**
     * Sets stop flag (stub implementation — does nothing).
     */
    @Override
    public void setStopFlag() {
        // Stub — no operation
    }
}