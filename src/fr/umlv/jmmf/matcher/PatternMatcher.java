package fr.umlv.jmmf.matcher;

import java.util.*;
import java.lang.reflect.*;

import fr.umlv.jmmf.reflect.*;
import fr.umlv.jmmf.util.NameLengthPair;

/** This class ease the use of multi-polymorphism.
    A simple way to use it consist in subclassing the
    <tt>PatternMatcher</tt> class.

    An example on a tree recursive type :
    <pre>
public class Test extends PatternMatcher
{
&nbsp;&nbsp;public interface Tree {}
&nbsp;&nbsp;public static class Leaf implements Tree {
&nbsp;&nbsp;&nbsp;&nbsp;Leaf(int value) {
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;this.value=value;
&nbsp;&nbsp;&nbsp;&nbsp;}
&nbsp;&nbsp;&nbsp;&nbsp;int value;
&nbsp;&nbsp;}
&nbsp;&nbsp;public static class Node implements Tree {
&nbsp;&nbsp;&nbsp;&nbsp;Node(Tree left,Tree right) {
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;this.left=left; this.right=right;
&nbsp;&nbsp;&nbsp;&nbsp;}
&nbsp;&nbsp;&nbsp;&nbsp;Tree left,right;
&nbsp;&nbsp;}

&nbsp;&nbsp;public int sum(Node node)
&nbsp;&nbsp;{ return sum(left)+sum(right); }
&nbsp;&nbsp;public int sum(Leaf leaf)
&nbsp;&nbsp;{ return value; }

&nbsp;&nbsp;public int sum(Tree tree)
&nbsp;&nbsp;{ return ((Integer)match("sum",tree)).intValue(); }

&nbsp;&nbsp;public static void main(String[] args) {
&nbsp;&nbsp;&nbsp;&nbsp;Tree tree=new Node(new Node(new Leaf(1),new Leaf(3)),new Leaf(7));
&nbsp;&nbsp;&nbsp;&nbsp;System.out.println("sum "+new Test().sum(tree));
&nbsp;&nbsp;}
}
    </pre>

    @author Remi Forax
    @version 0.8.2
 */
public class PatternMatcher
{
  /** create a pattern matcher component.
      Find all multi-methods in this component and store them.
   */
  public PatternMatcher()
  {
    this.bean=this;
    this.beanClass=getClass();
    this.factory=MultiFactory.getDefaultFactory();
  }

  /** create a pattern matcher component on a bean object.
      Find all multi-methods in the bean component and store them.
   */
  public PatternMatcher(Object bean)
  {
    this(bean,MultiFactory.getDefaultFactory());
  }

  /** create a pattern matcher component on a bean object.
      Find all multi-methods in the bean component and store them.
   */
  public PatternMatcher(Object bean,MultiFactory factory)
  {
    this.bean=bean;
    this.beanClass=bean.getClass();
    this.factory=factory;
  }

  private final MultiMethod getMultiMethod(String name,int argLength)
  {
    _pair.init(name,argLength);
    Object o=multimethods.get(_pair);
    if (o==null)
    {
      MultiMethod mm=factory.create(beanClass,name,argLength);
      multimethods.put(new NameLengthPair(_pair),mm);
      return mm;
    }
    else
      return (MultiMethod)o;
  }

  /** lookup the best method in the multi-method named name and invoke it.
      @param name name of the multi-method.
      @param arg first parameter of the multi-method.
      @exception NoMatchingMethodException there is no method that
        match the parameter arguments.
      @exception MultipleMatchingMethodsException there is no method that
        match the parameter arguments.
      @exception MatchingMethodInvocationException throws by
        the target method invoked.
      @exception IllegalStateException is method find isn't accessible,
        due to an internal error.
      @return the result of a call of the best method of the multi-method.
       if the type of the result is a primitive type, the result
       is wrapped in the corresponding object (int -> Integer, etc...).

      @see #match(String, Object[], Class[])
   */
  public Object match(String name,Object arg)
  {
    return match(name,new Object[]{arg},new Class[]{arg.getClass()});
  }

  /** lookup the best method in the multi-method named name and invoke it.
      @param name name of the multi-method.
      @param args parameters of the multi-method.
      @exception NoMatchingMethodException there is no method that
        match the parameter arguments.
      @exception MultipleMatchingMethodsException there is no method that
        match the parameter arguments.
      @exception MatchingMethodInvocationException throws by
        the target method invoked.
      @exception IllegalStateException is method find isn't accessible,
        due to an internal error.
      @return the result of a call of the best method of the multi-method.
       if the type of the result is a primitive type, the result
       is wrapped in the corresponding object (int -> Integer, etc...).

      @see #match(String, Object[], Class[])
   */
  public Object match(String name,Object[] args)
  {
    int index=args.length;
    Class types[]=new Class[index];
    for(;--index>=0;)
      types[index]=args[index].getClass();
    return match(name,args,types);
  }

  /** lookup the best method in the multi-method named name and invoke it.
      @param name name of the multi-method.
      @param args parameters of the multi-method.
      @param classes type of the parameter.
      @exception NoMatchingMethodException there is no method that
        match the parameter arguments.
      @exception MultipleMatchingMethodsException there is no method that
        match the parameter arguments.
      @exception MatchingMethodInvocationException throws by
        the target method invoked.
      @exception IllegalStateException is method find isn't accessible,
        due to an internal error.
      @return the result of a call of the best method of the multi-method.
       if the type of the result is a primitive type, the result
       is wrapped in the corresponding object (int -> Integer, etc...).
   */
  public Object match(String name,Object[] args, Class[] classes)
  {
    try
    {
      return getMultiMethod(name,args.length).
        invoke(bean,args,classes);
    }
    catch(IllegalAccessException e)
    {
      throw new IllegalStateException("illegal access : see the "+
        "method/class modifiers");
    }
    catch(InvocationTargetException e)
    {
      Throwable throwable=e.getTargetException();
      if (throwable instanceof RuntimeException)
        throw (RuntimeException)throwable;

      if (throwable instanceof Error)
        throw (Error)throwable;

      throw new MatchingMethodInvocationException((Exception)throwable);
    }
    catch(NoSuchMethodException e)
    {
      throw new NoMatchingMethodException(e);
    }
    catch(MultipleMethodsException e)
    {
      throw new MultipleMatchingMethodsException(e);
    }
  }

  // pair of name and method length declared here to avoid allocation
  private transient NameLengthPair _pair=new NameLengthPair();

  private Class beanClass;
  private Object bean;
  private MultiFactory factory;
  private HashMap multimethods=new HashMap();
}
