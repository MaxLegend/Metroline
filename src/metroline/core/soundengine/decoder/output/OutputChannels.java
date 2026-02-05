/*
 * 11/19/04 1.0 moved to LGPL.
 * 12/12/99 Initial implementation.		mdm@techie.com.
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
 * A type-safe representation of the supported output channel constants.
 * <p>
 * This class is immutable and thread-safe.
 * </p>
 *
 * @author Mat McGowan 12/12/99
 * @since 0.0.7
 */
public class OutputChannels {

	/**
	 * Flag to indicate output should include both channels.
	 */
	public static final int BOTH_CHANNELS = 0;

	/**
	 * Flag to indicate output should include the left channel only.
	 */
	public static final int LEFT_CHANNEL = 1;

	/**
	 * Flag to indicate output should include the right channel only.
	 */
	public static final int RIGHT_CHANNEL = 2;

	/**
	 * Flag to indicate output is mono (downmixed).
	 */
	public static final int DOWNMIX_CHANNELS = 3;

	/**
	 * Singleton instance for left channel output.
	 */
	public static final OutputChannels LEFT = new OutputChannels(LEFT_CHANNEL);

	/**
	 * Singleton instance for right channel output.
	 */
	public static final OutputChannels RIGHT = new OutputChannels(RIGHT_CHANNEL);

	/**
	 * Singleton instance for both channels output.
	 */
	public static final OutputChannels BOTH = new OutputChannels(BOTH_CHANNELS);

	/**
	 * Singleton instance for downmixed (mono) output.
	 */
	public static final OutputChannels DOWNMIX = new OutputChannels(DOWNMIX_CHANNELS);

	/**
	 * The internal channel code represented by this instance.
	 */
	private final int outputChannels;

	/**
	 * Private constructor to enforce immutability and singleton pattern.
	 *
	 * @param channels one of the channel code constants.
	 * @throws IllegalArgumentException if channels is not in valid range [0, 3].
	 */
	private OutputChannels(int channels) {
		if (channels < 0 || channels > 3) {
			throw new IllegalArgumentException("Invalid channel code: " + channels);
		}
		this.outputChannels = channels;
	}

	/**
	 * Creates an {@code OutputChannels} instance corresponding to the given channel code.
	 *
	 * @param code one of the OutputChannels channel code constants.
	 * @return the corresponding OutputChannels instance.
	 * @throws IllegalArgumentException if code is not a valid channel code.
	 */
	public static OutputChannels fromInt(int code) {
		switch (code) {
			case LEFT_CHANNEL:
				return LEFT;
			case RIGHT_CHANNEL:
				return RIGHT;
			case BOTH_CHANNELS:
				return BOTH;
			case DOWNMIX_CHANNELS:
				return DOWNMIX;
			default:
				throw new IllegalArgumentException("Invalid channel code: " + code);
		}
	}

	/**
	 * Retrieves the code representing the desired output channels.
	 * Will be one of {@link #LEFT_CHANNEL}, {@link #RIGHT_CHANNEL},
	 * {@link #BOTH_CHANNELS} or {@link #DOWNMIX_CHANNELS}.
	 *
	 * @return the channel code represented by this instance.
	 */
	public int getChannelsOutputCode() {
		return this.outputChannels;
	}

	/**
	 * Retrieves the number of output channels represented by this channel output type.
	 *
	 * @return the number of output channels: 2 for {@link #BOTH_CHANNELS}, 1 for all others.
	 */
	public int getChannelCount() {
		return (this.outputChannels == BOTH_CHANNELS) ? 2 : 1;
	}

	/**
	 * Compares this object with the specified object for equality.
	 *
	 * @param o the object to compare with.
	 * @return {@code true} if the objects are equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof OutputChannels)) {
			return false;
		}
		OutputChannels oc = (OutputChannels) o;
		return this.outputChannels == oc.outputChannels;
	}

	/**
	 * Returns a hash code value for this object.
	 *
	 * @return the hash code based on the channel code.
	 */
	@Override
	public int hashCode() {
		return this.outputChannels;
	}
}