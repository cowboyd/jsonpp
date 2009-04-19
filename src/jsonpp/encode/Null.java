package jsonpp.encode;


public class Null {
	public static Null INSTANCE = new Null();

	public String js() {
		return "null";
	}

	public String rb() {
		return "nil";
	}
}
