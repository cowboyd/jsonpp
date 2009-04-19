package jsonpp.encode;

import jsonpp.Codec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


public class DefaultCodec implements Codec {

	
	public String encodeKey(Object key) {
		return quote(key);
	}

	public String encodeKey(String key) {
		return key;
	}

	public String encodeValue(Object value) {
		return quote(value);
	}

	public String encodeValue(Number number) {
		return number.toString();
	}

	public String encodeValue(Boolean bool) {
		return bool ? "true" : "false";
	}

	public String encodeValue(Null nil) {
		return nil.js();
	}

	public Iterator<Map.Entry> entries(Object o) {
		return null;
	}

	public Iterator entries(Map map) {
		return map.entrySet().iterator();
	}

	public Iterator iterator(Object o) {
		return null;
	}

	public Iterator iterator(Object[] sequence) {
		return Arrays.asList(sequence).iterator();
	}

	public Iterator iterator(Collection c) {
		return c.iterator();
	}


	private String quote(Object value) {
		return String.format("\"%s\"", value.toString());
	}

}
