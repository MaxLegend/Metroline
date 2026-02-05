/*
 * 11/19/04		1.0 moved to LGPL.
 * 01/12/99		Initial version.	mdm@techie.com
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

import metroline.core.soundengine.decoder.bit.Bitstream;
import metroline.core.soundengine.decoder.exceptions.DecoderException;
import metroline.core.soundengine.decoder.interfaces.DecoderErrors;
import metroline.core.soundengine.decoder.interfaces.FrameDecoder;
import metroline.core.soundengine.decoder.layers.LayerIDecoder;
import metroline.core.soundengine.decoder.layers.LayerIIDecoder;
import metroline.core.soundengine.decoder.layers.LayerIIIDecoder;
import metroline.core.soundengine.decoder.layers.SynthesisFilter;
import metroline.core.soundengine.decoder.output.OutputBuffer;
import metroline.core.soundengine.decoder.output.OutputChannels;
import metroline.core.soundengine.decoder.output.SampleBuffer;


/**
 *=======================================================================*
 THIS CLASS FULLY SAFE REFACTORING BY M.A. Vavaev (@Tesmio) 09 Sep 2025
 *=======================================================================*
 */


/**
 * The <code>Decoder</code> class encapsulates the logic for decoding an MPEG audio frame.
 * <p>
 * It manages synthesis filters, output buffer, and delegates frame decoding to layer-specific decoders
 * (Layer I, II, or III). Supports equalizer settings and customizable output parameters.
 * </p>
 *
 * @author MDM
 * @version 0.0.7 12/12/99
 * @since 0.0.5
 */
public class Decoder implements DecoderErrors {

	/**
	 * Default decoder parameters.
	 */
	private static final Params DEFAULT_PARAMS = new Params();

	/**
	 * The bitstream from which MPEG audio frames are read.
	 * <p>
	 * Currently unused / commented out for legacy reasons.
	 * </p>
	 */
	// private Bitstream stream;

	/**
	 * Output buffer that receives decoded PCM samples.
	 */
	private OutputBuffer output;

	/**
	 * Synthesis filter for the left channel (or mono).
	 */
	private SynthesisFilter filter1;

	/**
	 * Synthesis filter for the right channel (stereo only).
	 */
	private SynthesisFilter filter2;

	/**
	 * Layer III frame decoder (lazy-initialized).
	 */
	private LayerIIIDecoder l3decoder;

	/**
	 * Layer II frame decoder (lazy-initialized).
	 */
	private LayerIIDecoder l2decoder;

	/**
	 * Layer I frame decoder (lazy-initialized).
	 */
	private LayerIDecoder l1decoder;

	/**
	 * Sample rate of decoded output (Hz).
	 */
	private int outputFrequency;

	/**
	 * Number of output channels (1 = mono, 2 = stereo).
	 */
	private int outputChannels;

	/**
	 * Equalizer applied during decoding.
	 */
	private Equalizer equalizer = new Equalizer();

	/**
	 * Customizable decoder parameters.
	 */
	private Params params;

	/**
	 * Flag indicating whether the decoder has been initialized.
	 */
	private boolean initialized;

	/**
	 * Creates a new <code>Decoder</code> instance with default parameters.
	 */
	public Decoder() {
		this(null);
	}

	/**
	 * Creates a new <code>Decoder</code> instance with specified parameters.
	 *
	 * @param params0 The <code>Params</code> instance describing customizable decoder aspects.
	 *                If null, default parameters are used.
	 */
	public Decoder(Params params0) {
		this.params = (params0 != null) ? params0 : DEFAULT_PARAMS;

		Equalizer eq = this.params.getInitialEqualizerSettings();
		if (eq != null) {
			this.equalizer.setFrom(eq);
		}
	}

	/**
	 * Returns a clone of the default decoder parameters.
	 *
	 * @return a new <code>Params</code> instance with default settings
	 */
	public static Params getDefaultParams() {
		return (Params) DEFAULT_PARAMS.clone();
	}

	/**
	 * Sets the equalizer to be used during decoding.
	 * <p>
	 * Applies equalizer band factors to active synthesis filters.
	 * If null, a pass-through equalizer is used.
	 * </p>
	 *
	 * @param eq the equalizer settings to apply
	 */
	public void setEqualizer(Equalizer eq) {
		if (eq == null) {
			eq = Equalizer.PASS_THRU_EQ;
		}
		this.equalizer.setFrom(eq);

		float[] factors = this.equalizer.getBandFactors();
		if (this.filter1 != null) {
			this.filter1.setEQ(factors);
		}
		if (this.filter2 != null) {
			this.filter2.setEQ(factors);
		}
	}

	/**
	 * Decodes one MPEG audio frame.
	 * <p>
	 * Initializes the decoder on first call. Clears output buffer before decoding,
	 * writes buffer after decoding, and returns the output buffer.
	 * </p>
	 *
	 * @param header   The header describing the frame to decode.
	 * @param bitstream The bitstream providing the frame body bits.
	 * @return An <code>OutputBuffer</code> containing decoded PCM samples.
	 * @throws DecoderException if decoding fails (e.g., unsupported layer)
	 */
	public OutputBuffer decodeFrame(Header header, Bitstream bitstream) throws DecoderException {
		if (!this.initialized) {
			initialize(header);
		}

		this.output.clearBuffer(); // Modernized name — old clear_buffer() still exists as wrapper

		int layer = header.layer();
		FrameDecoder decoder = retrieveDecoder(header, bitstream, layer);

		decoder.decodeFrame();

		this.output.writeBuffer(1); // Modernized name — old write_buffer() still exists as wrapper

		return this.output;
	}

	/**
	 * Sets the output buffer to receive decoded samples.
	 * <p>
	 * Takes effect on the next call to <code>decodeFrame()</code>.
	 * </p>
	 *
	 * @param out the output buffer to use
	 */
	public void setOutputBuffer(OutputBuffer out) {
		this.output = out;
	}

	/**
	 * Returns the sample frequency (in Hz) of decoded output.
	 * <p>
	 * Typically matches the sample rate encoded in the MPEG stream.
	 * </p>
	 *
	 * @return output sample rate in Hz
	 */
	public int getOutputFrequency() {
		return this.outputFrequency;
	}

	/**
	 * Returns the number of PCM output channels.
	 * <p>
	 * Usually matches the MPEG stream, but may differ based on decoder settings.
	 * </p>
	 *
	 * @return 1 for mono, 2 for stereo
	 */
	public int getOutputChannels() {
		return this.outputChannels;
	}

	/**
	 * Returns the maximum number of samples written per frame.
	 * <p>
	 * Useful for pre-allocating buffers. This is an upper bound — actual samples
	 * may be fewer depending on sample rate and channels.
	 * </p>
	 *
	 * @return maximum output block size (in samples)
	 */
	public int getOutputBlockSize() {
		return OutputBuffer.OBUFFERSIZE;
	}

	/**
	 * Creates a new DecoderException with specified error code.
	 *
	 * @param errorcode the error code (from DecoderErrors)
	 * @return a new DecoderException
	 */
	protected DecoderException newDecoderException(int errorcode) {
		return new DecoderException(errorcode, null);
	}

	/**
	 * Creates a new DecoderException with specified error code and cause.
	 *
	 * @param errorcode the error code (from DecoderErrors)
	 * @param throwable the cause of the exception
	 * @return a new DecoderException
	 */
	protected DecoderException newDecoderException(int errorcode, Throwable throwable) {
		return new DecoderException(errorcode, throwable);
	}

	/**
	 * Retrieves or initializes the appropriate frame decoder for the given layer.
	 *
	 * @param header    frame header
	 * @param bitstream bitstream for reading frame data
	 * @param layer     MPEG layer (1, 2, or 3)
	 * @return the frame decoder for the specified layer
	 * @throws DecoderException if layer is unsupported
	 */
	protected FrameDecoder retrieveDecoder(Header header, Bitstream bitstream, int layer) throws DecoderException {
		FrameDecoder decoder = null;

		// REVIEW: allow channel output selection type (LEFT, RIGHT, BOTH, DOWNMIX)
		switch (layer) {
			case 3:
				if (this.l3decoder == null) {
					this.l3decoder = new LayerIIIDecoder(bitstream,
							header, this.filter1, this.filter2,
							this.output, OutputChannels.BOTH_CHANNELS);
				}
				decoder = this.l3decoder;
				break;
			case 2:
				if (this.l2decoder == null) {
					this.l2decoder = new LayerIIDecoder();
					this.l2decoder.create(bitstream,
							header, this.filter1, this.filter2,
							this.output, OutputChannels.BOTH_CHANNELS);
				}
				decoder = this.l2decoder;
				break;
			case 1:
				if (this.l1decoder == null) {
					this.l1decoder = new LayerIDecoder();
					this.l1decoder.create(bitstream,
							header, this.filter1, this.filter2,
							this.output, OutputChannels.BOTH_CHANNELS);
				}
				decoder = this.l1decoder;
				break;
		}

		if (decoder == null) {
			throw newDecoderException(UNSUPPORTED_LAYER, null);
		}

		return decoder;
	}

	/**
	 * Initializes decoder state based on frame header.
	 * <p>
	 * Sets up output buffer, synthesis filters, channel count, and sample rate.
	 * Called once on first decodeFrame().
	 * </p>
	 *
	 * @param header frame header
	 * @throws DecoderException if initialization fails
	 */
	private void initialize(Header header) throws DecoderException {
		// REVIEW: allow customizable scale factor
		final float scalefactor = 32700.0f;

		int mode = header.mode();
		int layer = header.layer();
		int channels = (mode == Header.SINGLE_CHANNEL) ? 1 : 2;

		// Set up output buffer if not provided by client
		if (this.output == null) {
			this.output = new SampleBuffer(header.frequency(), channels);
		}

		float[] factors = this.equalizer.getBandFactors();
		this.filter1 = new SynthesisFilter(0, scalefactor, factors);

		// REVIEW: allow mono output for stereo
		if (channels == 2) {
			this.filter2 = new SynthesisFilter(1, scalefactor, factors);
		}

		this.outputChannels = channels;
		this.outputFrequency = header.frequency();
		this.initialized = true;
	}

	/**
	 * The <code>Params</code> class encapsulates customizable decoder settings.
	 * <p>
	 * Not thread-safe. Instances should be configured before passing to Decoder constructor.
	 * </p>
	 */
	public static class Params implements Cloneable {

		/**
		 * Output channel configuration (BOTH, LEFT, RIGHT, DOWNMIX).
		 */
		private OutputChannels outputChannels = OutputChannels.BOTH;

		/**
		 * Initial equalizer settings applied at decoder initialization.
		 */
		private Equalizer equalizer = new Equalizer();

		/**
		 * Constructs a new Params instance with default settings.
		 */
		public Params() {
		}

		/**
		 * Creates a shallow copy of this Params instance.
		 *
		 * @return a cloned Params instance
		 * @throws InternalError if cloning fails (should not occur)
		 */
		@Override
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException ex) {
				throw new InternalError(this + ": " + ex);
			}
		}

		/**
		 * Sets the output channel configuration.
		 *
		 * @param out the output channel setting (must not be null)
		 * @throws NullPointerException if out is null
		 */
		public void setOutputChannels(OutputChannels out) {
			if (out == null) {
				throw new NullPointerException("out");
			}
			this.outputChannels = out;
		}

		/**
		 * Returns the configured output channel setting.
		 *
		 * @return the output channel configuration
		 */
		public OutputChannels getOutputChannels() {
			return this.outputChannels;
		}

		/**
		 * Returns the equalizer settings used to initialize the decoder.
		 * <p>
		 * This equalizer is applied only once during decoder initialization.
		 * To adjust EQ in real-time, use {@link Decoder#setEqualizer(Equalizer)}.
		 * </p>
		 *
		 * @return the initial equalizer settings
		 */
		public Equalizer getInitialEqualizerSettings() {
			return this.equalizer;
		}
	}
}