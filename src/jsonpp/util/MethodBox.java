package jsonpp.util;

import fr.umlv.jmmf.reflect.MultiMethod;
import fr.umlv.jmmf.reflect.MultiFactory;
import fr.umlv.jmmf.reflect.MultipleMethodsException;

import java.lang.reflect.InvocationTargetException;

import jsonpp.PPException;

public class MethodBox<T> {
	private MultiMethod method;
	private T defaultValue;
	private Object object;
	private boolean missing = false;
	private String name;

	public MethodBox(Object o, String name, T defaultValue) {
		this(o, name, 1);
		this.defaultValue = defaultValue;
	}

	public MethodBox(Object object, String name) {
		this(object, name, 1);
	}

	public MethodBox(Object object, String name, int arity) {
		this.name = name;
		this.object = object;
		try {
			this.method = MultiFactory.getDefaultFactory().create(object.getClass(), name, arity);
		} catch (IllegalArgumentException e) {
			if (e.getMessage().startsWith("no method")) {
				this.missing = true;
			} else {
				throw e;
			}
		}
	}

	public T call(Object... args) {
		if (this.missing) {
			if (this.defaultValue != null) {
				return defaultValue;
			} else {
				throw new PPException("missing required method: " + this.name + joinArgs(args) + " in class " + this.object.getClass());
			}
		}
		try {
			//noinspection unchecked
			return (T)this.method.invoke(this.object, args);
		} catch (IllegalAccessException e) {
			throw new PPException(e);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException) {
				throw ((RuntimeException)e.getCause());
			} else {
				throw new PPException(e.getCause());
			}
		} catch (NoSuchMethodException e) {
			if (defaultValue != null) {
				return defaultValue;
			} else {
				throw new PPException("missing required method: " + method.getName());
			}
		} catch (MultipleMethodsException e) {
			throw new PPException(e);
		}
	}

	public String joinArgs(Object... args) {
		StringBuffer buffer = new StringBuffer("(");
		int i = 0;
		for (Object arg : args) {
			i++;
			buffer.append(arg.getClass());
			if (i != args.length) {
				buffer.append(", ");
			}
		}
		buffer.append(")");
		return buffer.toString();
	}

	public static <T> MethodBox<T> mm(Object object, String name, T defaultValue) {
		return new MethodBox<T>(object, name, defaultValue);
	}

	public static <T> MethodBox<T> mm(Object object, String name, int arity) {
		return new MethodBox<T>(object, name, arity);
	}

	public static <T> MethodBox<T> mm(Object object, String name) {
		return new MethodBox<T>(object, name);
	}
}
