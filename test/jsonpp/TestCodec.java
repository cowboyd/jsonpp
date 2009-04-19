package jsonpp;

import jsonpp.encode.DefaultCodec;

public class TestCodec extends DefaultCodec {

	@Override
	public String encodeKey(Object key) {
		return "'" + key + "'";
	}

	public String encodeValue(Object value) {
		return "'" + value + "'";
	}
}
