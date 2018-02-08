package dk.pkg.xmputil4j.core;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link InputStream} that returns XMP data from a JPEG.
 */
public class JpegXmpInputStream extends InputStream {
	private static final char[] XMP_NAMESPACE_NULL_TERMINATED = ("http://ns.adobe.com/xap/1.0/" + (char)0).toCharArray();
	private static final int XMP_JPEG_APP1_CHAR1 = 0xFF;
	private static final int XMP_JPEG_APP1_CHAR2 = 0xE1;

	private static final int INITIAL_STATE = 0;
	private static final int BEGINNING_OF_APP1_FOUND_STATE = 1;
	private static final int END_OF_APP1_FOUND_STATE = 2;
	private static final int XMP_LENGTH_1_FOUND_STATE = 3;
	private static final int XMP_LENGTH_2_FOUND_STATE = 4;
	private static final int IN_XMP_STATE = 5;

	private final InputStream jpegIn;
	private int xmpLengthRaw1;
	private int xmpLengthRaw2;
	private int xmpLeft;
	private int state = INITIAL_STATE;

	private final int READ_NO_MORE_THAN_BYTES;

	/**
	 * Creates a {@link JpegXmpInputStream} that returns XMP data from the JPEG
	 * data found in <code>jpegIn</code>.
	 */
	public JpegXmpInputStream(final InputStream jpegIn)
	{
		this.jpegIn = jpegIn;
		READ_NO_MORE_THAN_BYTES = 0;
	}

	public JpegXmpInputStream(final InputStream jpegIn, int readNoMoreThanBytes)
	{
		this.jpegIn = jpegIn;
		this.READ_NO_MORE_THAN_BYTES = readNoMoreThanBytes;
	}

	/**
	 * Seeks to the XMP data and (if present) returns XMP data from it
	 *
	 * @see InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		// Look for APP1 marker
		int ch = 0;
		int byteIndex = 0;
		while ((ch = jpegIn.read()) != -1) {

			if (READ_NO_MORE_THAN_BYTES > 0 && byteIndex++ > READ_NO_MORE_THAN_BYTES && state == INITIAL_STATE) {
				// read READ_NO_MORE_THAN_BYTES and xmp header still not found
				// don't look further (scanning big images without any xmp information can take long)
				return -1;
			}

			switch (state) {
			case INITIAL_STATE:
				if (ch == XMP_JPEG_APP1_CHAR1) {
					state = BEGINNING_OF_APP1_FOUND_STATE;
				} else {
					// Not beginning of APP1 marker
					state = INITIAL_STATE;
				}
				break;
			case BEGINNING_OF_APP1_FOUND_STATE:
				if (ch == XMP_JPEG_APP1_CHAR2) {
					state = END_OF_APP1_FOUND_STATE;
				} else {
					// Not end of APP1 marker
					state = INITIAL_STATE;
				}
				break;
			case END_OF_APP1_FOUND_STATE:
				xmpLengthRaw1 = ch;
				state = XMP_LENGTH_1_FOUND_STATE;
				break;
			case XMP_LENGTH_1_FOUND_STATE:
				xmpLengthRaw2 = ch;
				state = XMP_LENGTH_2_FOUND_STATE;
				break;
			case XMP_LENGTH_2_FOUND_STATE:
				// Look for name space
				if (isBeginsWith(ch, jpegIn, XMP_NAMESPACE_NULL_TERMINATED)) {
					final int xmpLength =
						((xmpLengthRaw1 << 8) + (xmpLengthRaw2 << 0)) // Same as DataInputStream.readUnsignedShort()
						- 2 // APP1 marker length
						- XMP_NAMESPACE_NULL_TERMINATED.length; // XMP name space length

					xmpLeft = xmpLength;
					state = IN_XMP_STATE;
				} else {
					// Not XMP data
					state = INITIAL_STATE;
				}
				break;
			case IN_XMP_STATE:
				if (xmpLeft-- > 0) {
					return ch;
				}

				// No more XMP data
				return -1;
			default:
				throw new IllegalStateException("Unsupported state: " + state);
			}
		}

		return -1;
	}

	/**
	 * @param firstRead character already read from <code>inputStream</code>
	 * @return <code>true</code> if <code>inputStream</code> starts with
	 *         <code>prefix</code> at the current position in the stream
	 */
	private static boolean isBeginsWith(int firstRead,
			InputStream inputStream, char[] prefixChars) throws IOException {
		int read = firstRead;

		for (int i = 0; i < prefixChars.length; i++) {
			final char prefixChar = prefixChars[i];

			if (read == -1 || read != prefixChar) {
				return false;
			}

			if (i < prefixChars.length-1) {
				read = inputStream.read();
			}
		}

		return true;
	}

	/** Closes the JPEG stream */
	@Override
	public void close() throws IOException {
		jpegIn.close();
	}
}
