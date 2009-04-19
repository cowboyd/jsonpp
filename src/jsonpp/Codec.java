package jsonpp;

import java.util.Iterator;
import java.util.Map;


public interface Codec {

	public String encodeKey(Object key);

	public String encodeValue(Object value);

	public Iterator<Map.Entry> entries(Object o);

	public Iterator iterator(Object o);

}
