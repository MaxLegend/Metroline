package metroline.core.soundengine.decoder.input;

import metroline.core.soundengine.decoder.interfaces.Source;

import java.io.IOException;
import java.io.InputStream;

/**
 *=======================================================================*
 THIS CLASS FULLY SAFE REFACTORING BY M.A. Vavaev (@Tesmio) 09 Sep 2025
 *=======================================================================*
 */

/**
 * A {@link Source} implementation that reads data from an {@link InputStream}.
 * <p>
 * This source does not support seeking — all position-related methods return {@code -1}.
 * Suitable for streaming audio from network, pipes, or other non-seekable sources.
 * </p>
 * <p>
 * <strong>Thread Safety:</strong> Not thread-safe. The underlying {@code InputStream}
 * must not be accessed concurrently by other threads.
 * </p>
 *
 * @author MDM
 * @since 0.0.5
 */
public class InputStreamSource implements Source {

	/**
	 * The underlying input stream.
	 */
	private final InputStream in;

	/**
	 * Constructs a new InputStreamSource wrapping the given InputStream.
	 *
	 * @param in the input stream to read from (must not be null)
	 * @throws NullPointerException if in is null
	 */
	public InputStreamSource(InputStream in) {
		if (in == null) {
			throw new NullPointerException("in");
		}
		this.in = in;
	}

	/**
	 * Reads up to {@code len} bytes from the input stream into the given buffer.
	 *
	 * @param b    the buffer to read into
	 * @param offs the offset in the buffer to start writing at
	 * @param len  the maximum number of bytes to read
	 * @return the number of bytes actually read, or -1 if end of stream
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public int read(byte[] b, int offs, int len) throws IOException {
		return this.in.read(b, offs, len);
	}

	/**
	 * Indicates whether the next read operation may block.
	 * <p>
	 * Always returns {@code true} for InputStreamSource, since InputStream.available()
	 * is not reliable for all stream types (e.g., network streams).
	 * </p>
	 * <p>
	 * <strong>Note:</strong> Previously attempted to use {@code in.available() == 0},
	 * but this was found to be unreliable and was replaced with constant {@code true}.
	 * </p>
	 *
	 * @return {@code true} (indicating next read may block)
	 */
	@Override
	public boolean willReadBlock() {
		return true;
		// Legacy logic (commented out for reference):
		// boolean block = (in.available() == 0);
		// return block;
	}

	/**
	 * Indicates whether this source supports seeking.
	 * <p>
	 * Returns {@code false} — InputStreams are generally not seekable.
	 * </p>
	 *
	 * @return {@code false}
	 */
	@Override
	public boolean isSeekable() {
		return false;
	}

	/**
	 * Returns the current position in the stream.
	 * <p>
	 * Not supported for InputStream — always returns {@code -1}.
	 * </p>
	 *
	 * @return {@code -1}
	 */
	@Override
	public long tell() {
		return -1;
	}

	/**
	 * Attempts to seek to the specified position.
	 * <p>
	 * Not supported for InputStream — always returns {@code -1}.
	 * </p>
	 *
	 * @param to the target position (ignored)
	 * @return {@code -1}
	 */
	@Override
	public long seek(long to) {
		return -1;
	}

	/**
	 * Returns the total length of the stream.
	 * <p>
	 * Not supported for InputStream — always returns {@code -1}.
	 * </p>
	 *
	 * @return {@code -1}
	 */
	@Override
	public long length() {
		return -1;
	}
}