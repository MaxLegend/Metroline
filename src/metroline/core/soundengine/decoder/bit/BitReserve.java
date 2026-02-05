/**
 * ------------Commemorative plaque Legacy Programmers ------------
 * 11/19/04			1.0 moved to LGPL.
 *
 * 12/12/99 0.0.7	Implementation stores single bits
 *					as ints for better performance. mdm@techie.com.
 *
 * 02/28/99 0.0     Java Conversion by E.B, javalayer@javazoom.net
 *
 *                  Adapted from the public c code by Jeff Tsay.
 * ------------Commemorative plaque Legacy Programmers ------------
 *
 * THIS CLASS FULLY SAFE REFACTORING BY M.A. Vavaev (@Tesmio) in 09 Sep 2025
 *
 */

package metroline.core.soundengine.decoder.bit;

/**
 * Implementation of Bit Reservoir for Layer III.
 * <p>
 * The implementation stores single bits as a word in the buffer. If
 * a bit is set, the corresponding word in the buffer will be non-zero.
 * If a bit is clear, the corresponding word is zero. Although this
 * may seem wasteful, this can be a factor of two quicker than
 * packing 8 bits to a byte and extracting.
 * <p>
 * Buffer size is fixed and must be power of two for efficient modulus via masking.
 *
 *
 */

/**
 *=======================================================================*
 THIS CLASS FULLY SAFE REFACTORING BY M.A. Vavaev (@Tesmio) 09 Sep 2025
 *=======================================================================*
 */

public final class BitReserve {

	/**
	 * Size of the internal buffer to store the reserved bits.
	 * Must be a power of 2. Each bit is stored as a single int entry (0 or non-zero).
	 */
	private static final int BUFFER_SIZE = 4096 * 8;

	/**
	 * Mask to implement fast modulus operation for buffer indexing.
	 */
	private static final int BUFFER_MASK = BUFFER_SIZE - 1;

	/**
	 * Write position in the buffer (next position to write a bit).
	 */
	private int writeOffset;

	/**
	 * Total number of bits read so far.
	 */
	private int totalBitsRead;

	/**
	 * Read position in the buffer (next position to read a bit).
	 */
	private int readOffset;

	/**
	 * Internal buffer storing bits as ints (0 = bit clear, non-zero = bit set).
	 */
	private final int[] buffer = new int[BUFFER_SIZE];

	/**
	 * Constructs a new BitReserve with initial state.
	 */
	public BitReserve() {
		this.writeOffset = 0;
		this.totalBitsRead = 0;
		this.readOffset = 0;
	}

	// ==================== NEW MODERN METHODS ====================

	/**
	 * Returns the total number of bits read from the reservoir so far.
	 *
	 * @return total bits read
	 */
	public int getTotalBitsRead() {
		return totalBitsRead;
	}

	/**
	 * Reads N bits from the reservoir and returns them as an integer.
	 * Bits are read in big-endian order (first bit becomes MSB).
	 *
	 * @param n number of bits to read (must be >= 0)
	 * @return integer value composed of the next N bits
	 */
	public int readBits(int n) {
		totalBitsRead += n;
		int value = 0;

		int pos = readOffset;
		if (pos + n < BUFFER_SIZE) {
			// Fast path: no wraparound
			for (int i = 0; i < n; i++) {
				value = (value << 1) | (buffer[pos++] != 0 ? 1 : 0);
			}
		} else {
			// Slow path: wraparound using mask
			for (int i = 0; i < n; i++) {
				value = (value << 1) | (buffer[pos] != 0 ? 1 : 0);
				pos = (pos + 1) & BUFFER_MASK;
			}
		}
		readOffset = pos;
		return value;
	}

	/**
	 * Reads a single bit from the reservoir.
	 *
	 * @return 1 if bit is set, 0 if bit is clear
	 */
	public int readBit() {
		totalBitsRead++;
		int bit = buffer[readOffset];
		readOffset = (readOffset + 1) & BUFFER_MASK;
		return bit;
	}

	/**
	 * Writes a byte (8 bits) into the reservoir.
	 * Each bit is stored as a separate int in the buffer.
	 *
	 * @param byteValue the byte to write (only lowest 8 bits are used)
	 */
	public void writeByte(int byteValue) {
		int ofs = writeOffset;
		buffer[ofs++] = byteValue & 0x80; // bit 7
		buffer[ofs++] = byteValue & 0x40; // bit 6
		buffer[ofs++] = byteValue & 0x20; // bit 5
		buffer[ofs++] = byteValue & 0x10; // bit 4
		buffer[ofs++] = byteValue & 0x08; // bit 3
		buffer[ofs++] = byteValue & 0x04; // bit 2
		buffer[ofs++] = byteValue & 0x02; // bit 1
		buffer[ofs++] = byteValue & 0x01; // bit 0

		writeOffset = (ofs == BUFFER_SIZE) ? 0 : ofs;
	}

	/**
	 * Rewinds the read position by N bits.
	 *
	 * @param n number of bits to rewind
	 */
	public void rewindBits(int n) {
		totalBitsRead -= n;
		readOffset -= n;
		if (readOffset < 0) {
			readOffset += BUFFER_SIZE;
		}
	}

	/**
	 * Rewinds the read position by N bytes (i.e., N * 8 bits).
	 *
	 * @param n number of bytes to rewind
	 */
	public void rewindBytes(int n) {
		int bits = n << 3; // n * 8
		totalBitsRead -= bits;
		readOffset -= bits;
		if (readOffset < 0) {
			readOffset += BUFFER_SIZE;
		}
	}

	// ==================== DEPRECATED COMPATIBILITY WRAPPERS ====================

	/**
	 * @deprecated Use {@link #getTotalBitsRead()} instead.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public int hsstell() {
		return getTotalBitsRead();
	}

	/**
	 * @deprecated Use {@link #readBits(int)} instead.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public int hgetbits(int N) {
		return readBits(N);
	}

	/**
	 * @deprecated Use {@link #readBit()} instead.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public int hget1bit() {
		return readBit();
	}

	/**
	 * @deprecated Use {@link #writeByte(int)} instead.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public void hputbuf(int val) {
		writeByte(val);
	}

	/**
	 * @deprecated Use {@link #rewindBits(int)} instead.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public void rewindNbits(int N) {
		rewindBits(N);
	}

	/**
	 * @deprecated Use {@link #rewindBytes(int)} instead.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public void rewindNbytes(int N) {
		rewindBytes(N);
	}
}