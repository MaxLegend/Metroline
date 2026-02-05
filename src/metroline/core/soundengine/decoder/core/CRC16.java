/*
 * 11/19/04 : 1.0 moved to LGPL.
 *
 * 02/12/99 : Java Conversion by E.B , javalayer@javazoom.net
 *
 *  @(#) crc.h 1.5, last edit: 6/15/94 16:55:32
 *  @(#) Copyright (C) 1993, 1994 Tobias Bading (bading@cs.tu-berlin.de)
 *  @(#) Berlin University of Technology
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

package metroline.core.soundengine.decoder.core;

/**
 *=======================================================================*
 THIS CLASS FULLY SAFE REFACTORING BY M.A. Vavaev (@Tesmio) 09 Sep 2025
 *=======================================================================*
 */

/**
 * Computes 16-bit CRC checksums using the polynomial 0x8005 (CRC-16-IBM).
 * <p>
 * This class follows the MPEG audio CRC computation standard.
 * Each instance maintains its own CRC state. After calling {@link #checksum()},
 * the internal state is reset to {@code 0xFFFF}.
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> Not thread-safe. Each thread should use its own instance.
 * </p>
 */
public final class CRC16 {

    /**
     * CRC-16 polynomial: x^16 + x^15 + x^2 + 1 (0x8005).
     */
    private static final short POLYNOMIAL = (short) 0x8005;

    /**
     * Current CRC value. Initialized to 0xFFFF.
     */
    private short crc;

    /**
     * Constructs a new Crc16 instance with initial CRC value 0xFFFF.
     */
    public CRC16() {
        this.crc = (short) 0xFFFF;
    }

    /**
     * Feeds a bitstring into the CRC calculation.
     * <p>
     * <strong>Deprecated:</strong> Use {@link #addBits(int, int)} for modern naming.
     * Preserved for backward compatibility.
     * </p>
     *
     * @param bitstring the integer containing bits to process (LSB-aligned)
     * @param length    number of bits to process (must satisfy: 0 < length <= 32)
     * @deprecated Use {@link #addBits(int, int)}
     */
    @Deprecated
    public void add_bits(int bitstring, int length) {
        addBits(bitstring, length);
    }

    /**
     * Feeds a bitstring into the CRC calculation.
     * <p>
     * Processes the <code>length</code> least significant bits of <code>bitstring</code>,
     * starting from the highest bit position within those <code>length</code> bits.
     * </p>
     * <p>
     * Example: if bitstring=0b1101 and length=4, bits are processed in order: 1, 1, 0, 1.
     * </p>
     *
     * @param bitstring the integer containing bits to process (LSB-aligned)
     * @param length    number of bits to process (must satisfy: 0 < length <= 32)
     */
    public void addBits(int bitstring, int length) {
        // Start with the highest bit within the specified length
        int bitmask = 1 << (length - 1);

        do {
            // XOR condition: if top CRC bit and current input bit differ
            if (((this.crc & 0x8000) == 0) ^ ((bitstring & bitmask) == 0)) {
                this.crc <<= 1;
                this.crc ^= POLYNOMIAL;
            } else {
                this.crc <<= 1;
            }
        } while ((bitmask >>>= 1) != 0);
    }

    /**
     * Returns the current CRC checksum and resets internal state to 0xFFFF.
     *
     * @return the 16-bit CRC checksum
     */
    public short checksum() {
        short sum = this.crc;
        this.crc = (short) 0xFFFF;
        return sum;
    }
}