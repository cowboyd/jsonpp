package jsonpp;

import jsonpp.layout.CompactLayout;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class JSONPPTest {
	private JSONPP pp;
	private StringBuffer result = new StringBuffer();

	@Before
	public void create() {
		pp = new JSONPP(new StringBufferWriter(result), new TestCodec(), new CompactLayout());
	}

	@Test
	public void BasicObjectsAreMappedThroughByDefault() {
		ppCheck("'foo'", "foo");
		ppCheck("1", 1);
		ppCheck("true", true);
		ppCheck("false", false);
		ppCheck("1.0", 1.0);
		ppCheck("null", null);
		Object o = new Object();
		ppCheck("'" + o.toString() + "'", o);
	}

	@Test
	public void RenderAnEmptyMap() {
		ppCheck("{}", m());
	}

	@Test
	public void RenderAMapWithValues() {
		ppCheck("{foo: 'bar'}", m("foo", "bar"));
	}

	@Test
	public void RenderAMapWithMultipleValues() {
		ppCheck("{foo: 'bar',biz: 1,bang: true}", m("foo", "bar", "biz", 1, "bang", true));
	}

	@Test
	public void RenderAMapWithNestedMaps() {
		ppCheck("{foo: {bar: 'baz'},bang: 1}", m("foo",m("bar", "baz"),"bang", 1));
	}

	@Test
	public void RenderAnArray() {
		ppCheck("[1,true,5.0]", new Object[] {1, true, 5.0});
	}

	@Test
	public void RenderACollection() {
		LinkedHashSet<Object> set = new LinkedHashSet<Object>();
		set.add(1);
		set.add(true);
		set.add(5.0);
		ppCheck("[1,true,5.0]", set);
	}

	private void ppCheck(String expected, Object object) {
		result.setLength(0);
		pp.pp(object);
		assertEquals(expected, this.result.toString());
	}

	private static Map m(Object... elements) {
		assertEquals("map constructor must take even number of elements", 0, elements.length % 2);
		Map<Object, Object> m = new LinkedHashMap<Object, Object>();
		for (int i = 0; i < elements.length; i += 2) {
			m.put(elements[i], elements[i + 1]);
		}
		return m;
	}
}
