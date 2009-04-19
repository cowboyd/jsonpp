package jsonpp;


public class PPException extends RuntimeException {
	public PPException(Throwable t) {
		super(t);
	}

	public PPException(String message) {
		super(message);
	}
}
