package jsonpp;

import java.io.Writer;
import java.io.IOException;

public class StringBufferWriter extends Writer {
	private StringBuffer buffer;

	public StringBufferWriter(StringBuffer buffer) {
		this.buffer = buffer;
	}

	public void write(char[] chars, int i, int i1) throws IOException {
		this.buffer.append(chars, i, i1);
	}

	public void flush() throws IOException {

	}

	public void close() throws IOException {

	}
}
