/*
 * 11/19/04  1.0 moved to LGPL.
 * 12/12/99  Added appendSamples() method for efficiency. MDM.
 * 15/02/99 ,Java Conversion by E.B ,ebsp@iname.com, JavaLayer
 *
 *   Declarations for output buffer, includes operating system
 *   implementation of the virtual Obuffer. Optional routines
 *   enabling seeks and stops added by Jeff Tsay.
 *
 *  @(#) obuffer.h 1.8, last edit: 6/15/94 16:51:56
 *  @(#) Copyright (C) 1993, 1994 Tobias Bading (bading@cs.tu-berlin.de)
 *  @(#) Berlin University of Technology
 *
 *  Idea and first implementation for u-law output with fast downsampling by
 *  Jim Boucher (jboucher@flash.bu.edu)
 *
 *  LinuxObuffer class written by
 *  Louis P. Kruger (lpkruger@phoenix.princeton.edu)
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
 * Base class for audio output buffers.
 * <p>
 * Subclasses must implement methods to handle sample output, buffer management,
 * and stream control. This class provides common utility methods like sample clipping
 * and bulk sample appending.
 * </p>
 */
public abstract class OutputBuffer {

    /**
     * Maximum buffer size: 2 channels × 1152 samples per frame.
     */
    public static final int OBUFFERSIZE = 2 * 1152;

    /**
     * Maximum number of supported audio channels.
     */
    public static final int MAXCHANNELS = 2;

    /**
     * Appends a single 16-bit PCM sample to the specified channel.
     *
     * @param channel the target channel (0 = left, 1 = right, etc.)
     * @param value   the 16-bit PCM sample value
     */
    public abstract void append(int channel, short value);

    /**
     * Accepts 32 new PCM samples for the specified channel.
     * <p>
     * This default implementation calls {@link #append(int, short)} for each sample
     * after clipping it to 16-bit range.
     * </p>
     *
     * @param channel the target channel
     * @param samples array of 32 floating-point samples
     */
    public void appendSamples(int channel, float[] samples) {
        for (int i = 0; i < 32; i++) {
            short clipped = clip(samples[i]);
            append(channel, clipped);
        }
    }

    /**
     * Clips a floating-point sample value to the 16-bit signed integer range [-32768, 32767].
     *
     * @param sample the input sample value
     * @return the clipped 16-bit sample as a {@code short}
     */
    protected static short clip(float sample) {
        if (sample > 32767.0f) {
            return 32767;
        } else if (sample < -32768.0f) {
            return -32768;
        } else {
            return (short) sample;
        }
    }

    // ================ СОВМЕСТИМОСТЬ: старые имена методов как обертки =================

    /**
     * Writes buffered samples to output (file or audio hardware).
     * <p>
     * <strong>Deprecated:</strong> Use {@link #writeBuffer(int)} instead.
     * This method remains for backward compatibility.
     * </p>
     *
     * @param val parameter whose meaning is implementation-specific
     * @deprecated Use {@link #writeBuffer(int)}
     */
    @Deprecated
    public void write_buffer(int val) {
        writeBuffer(val);
    }

    /**
     * Writes buffered samples to output (file or audio hardware).
     *
     * @param val parameter whose meaning is implementation-specific
     */
    public abstract void writeBuffer(int val);

    /**
     * Closes the output buffer and releases any associated resources.
     */
    public abstract void close();

    /**
     * Clears all data in the buffer (used for seeking).
     * <p>
     * <strong>Deprecated:</strong> Use {@link #clearBuffer()} instead.
     * This method remains for backward compatibility.
     * </p>
     *
     * @deprecated Use {@link #clearBuffer()}
     */
    @Deprecated
    public void clear_buffer() {
        clearBuffer();
    }

    /**
     * Clears all data in the buffer (used for seeking).
     */
    public abstract void clearBuffer();

    /**
     * Notifies the buffer that the user has stopped the stream.
     * <p>
     * <strong>Deprecated:</strong> Use {@link #setStopFlag()} instead.
     * This method remains for backward compatibility.
     * </p>
     *
     * @deprecated Use {@link #setStopFlag()}
     */
    @Deprecated
    public void set_stop_flag() {
        setStopFlag();
    }

    /**
     * Notifies the buffer that the user has stopped the stream.
     */
    public abstract void setStopFlag();
}