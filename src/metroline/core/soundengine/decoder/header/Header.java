/*
 * 11/19/04 : 1.0 moved to LGPL.
 *            VBRI header support added, E.B javalayer@javazoom.net
 * 
 * 12/04/03 : VBR (XING) header support added, E.B javalayer@javazoom.net
 *
 * 02/13/99 : Java Conversion by JavaZOOM , E.B javalayer@javazoom.net
 *
 * Declarations for MPEG header class
 * A few layer III, MPEG-2 LSF, and seeking modifications made by Jeff Tsay.
 * Last modified : 04/19/97
 *
 *  @(#) header.h 1.7, last edit: 6/15/94 16:55:33
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
package metroline.core.soundengine.decoder.header;

import metroline.core.soundengine.decoder.exceptions.BitstreamException;
import metroline.core.soundengine.decoder.core.CRC16;
import metroline.core.soundengine.decoder.bit.Bitstream;

/**
 *=======================================================================*
 THIS CLASS FULLY SAFE REFACTORING BY M.A. Vavaev (@Tesmio) 09 Sep 2025
 *=======================================================================*
 */

/**
 * Class for extracting information from an MPEG audio frame header.
 * Supports MPEG-1, MPEG-2 LSF, MPEG-2.5, VBR (Xing/VBRI).
 */
public final class Header {

	// ===================== Sample Rates =====================

	/**
	 * Sample frequencies in Hz for [version][sample_frequency_index]
	 */
	private static final int[][] FREQUENCIES = {
			{22050, 24000, 16000, 1},  // MPEG-2 LSF
			{44100, 48000, 32000, 1},  // MPEG-1
			{11025, 12000, 8000, 1}    // MPEG-2.5 LSF
	};

	// ===================== MPEG Version Constants (Legacy + Modern) =====================

	@Deprecated public static final int MPEG2_LSF = 0;
	@Deprecated public static final int MPEG25_LSF = 2;
	@Deprecated public static final int MPEG1 = 1;

	private static final int MPEG_VERSION_2_LSF = 0;
	private static final int MPEG_VERSION_25_LSF = 2;
	private static final int MPEG_VERSION_1 = 1;

	// ===================== Channel Mode Constants (Legacy + Modern) =====================

	@Deprecated public static final int STEREO = 0;
	@Deprecated public static final int JOINT_STEREO = 1;
	@Deprecated public static final int DUAL_CHANNEL = 2;
	@Deprecated public static final int SINGLE_CHANNEL = 3;

	private static final int MODE_STEREO = 0;
	private static final int MODE_JOINT_STEREO = 1;
	private static final int MODE_DUAL_CHANNEL = 2;
	private static final int MODE_SINGLE_CHANNEL = 3;

	// ===================== Sample Frequency Index Constants (Legacy + Modern) =====================

	@Deprecated public static final int FOURTYFOUR_POINT_ONE = 0;
	@Deprecated public static final int FOURTYEIGHT = 1;
	@Deprecated public static final int THIRTYTWO = 2;

	private static final int SAMPLE_FREQ_INDEX_44100 = 0;
	private static final int SAMPLE_FREQ_INDEX_48000 = 1;
	private static final int SAMPLE_FREQ_INDEX_32000 = 2;

	// ===================== VBR Constants =====================

	private static final double[] VBR_TIME_PER_FRAME = {-1, 384, 1152, 1152};

	// ===================== Bitrate Tables =====================

	private static final int[][][] BITRATES = {
			{
					{0, 32000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 176000, 192000, 224000, 256000, 0},
					{0, 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 0},
					{0, 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 0}
			},
			{
					{0, 32000, 64000, 96000, 128000, 160000, 192000, 224000, 256000, 288000, 320000, 352000, 384000, 416000, 448000, 0},
					{0, 32000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 160000, 192000, 224000, 256000, 320000, 384000, 0},
					{0, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 160000, 192000, 224000, 256000, 320000, 0}
			},
			{
					{0, 32000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 176000, 192000, 224000, 256000, 0},
					{0, 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 0},
					{0, 8000, 16000, 24000, 32000, 40000, 48000, 56000, 64000, 80000, 96000, 112000, 128000, 144000, 160000, 0}
			}
	};

	private static final String[][][] BITRATE_STRINGS = {
			{
					{"free format", "32 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "176 kbit/s", "192 kbit/s", "224 kbit/s", "256 kbit/s", "forbidden"},
					{"free format", "8 kbit/s", "16 kbit/s", "24 kbit/s", "32 kbit/s", "40 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "forbidden"},
					{"free format", "8 kbit/s", "16 kbit/s", "24 kbit/s", "32 kbit/s", "40 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "forbidden"}
			},
			{
					{"free format", "32 kbit/s", "64 kbit/s", "96 kbit/s", "128 kbit/s", "160 kbit/s", "192 kbit/s", "224 kbit/s", "256 kbit/s", "288 kbit/s", "320 kbit/s", "352 kbit/s", "384 kbit/s", "416 kbit/s", "448 kbit/s", "forbidden"},
					{"free format", "32 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "160 kbit/s", "192 kbit/s", "224 kbit/s", "256 kbit/s", "320 kbit/s", "384 kbit/s", "forbidden"},
					{"free format", "32 kbit/s", "40 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "160 kbit/s", "192 kbit/s", "224 kbit/s", "256 kbit/s", "320 kbit/s", "forbidden"}
			},
			{
					{"free format", "32 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "176 kbit/s", "192 kbit/s", "224 kbit/s", "256 kbit/s", "forbidden"},
					{"free format", "8 kbit/s", "16 kbit/s", "24 kbit/s", "32 kbit/s", "40 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "forbidden"},
					{"free format", "8 kbit/s", "16 kbit/s", "24 kbit/s", "32 kbit/s", "40 kbit/s", "48 kbit/s", "56 kbit/s", "64 kbit/s", "80 kbit/s", "96 kbit/s", "112 kbit/s", "128 kbit/s", "144 kbit/s", "160 kbit/s", "forbidden"}
			}
	};

	// ===================== Instance Fields =====================

	// Header data fields (prefixed with 'h_' for historical compatibility)
	private int h_layer;
	private int h_protection_bit;
	private int h_bitrate_index;
	private int h_padding_bit;
	private int h_mode_extension;

	private int h_version;
	private int h_mode;
	private int h_sample_frequency;

	private int h_number_of_subbands;
	private int h_intensity_stereo_bound;

	private boolean h_copyright;
	private boolean h_original;

	// VBR fields
	private boolean h_vbr;
	private int h_vbr_frames;
	private int h_vbr_scale;
	private int h_vbr_bytes;
	private byte[] h_vbr_toc;

	// Sync and CRC
	private byte syncmode = Bitstream.INITIAL_SYNC;
	private CRC16 crc;

	// Public fields (used by decoder) — kept for compatibility
	public short checksum;
	public int framesize;
	public int nSlots;

	// Internal state
	private int _headerstring = -1; // E.B — original header int value

	// ===================== Constructor =====================

	/**
	 * Creates a new Header instance.
	 */
	public Header() {
		// Nothing to initialize — all fields have default values
	}

	// ===================== toString() =====================

	/**
	 * Returns a string representation of this header.
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder(200);
		buffer.append("Layer ").append(getLayerName())
			  .append(" frame ").append(getModeName())
			  .append(' ').append(getVersionName());

		if (!hasChecksum()) {
			buffer.append(" no");
		}
		buffer.append(" checksums ")
			  .append(getSampleRateString())
			  .append(", ")
			  .append(getBitrateString());

		return buffer.toString();
	}
	// ===================== Core Header Reading =====================

	/**
	 * Reads and parses a 32-bit MPEG frame header from the bitstream.
	 * @param stream the bitstream to read from
	 * @param crcp output parameter for CRC object (if checksum present)
	 * @throws BitstreamException if header is invalid or sync fails
	 */
	public void read_header(Bitstream stream, CRC16[] crcp) throws BitstreamException {
		int headerstring;
		boolean sync = false;

		do {
			headerstring = stream.syncHeader(syncmode);
			_headerstring = headerstring;

			// Parse version and sample frequency on initial sync
			if (syncmode == Bitstream.INITIAL_SYNC) {
				parseVersionAndSampleFrequency(headerstring, stream);
			}

			// Parse main header fields
			parseHeaderFields(headerstring);

			// Calculate subbands based on layer, mode, bitrate
			calculateSubbands();

			// Calculate frame size and slots
			calculate_framesize();

			// Read frame data
			int framesizeloaded = stream.read_frame_data(framesize);
			if (framesize >= 0 && framesizeloaded != framesize) {
				throw stream.newBitstreamException(Bitstream.INVALIDFRAME);
			}

			// Validate sync
			sync = validateSync(stream, headerstring);

			if (!sync) {
				stream.unreadFrame();
			}

		} while (!sync);

		// Parse the rest of the frame
		stream.parse_frame();

		// Handle CRC if present
		handleChecksum(stream, crcp, headerstring);
	}

	/**
	 * Parses MPEG version and sample frequency from header.
	 */
	private void parseVersionAndSampleFrequency(int headerstring, Bitstream stream) throws BitstreamException {
		h_version = (headerstring >>> 19) & 1;

		// Detect MPEG-2.5
		if (((headerstring >>> 20) & 1) == 0) {
			if (h_version == MPEG2_LSF) {
				h_version = MPEG25_LSF;
			} else {
				throw stream.newBitstreamException(Bitstream.UNKNOWN_ERROR);
			}
		}

		h_sample_frequency = (headerstring >>> 10) & 3;
		if (h_sample_frequency == 3) {
			throw stream.newBitstreamException(Bitstream.UNKNOWN_ERROR);
		}
	}

	/**
	 * Parses layer, bitrate, mode, and other fields from header.
	 */
	private void parseHeaderFields(int headerstring) {
		h_layer = 4 - ((headerstring >>> 17) & 3);
		h_protection_bit = (headerstring >>> 16) & 1;
		h_bitrate_index = (headerstring >>> 12) & 0xF;
		h_padding_bit = (headerstring >>> 9) & 1;
		h_mode = (headerstring >>> 6) & 3;
		h_mode_extension = (headerstring >>> 4) & 3;

		// Intensity stereo bound (Layer II joint stereo)
		if (h_mode == JOINT_STEREO) {
			h_intensity_stereo_bound = (h_mode_extension << 2) + 4;
		} else {
			h_intensity_stereo_bound = 0;
		}

		h_copyright = ((headerstring >>> 3) & 1) == 1;
		h_original = ((headerstring >>> 2) & 1) == 1;
	}

	/**
	 * Calculates number of subbands based on layer, mode, bitrate, and sample rate.
	 */
	private void calculateSubbands() {
		if (h_layer == 1) {
			h_number_of_subbands = 32;
		} else {
			int channel_bitrate = h_bitrate_index;

			// Adjust bitrate per channel
			if (h_mode != SINGLE_CHANNEL) {
				if (channel_bitrate == 4) {
					channel_bitrate = 1;
				} else {
					channel_bitrate -= 4;
				}
			}

			// Determine subbands
			if (channel_bitrate == 1 || channel_bitrate == 2) {
				if (h_sample_frequency == THIRTYTWO) {
					h_number_of_subbands = 12;
				} else {
					h_number_of_subbands = 8;
				}
			} else if (h_sample_frequency == FOURTYEIGHT || (channel_bitrate >= 3 && channel_bitrate <= 5)) {
				h_number_of_subbands = 27;
			} else {
				h_number_of_subbands = 30;
			}
		}

		// Clamp intensity stereo bound
		if (h_intensity_stereo_bound > h_number_of_subbands) {
			h_intensity_stereo_bound = h_number_of_subbands;
		}
	}

	/**
	 * Validates sync position and updates sync mode if needed.
	 */
	private boolean validateSync(Bitstream stream, int headerstring) throws BitstreamException {
		if (stream.isSyncCurrentPosition(syncmode)) {
			if (syncmode == Bitstream.INITIAL_SYNC) {
				syncmode = Bitstream.STRICT_SYNC;
				stream.set_syncword(headerstring & 0xFFF80CC0);
			}
			return true;
		}
		return false;
	}

	/**
	 * Handles CRC checksum if present in frame.
	 */
	private void handleChecksum(Bitstream stream, CRC16[] crcp, int headerstring) throws BitstreamException {
		if (h_protection_bit == 0) {
			checksum = (short) stream.get_bits(16);
			if (crc == null) {
				crc = new CRC16();
			}
			crc.add_bits(headerstring, 16);
			crcp[0] = crc;
		} else {
			crcp[0] = null;
		}
	}

	/**
	 * Helper to read big-endian 32-bit integer from byte array.
	 */
	private int readInt32BE(byte[] data, int offset) {
		return ((data[offset] & 0xFF) << 24) |
				((data[offset + 1] & 0xFF) << 16) |
				((data[offset + 2] & 0xFF) << 8) |
				(data[offset + 3] & 0xFF);
	}
	// ===================== VBR Header Parsing =====================

	/**
	 * Attempts to parse optional VBR header (Xing or VBRI) from the first frame data.
	 * @param firstframe raw frame data (must include header + side info)
	 * @throws BitstreamException if VBR header is corrupted
	 */
	public void parseVBR(byte[] firstframe) throws BitstreamException {
		if (tryParseXingVBR(firstframe)) {
			return;
		}
		tryParseVBRI(firstframe);
	}

	/**
	 * Tries to parse Xing VBR header.
	 * @return true if Xing header was found and parsed
	 */
	private boolean tryParseXingVBR(byte[] firstframe) throws BitstreamException {
		final String XING = "Xing";
		byte[] tmp = new byte[4];

		// Compute offset based on MPEG version and mode
		int offset;
		if (h_version == MPEG1) {
			offset = (h_mode == SINGLE_CHANNEL) ? 21 - 4 : 36 - 4;
		} else {
			offset = (h_mode == SINGLE_CHANNEL) ? 13 - 4 : 21 - 4;
		}

		try {
			System.arraycopy(firstframe, offset, tmp, 0, 4);
			if (!XING.equals(new String(tmp))) {
				return false;
			}

			// Xing header found
			initializeVbrFields();

			int length = 4;

			// Read flags
			byte[] flags = new byte[4];
			System.arraycopy(firstframe, offset + length, flags, 0, flags.length);
			length += flags.length;

			// Read number of frames
			if ((flags[3] & (byte) (1 << 0)) != 0) {
				h_vbr_frames = readInt32BE(firstframe, offset + length);
				length += 4;
			}

			// Read total bytes
			if ((flags[3] & (byte) (1 << 1)) != 0) {
				h_vbr_bytes = readInt32BE(firstframe, offset + length);
				length += 4;
			}

			// Read TOC
			if ((flags[3] & (byte) (1 << 2)) != 0) {
				h_vbr_toc = new byte[100];
				System.arraycopy(firstframe, offset + length, h_vbr_toc, 0, h_vbr_toc.length);
				length += h_vbr_toc.length;
			}

			// Read VBR scale
			if ((flags[3] & (byte) (1 << 3)) != 0) {
				h_vbr_scale = readInt32BE(firstframe, offset + length);
				length += 4;
			}

			return true;

		} catch (ArrayIndexOutOfBoundsException e) {
			throw new BitstreamException("XingVBRHeader Corrupted", e);
		}
	}

	/**
	 * Tries to parse VBRI VBR header.
	 * @return true if VBRI header was found and parsed
	 */
	private boolean tryParseVBRI(byte[] firstframe) throws BitstreamException {
		final String VBRI = "VBRI";
		byte[] tmp = new byte[4];
		int offset = 36 - 4;

		try {
			System.arraycopy(firstframe, offset, tmp, 0, 4);
			if (!VBRI.equals(new String(tmp))) {
				return false;
			}

			// VBRI header found
			initializeVbrFields();

			int length = 4 + 6; // Skip 6 reserved bytes

			// Read total bytes
			h_vbr_bytes = readInt32BE(firstframe, offset + length);
			length += 4;

			// Read number of frames
			h_vbr_frames = readInt32BE(firstframe, offset + length);
			length += 4;

			// TODO: Parse TOC if needed

			return true;

		} catch (ArrayIndexOutOfBoundsException e) {
			throw new BitstreamException("VBRIHeader Corrupted", e);
		}
	}

	/**
	 * Initializes VBR fields before parsing.
	 */
	private void initializeVbrFields() {
		h_vbr = true;
		h_vbr_frames = -1;
		h_vbr_bytes = -1;
		h_vbr_scale = -1;
		h_vbr_toc = null; // will be allocated only if TOC present
	}
	// ===================== Modern Getters (Recommended) =====================

	/**
	 * Returns the MPEG version: 0=MPEG-2 LSF, 1=MPEG-1, 2=MPEG-2.5 LSF.
	 */
	public int getVersion() {
		return h_version;
	}

	/**
	 * Returns the layer: 1, 2, or 3.
	 */
	public int getLayer() {
		return h_layer;
	}

	/**
	 * Returns the bitrate index (0-15).
	 */
	public int getBitrateIndex() {
		return h_bitrate_index;
	}

	/**
	 * Returns sample frequency index: 0=44.1/22.05/11.025, 1=48/24/12, 2=32/16/8 kHz.
	 */
	public int getSampleFrequencyIndex() {
		return h_sample_frequency;
	}

	/**
	 * Returns actual sample rate in Hz.
	 */
	public int getSampleRateHz() {
		return FREQUENCIES[h_version][h_sample_frequency];
	}

	/**
	 * Returns channel mode: 0=stereo, 1=joint stereo, 2=dual channel, 3=single channel.
	 */
	public int getMode() {
		return h_mode;
	}

	/**
	 * Returns true if frame includes a CRC checksum.
	 */
	public boolean hasChecksum() {
		return h_protection_bit == 0;
	}

	/**
	 * Returns true if audio is copyrighted.
	 */
	public boolean isCopyrighted() {
		return h_copyright;
	}

	/**
	 * Returns true if audio is original (not a copy).
	 */
	public boolean isOriginal() {
		return h_original;
	}

	/**
	 * Returns true if VBR header (Xing or VBRI) was detected.
	 */
	public boolean isVbr() {
		return h_vbr;
	}

	/**
	 * Returns VBR scale, or -1 if not available.
	 */
	public int getVbrScale() {
		return h_vbr_scale;
	}

	/**
	 * Returns VBR TOC (table of contents), or null if not available.
	 */
	public byte[] getVbrToc() {
		return h_vbr_toc;
	}

	/**
	 * Returns true if computed CRC matches stream CRC.
	 */
	public boolean isChecksumValid() {
		return crc != null && checksum == crc.checksum();
	}

	/**
	 * Returns true if frame has padding bit set.
	 */
	public boolean hasPadding() {
		return h_padding_bit != 0;
	}

	/**
	 * Returns number of slots (Layer III only).
	 */
	public int getSlots() {
		return nSlots;
	}

	/**
	 * Returns mode extension (used in Joint Stereo).
	 */
	public int getModeExtension() {
		return h_mode_extension;
	}

	// ===================== Legacy Getters (Deprecated) =====================

	@Deprecated
	public int version() { return getVersion(); }

	@Deprecated
	public int layer() { return getLayer(); }

	@Deprecated
	public int bitrate_index() { return getBitrateIndex(); }

	@Deprecated
	public int sample_frequency() { return getSampleFrequencyIndex(); }

	@Deprecated
	public int frequency() { return getSampleRateHz(); }

	@Deprecated
	public int mode() { return getMode(); }

	@Deprecated
	public boolean checksums() { return hasChecksum(); }

	@Deprecated
	public boolean copyright() { return isCopyrighted(); }

	@Deprecated
	public boolean original() { return isOriginal(); }

	@Deprecated
	public boolean vbr() { return isVbr(); }

	@Deprecated
	public int vbr_scale() { return getVbrScale(); }

	@Deprecated
	public byte[] vbr_toc() { return getVbrToc(); }

	@Deprecated
	public boolean checksum_ok() { return isChecksumValid(); }

	@Deprecated
	public boolean padding() { return hasPadding(); }

	@Deprecated
	public int slots() { return getSlots(); }

	@Deprecated
	public int mode_extension() { return getModeExtension(); }
	// ===================== Frame Size & Timing Calculations =====================

	/**
	 * Calculates and updates {@link #framesize} and {@link #nSlots} based on current header values.
	 * @return frame size in bytes, excluding 4-byte header
	 */
	public int calculate_framesize() {
		if (h_layer == 1) {
			framesize = (12 * BITRATES[h_version][0][h_bitrate_index]) / FREQUENCIES[h_version][h_sample_frequency];
			if (h_padding_bit != 0) {
				framesize++;
			}
			framesize <<= 2; // one slot is 4 bytes
			nSlots = 0;
		} else {
			framesize = (144 * BITRATES[h_version][h_layer - 1][h_bitrate_index]) / FREQUENCIES[h_version][h_sample_frequency];
			if (h_version == MPEG2_LSF || h_version == MPEG25_LSF) {
				framesize >>= 1;
			}
			if (h_padding_bit != 0) {
				framesize++;
			}

			// Layer III: calculate slots (side info size)
			if (h_layer == 3) {
				int sideInfoSize = (h_mode == SINGLE_CHANNEL) ? (h_version == MPEG1 ? 17 : 9) : (h_version == MPEG1 ? 32 : 17);
				int crcSize = (h_protection_bit != 0) ? 0 : 2;
				nSlots = framesize - sideInfoSize - crcSize - 4; // subtract header
			} else {
				nSlots = 0;
			}
		}

		framesize -= 4; // subtract header size
		return framesize;
	}

	/**
	 * Returns the maximum number of frames that fit in a stream of given size.
	 * For VBR, returns value from VBR header.
	 * @param streamsize total stream size in bytes
	 * @return estimated number of frames
	 */
	public int max_number_of_frames(int streamsize) {
		if (h_vbr) {
			return h_vbr_frames;
		} else {
			int frameSizeWithHeader = framesize + 4 - h_padding_bit;
			return (frameSizeWithHeader == 0) ? 0 : streamsize / frameSizeWithHeader;
		}
	}

	/**
	 * Returns the minimum number of frames that fit in a stream of given size.
	 * For VBR, returns value from VBR header.
	 * @param streamsize total stream size in bytes
	 * @return estimated number of frames
	 */
	public int min_number_of_frames(int streamsize) {
		if (h_vbr) {
			return h_vbr_frames;
		} else {
			int frameSizeWithHeader = framesize + 5 - h_padding_bit;
			return (frameSizeWithHeader == 0) ? 0 : streamsize / frameSizeWithHeader;
		}
	}

	/**
	 * Returns duration of one frame in milliseconds.
	 * For VBR, uses VBR timing table.
	 * @return milliseconds per frame
	 */
	public float ms_per_frame() {
		if (h_vbr) {
			double tpf = VBR_TIME_PER_FRAME[getLayer()] / (double) getSampleRateHz();
			if (h_version == MPEG2_LSF || h_version == MPEG25_LSF) {
				tpf /= 2.0;
			}
			return (float) (tpf * 1000.0);
		} else {
			float[][] MS_PER_FRAME_ARRAY = {
					{8.707483f, 8.0f, 12.0f},     // Layer I
					{26.12245f, 24.0f, 36.0f},    // Layer II
					{26.12245f, 24.0f, 36.0f}     // Layer III
			};
			return MS_PER_FRAME_ARRAY[h_layer - 1][h_sample_frequency];
		}
	}

	/**
	 * Returns total duration of stream in milliseconds.
	 * @param streamsize total stream size in bytes
	 * @return total duration in ms
	 */
	public float total_ms(int streamsize) {
		return max_number_of_frames(streamsize) * ms_per_frame();
	}

	/**
	 * Returns the raw 32-bit synchronized header value.
	 * @return header as int, or -1 if not read yet
	 */
	public int getSyncHeader() {
		return _headerstring;
	}
	// ===================== String Representation Methods =====================

	/**
	 * Returns layer name: "I", "II", or "III".
	 */
	public String getLayerName() {
		switch (h_layer) {
			case 1: return "I";
			case 2: return "II";
			case 3: return "III";
			default: return null;
		}
	}

	@Deprecated
	public String layer_string() {
		return getLayerName();
	}

	/**
	 * Returns bitrate as a formatted string (e.g., "128 kbit/s").
	 * For VBR, returns average bitrate string.
	 */
	public String getBitrateString() {
		if (h_vbr) {
			return bitrate() / 1000 + " kb/s";
		} else {
			return BITRATE_STRINGS[h_version][h_layer - 1][h_bitrate_index];
		}
	}

	@Deprecated
	public String bitrate_string() {
		return getBitrateString();
	}

	/**
	 * Returns bitrate in bits per second.
	 * For VBR, calculates average bitrate based on total bytes and frames.
	 * @return bitrate in bps
	 */
	public int bitrate() {
		if (h_vbr && h_vbr_frames > 0) {
			return (int) ((h_vbr_bytes * 8L) / (ms_per_frame() * h_vbr_frames)) * 1000;
		} else {
			return BITRATES[h_version][h_layer - 1][h_bitrate_index];
		}
	}

	/**
	 * Returns instantaneous bitrate (ignores VBR — returns current frame's bitrate).
	 * @return bitrate in bps
	 */
	public int bitrate_instant() {
		return BITRATES[h_version][h_layer - 1][h_bitrate_index];
	}

	/**
	 * Returns sample rate as a formatted string (e.g., "44.1 kHz").
	 */
	public String getSampleRateString() {
		final String mpeg1Freq, mpeg2Freq, mpeg25Freq;

		switch (h_sample_frequency) {
			case SAMPLE_FREQ_INDEX_32000:
				mpeg1Freq = "32 kHz";
				mpeg2Freq = "16 kHz";
				mpeg25Freq = "8 kHz";
				break;
			case SAMPLE_FREQ_INDEX_44100:
				mpeg1Freq = "44.1 kHz";
				mpeg2Freq = "22.05 kHz";
				mpeg25Freq = "11.025 kHz";
				break;
			case SAMPLE_FREQ_INDEX_48000:
				mpeg1Freq = "48 kHz";
				mpeg2Freq = "24 kHz";
				mpeg25Freq = "12 kHz";
				break;
			default:
				return null;
		}

		switch (h_version) {
			case MPEG1: return mpeg1Freq;
			case MPEG2_LSF: return mpeg2Freq;
			case MPEG25_LSF: return mpeg25Freq;
			default: return null;
		}
	}

	@Deprecated
	public String sample_frequency_string() {
		return getSampleRateString();
	}

	/**
	 * Returns channel mode as a string: "Stereo", "Joint stereo", etc.
	 */
	public String getModeName() {
		switch (h_mode) {
			case STEREO: return "Stereo";
			case JOINT_STEREO: return "Joint stereo";
			case DUAL_CHANNEL: return "Dual channel";
			case SINGLE_CHANNEL: return "Single channel";
			default: return null;
		}
	}

	@Deprecated
	public String mode_string() {
		return getModeName();
	}

	/**
	 * Returns MPEG version as a string: "MPEG-1", "MPEG-2 LSF", etc.
	 */
	public String getVersionName() {
		switch (h_version) {
			case MPEG1: return "MPEG-1";
			case MPEG2_LSF: return "MPEG-2 LSF";
			case MPEG25_LSF: return "MPEG-2.5 LSF";
			default: return null;
		}
	}

	@Deprecated
	public String version_string() {
		return getVersionName();
	}
	// ===================== Subband & Stereo Processing =====================

	/**
	 * Returns the number of subbands in the current frame.
	 * Depends on layer, mode, bitrate, and sample rate.
	 * @return number of subbands (typically 8, 12, 27, 30, or 32)
	 */
	public int getNumberOfSubbands() {
		return h_number_of_subbands;
	}

	@Deprecated
	public int number_of_subbands() {
		return getNumberOfSubbands();
	}

	/**
	 * Returns the intensity stereo bound (Layer II joint stereo only).
	 * Subbands above this index are encoded in intensity stereo mode.
	 * @return intensity stereo bound (0 if not applicable)
	 */
	public int getIntensityStereoBound() {
		return h_intensity_stereo_bound;
	}

	@Deprecated
	public int intensity_stereo_bound() {
		return getIntensityStereoBound();
	}

}