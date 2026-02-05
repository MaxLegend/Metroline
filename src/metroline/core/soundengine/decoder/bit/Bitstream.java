/*
 * ------------Commemorative plaque Legacy Programmers ------------
 * 11/19/04  1.0 moved to LGPL.
 * 
 * 11/17/04	 Uncomplete frames discarded. E.B, javalayer@javazoom.net 
 *
 * 12/05/03	 ID3v2 tag returned. E.B, javalayer@javazoom.net 
 *
 * 12/12/99	 Based on Ibitstream. Exceptions thrown on errors,
 *			 Temporary removed seek functionality. mdm@techie.com
 *
 * 02/12/99 : Java Conversion by E.B , javalayer@javazoom.net
 *
 * 04/14/97 : Added function prototypes for new syncing and seeking
 * mechanisms. Also made this file portable. Changes made by Jeff Tsay
 *
 *  @(#) ibitstream.h 1.5, last edit: 6/15/94 16:55:34
 *  @(#) Copyright (C) 1993, 1994 Tobias Bading (bading@cs.tu-berlin.de)
 *  @(#) Berlin University of Technology
 * ------------Commemorative plaque Legacy Programmers ------------
 */

 /**
  *=======================================================================*
  THIS CLASS FULLY SAFE REFACTORING BY M.A. Vavaev (@Tesmio) 09 Sep 2025
  *=======================================================================*
 */

package metroline.core.soundengine.decoder.bit;

import metroline.core.soundengine.decoder.interfaces.BitstreamErrors;
import metroline.core.soundengine.decoder.exceptions.BitstreamException;
import metroline.core.soundengine.decoder.core.CRC16;
import metroline.core.soundengine.decoder.header.Header;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * The <code>Bitstream</code> class is responsible for parsing
 * an MPEG audio bitstream.
 * <p>
 * <b>REVIEW:</b> much of the parsing currently occurs in the
 * various decoders. This should be moved into this class and associated
 * inner classes.
 */
public final class Bitstream implements BitstreamErrors {

	/**
	 * Synchronization control constant for the initial
	 * synchronization to the start of a frame.
	 */
	public static final byte INITIAL_SYNC = 0;

	/**
	 * Synchronization control constant for non-initial frame
	 * synchronizations.
	 */
	public static final byte STRICT_SYNC = 1;

	/**
	 * Maximum size of the frame buffer in integers (max 1730 bytes per frame).
	 */
	private static final int BUFFER_INT_SIZE = 433;

	/**
	 * The frame buffer that holds the data for the current frame as integers.
	 */
	private final int[] frameBuffer = new int[BUFFER_INT_SIZE];

	/**
	 * Number of valid bytes in the frame buffer.
	 */
	private int frameSize = -1;

	/**
	 * The bytes read from the stream for the current frame.
	 */
	private byte[] frameBytes = new byte[BUFFER_INT_SIZE * 4];

	/**
	 * Index into {@link #frameBuffer} where the next bits are retrieved.
	 */
	private int wordPointer = -1;

	/**
	 * Bit index (0-31, from MSB to LSB) for the next bit to read.
	 */
	private int bitIndex = -1;

	/**
	 * The current specified sync word.
	 */
	private int syncWord;

	/**
	 * Audio header position in stream (size of ID3v2 tag).
	 */
	private int headerPosition = 0;

	/**
	 * True if audio is in single channel mode.
	 */
	private boolean singleChannelMode;

	/**
	 * Bitmask for reading N bits: bitmask[n] = (1 << n) - 1.
	 */
	private static final int[] bitMask = {
			0x00000000, 0x00000001, 0x00000003, 0x00000007,
			0x0000000F, 0x0000001F, 0x0000003F, 0x0000007F,
			0x000000FF, 0x000001FF, 0x000003FF, 0x000007FF,
			0x00000FFF, 0x00001FFF, 0x00003FFF, 0x00007FFF,
			0x0000FFFF, 0x0001FFFF
	};

	/**
	 * Input source with pushback capability.
	 */
	private final PushbackInputStream source;

	/**
	 * Reusable header object.
	 */
	private final Header header = new Header();

	/**
	 * Buffer for sync checks (4 bytes).
	 */
	private final byte[] syncBuffer = new byte[4];

	/**
	 * CRC-16 calculator (optional).
	 */
	private final CRC16[] crc = new CRC16[1];

	/**
	 * Raw ID3v2 tag data (if present).
	 */
	private byte[] rawId3v2 = null;

	/**
	 * True if this is the first frame being read.
	 */
	private boolean isFirstFrame = true;

	/**
	 * Constructs a Bitstream that reads data from a given InputStream.
	 *
	 * @param in The InputStream to read from.
	 * @throws NullPointerException if in is null.
	 */
	public Bitstream(InputStream in) {
		if (in == null) throw new NullPointerException("in");
		in = new BufferedInputStream(in);
		loadID3v2(in);
		isFirstFrame = true;
		source = new PushbackInputStream(in, BUFFER_INT_SIZE * 4);
		closeFrame();
	}

	// ==================== NEW MODERN METHODS ====================

	/**
	 * Returns the position of the first audio header (i.e., size of ID3v2 tag).
	 *
	 * @return size of ID3v2 tag in bytes, or 0 if none.
	 */
	public int getHeaderPosition() {
		return headerPosition;
	}

	/**
	 * Loads ID3v2 tag from the beginning of the stream.
	 *
	 * @param in MP3 InputStream.
	 */
	private void loadID3v2(InputStream in) {
		int size = -1;
		try {
			in.mark(10);
			size = readID3v2Header(in);
			headerPosition = size;
		} catch (IOException ignored) {
		} finally {
			try {
				in.reset();
			} catch (IOException ignored) {
			}
		}

		if (size > 0) {
			try {
				rawId3v2 = new byte[size];
				in.read(rawId3v2, 0, rawId3v2.length);
			} catch (IOException ignored) {
			}
		}
	}

	/**
	 * Parses ID3v2 header to determine its total size.
	 *
	 * @param in MP3 InputStream.
	 * @return total size of ID3v2 tag including header, or -10 if not found.
	 * @throws IOException if reading fails.
	 */
	private int readID3v2Header(InputStream in) throws IOException {
		byte[] id3header = new byte[4];
		in.read(id3header, 0, 3);

		if (id3header[0] == 'I' && id3header[1] == 'D' && id3header[2] == '3') {
			in.read(id3header, 0, 3); // skip version & flags
			in.read(id3header, 0, 4); // read size bytes
			int size = ((id3header[0] & 0x7F) << 21) |
					((id3header[1] & 0x7F) << 14) |
					((id3header[2] & 0x7F) << 7) |
					(id3header[3] & 0x7F);
			return size + 10; // include 10-byte header
		}
		return -10;
	}

	/**
	 * Returns raw ID3v2 tag data as an InputStream, or null if not present.
	 *
	 * @return InputStream of ID3v2 data, or null.
	 */
	public InputStream getRawID3v2() {
		return rawId3v2 == null ? null : new ByteArrayInputStream(rawId3v2);
	}

	/**
	 * Closes the underlying input stream.
	 *
	 * @throws BitstreamException if an I/O error occurs.
	 */
	public void close() throws BitstreamException {
		try {
			source.close();
		} catch (IOException ex) {
			throw newBitstreamException(STREAM_ERROR, ex);
		}
	}

	/**
	 * Reads and parses the next frame from the input source.
	 *
	 * @return the Header describing the frame, or null if EOF.
	 * @throws BitstreamException on stream or frame errors.
	 */
	public Header readFrame() throws BitstreamException {
		Header result = null;
		try {
			result = readNextFrame();
			if (isFirstFrame) {
				result.parseVBR(frameBytes);
				isFirstFrame = false;
			}
		} catch (BitstreamException ex) {
			if (ex.getErrorCode() == INVALIDFRAME) {
				try {
					closeFrame();
					result = readNextFrame();
				} catch (BitstreamException e) {
					if (e.getErrorCode() != STREAM_EOF) {
						throw newBitstreamException(e.getErrorCode(), e);
					}
				}
			} else if (ex.getErrorCode() != STREAM_EOF) {
				throw newBitstreamException(ex.getErrorCode(), ex);
			}
		}
		return result;
	}

	/**
	 * Reads the next MP3 frame header.
	 *
	 * @return the frame header.
	 * @throws BitstreamException if frame cannot be read.
	 */
	private Header readNextFrame() throws BitstreamException {
		if (frameSize == -1) {
			nextFrame();
		}
		return header;
	}

	/**
	 * Advances to the next frame by reading its header.
	 *
	 * @throws BitstreamException if header read fails.
	 */
	private void nextFrame() throws BitstreamException {
		header.read_header(this, crc);
	}

	/**
	 * Pushes back the current frame bytes into the stream.
	 *
	 * @throws BitstreamException if unread fails.
	 */
	public void unreadFrame() throws BitstreamException {
		if (wordPointer == -1 && bitIndex == -1 && frameSize > 0) {
			try {
				source.unread(frameBytes, 0, frameSize);
			} catch (IOException ex) {
				throw newBitstreamException(STREAM_ERROR);
			}
		}
	}

	/**
	 * Resets frame state (closes current frame).
	 */
	public void closeFrame() {
		frameSize = -1;
		wordPointer = -1;
		bitIndex = -1;
	}

	/**
	 * Checks if the next 4 bytes represent a valid frame header.
	 *
	 * @param syncmode sync mode: {@link #INITIAL_SYNC} or {@link #STRICT_SYNC}
	 * @return true if sync mark is found.
	 * @throws BitstreamException if I/O error occurs.
	 */
	public boolean isSyncCurrentPosition(int syncmode) throws BitstreamException {
		int read = readBytes(syncBuffer, 0, 4);
		int headerString = ((syncBuffer[0] << 24) & 0xFF000000) |
				((syncBuffer[1] << 16) & 0x00FF0000) |
				((syncBuffer[2] << 8)  & 0x0000FF00) |
				((syncBuffer[3] << 0)  & 0x000000FF);

		try {
			source.unread(syncBuffer, 0, read);
		} catch (IOException ignored) {
		}

		return switch (read) {
			case 0 -> true;
			case 4 -> isSyncMark(headerString, syncmode, syncWord);
			default -> false;
		};
	}

	/**
	 * Reads bits from the current frame buffer.
	 *
	 * @param n number of bits to read (1-16)
	 * @return the bits as an integer (LSB = latest bit)
	 */
	private int readBitsInternal(int n) {
		if (wordPointer < 0) wordPointer = 0;

		int sum = bitIndex + n;

		if (sum <= 32) {
			int returnValue = (frameBuffer[wordPointer] >>> (32 - sum)) & bitMask[n];
			bitIndex += n;
			if (bitIndex == 32) {
				bitIndex = 0;
				wordPointer++;
			}
			return returnValue;
		}

		// Handle cross-word read
		int right = frameBuffer[wordPointer] & 0x0000FFFF;
		wordPointer++;
		int left = frameBuffer[wordPointer] & 0xFFFF0000;
		int returnValue = ((right << 16) & 0xFFFF0000) | ((left >>> 16) & 0x0000FFFF);

		returnValue >>>= 48 - sum; // Equivalent to: >>= 16 - (n - (32 - bitIndex))
		returnValue &= bitMask[n];
		bitIndex = sum - 32;
		return returnValue;
	}

	/**
	 * Synchronizes to the next frame header.
	 *
	 * @param syncmode sync mode: {@link #INITIAL_SYNC} or {@link #STRICT_SYNC}
	 * @return the 32-bit header value.
	 * @throws BitstreamException if EOF or sync fails.
	 */
	public int syncToHeader(byte syncmode) throws BitstreamException {
		int bytesRead = readBytes(syncBuffer, 0, 3);
		if (bytesRead != 3) throw newBitstreamException(STREAM_EOF, null);

		int headerString = ((syncBuffer[0] << 16) & 0x00FF0000) |
				((syncBuffer[1] << 8)  & 0x0000FF00) |
				((syncBuffer[2] << 0)  & 0x000000FF);

		do {
			headerString <<= 8;
			if (readBytes(syncBuffer, 3, 1) != 1)
				throw newBitstreamException(STREAM_EOF, null);
			headerString |= (syncBuffer[3] & 0x000000FF);
		} while (!isSyncMark(headerString, syncmode, syncWord));

		return headerString;
	}

	/**
	 * Checks if the given header string matches sync criteria.
	 *
	 * @param headerString the 32-bit frame header
	 * @param syncmode sync mode
	 * @param word expected sync word (for strict mode)
	 * @return true if valid sync mark
	 */
	public boolean isSyncMark(int headerString, int syncmode, int word) {
		boolean sync = switch (syncmode) {
			case INITIAL_SYNC -> (headerString & 0xFFE00000) == 0xFFE00000; // MPEG 2.5 compatible
			default -> ((headerString & 0xFFF80C00) == word) &&
					(((headerString & 0x000000C0) == 0x000000C0) == singleChannelMode);
		};

		if (!sync) return false;

		// Filter invalid sample rate (11: illegal)
		if (((headerString >>> 10) & 3) == 3) return false;
		// Filter invalid layer (00: reserved)
		if (((headerString >>> 17) & 3) == 0) return false;
		// Filter invalid version (01: reserved)
		return ((headerString >>> 19) & 3) != 1;
	}

	/**
	 * Reads frame data into internal buffer.
	 *
	 * @param byteSize number of bytes to read
	 * @return number of bytes actually read
	 * @throws BitstreamException on I/O error
	 */
	public int readFrameData(int byteSize) throws BitstreamException {
		int numRead = readFully(frameBytes, 0, byteSize);
		frameSize = byteSize;
		wordPointer = -1;
		bitIndex = -1;
		return numRead;
	}

	/**
	 * Parses the previously read frame data into integer buffer.
	 *
	 * @throws BitstreamException never actually thrown in current impl
	 */
	public void parseFrame() throws BitstreamException {
		int b = 0;
		int bytesize = frameSize;

		for (int k = 0; k < bytesize; k += 4) {
			int b0 = frameBytes[k] & 0xFF;
			int b1 = (k + 1 < bytesize) ? frameBytes[k + 1] & 0xFF : 0;
			int b2 = (k + 2 < bytesize) ? frameBytes[k + 2] & 0xFF : 0;
			int b3 = (k + 3 < bytesize) ? frameBytes[k + 3] & 0xFF : 0;

			frameBuffer[b++] = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
		}

		wordPointer = 0;
		bitIndex = 0;
	}

	/**
	 * Sets the sync word to match against in strict mode.
	 *
	 * @param syncword0 the 32-bit sync word (big-endian)
	 */
	public void setSyncWord(int syncword0) {
		syncWord = syncword0 & 0xFFFFFF3F;
		singleChannelMode = (syncword0 & 0x000000C0) == 0x000000C0;
	}

	/**
	 * Factory method for BitstreamException.
	 */
	public BitstreamException newBitstreamException(int errorcode) {
		return new BitstreamException(errorcode, null);
	}

	/**
	 * Factory method for BitstreamException with cause.
	 */
	protected BitstreamException newBitstreamException(int errorcode, Throwable throwable) {
		return new BitstreamException(errorcode, throwable);
	}

	/**
	 * Reads exactly len bytes into array, padding with zeros on EOF.
	 *
	 * @throws BitstreamException on I/O error
	 */
	private int readFully(byte[] b, int offs, int len) throws BitstreamException {
		int nRead = 0;
		try {
			while (len > 0) {
				int bytesread = source.read(b, offs, len);
				if (bytesread == -1) {
					while (len-- > 0) {
						b[offs++] = 0;
					}
					break;
				}
				nRead += bytesread;
				offs += bytesread;
				len -= bytesread;
			}
		} catch (IOException ex) {
			throw newBitstreamException(STREAM_ERROR, ex);
		}
		return nRead;
	}

	/**
	 * Reads up to len bytes into array, stops at EOF.
	 *
	 * @throws BitstreamException on I/O error
	 */
	private int readBytes(byte[] b, int offs, int len) throws BitstreamException {
		int totalBytesRead = 0;
		try {
			while (len > 0) {
				int bytesread = source.read(b, offs, len);
				if (bytesread == -1) break;
				totalBytesRead += bytesread;
				offs += bytesread;
				len -= bytesread;
			}
		} catch (IOException ex) {
			throw newBitstreamException(STREAM_ERROR, ex);
		}
		return totalBytesRead;
	}

	// ==================== DEPRECATED COMPATIBILITY WRAPPERS ====================

	/**
	 * @deprecated Use {@link #getHeaderPosition()} instead.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public int header_pos() {
		return getHeaderPosition();
	}

	/**
	 * @deprecated Use {@link #readBitsInternal(int)} via {@link #readBits(int)}.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public int get_bits(int n) {
		return readBitsInternal(n);
	}

	/**
	 * @deprecated Use {@link #readBitsInternal(int)}.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public int readBits(int n) {
		return readBitsInternal(n);
	}

	/**
	 * @deprecated Use {@link #readBitsInternal(int)} — CRC not implemented anyway.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public int readCheckedBits(int n) {
		return readBitsInternal(n);
	}

	/**
	 * @deprecated Use {@link #syncToHeader(byte)}.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public int syncHeader(byte syncmode) throws BitstreamException {
		return syncToHeader(syncmode);
	}

	/**
	 * @deprecated Use {@link #readFrameData(int)}.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public int read_frame_data(int bytesize) throws BitstreamException {
		return readFrameData(bytesize);
	}

	/**
	 * @deprecated Use {@link #parseFrame()}.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public void parse_frame() throws BitstreamException {
		parseFrame();
	}

	/**
	 * @deprecated Use {@link #setSyncWord(int)}.
	 */
	@Deprecated
	@SuppressWarnings("unused")
	public void set_syncword(int syncword0) {
		setSyncWord(syncword0);
	}
}