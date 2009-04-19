/*
 * Java MultiMethod Framework API 0.8
 *
 * $Id$
 *
 * Copyright (C) 1999-2001 Remi Forax <forax@univ-mlv.fr>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package fr.umlv.jmmf.reflect;

import java.util.*;
import java.lang.reflect.*;

/** The multi-method object.

    This class support the 1.2 security model.
    The current implementation of the multi-method mecanism
    desactivate default Java language access control checks.
    see {@link java.lang.reflect.AccessibleObject AccessibleObject}.

    The method {@link #create(Class,String,int) create} permit to
    create a multi-method object.

    <pre>
import fr.umlv.jmmf.reflect.*;

public class Test {
&nbsp;&nbsp;public interface A {}
&nbsp;&nbsp;public static class B implements A {}
&nbsp;&nbsp;public static class C implements A {
&nbsp;&nbsp;  int value;
&nbsp;&nbsp;}

&nbsp;&nbsp;static public String toString(B b) {
&nbsp;&nbsp;  return b.toString();
&nbsp;&nbsp;}
&nbsp;&nbsp;static public String toString(C c) {
&nbsp;&nbsp;  return String.valueOf(c.value);
&nbsp;&nbsp;}

&nbsp;&nbsp;public static void main(String[] args)
&nbsp;&nbsp;&nbsp;&nbsp;throws Exception
&nbsp;&nbsp;{
&nbsp;&nbsp;&nbsp;&nbsp;MultiMethod mm=MultiMethod.create(Test.class,"toString",1);

&nbsp;&nbsp;&nbsp;&nbsp;A a=new B();
&nbsp;&nbsp;&nbsp;&nbsp;System.out.println(mm.invoke(null,new Object[]{a}));
&nbsp;&nbsp;&nbsp;&nbsp;a=new C();
&nbsp;&nbsp;&nbsp;&nbsp;System.out.println(mm.invoke(null,new Object[]{a}));
&nbsp;&nbsp;}
}
    </pre>

    @author Remi Forax
    @version 0.9
 */
public final class MultiMethod {

  /** construct a multi-method by taking
      an implementation.
      This method is called by MultiMethodFactory.create()
   */
  MultiMethod(Impl impl,boolean onlyPublic) {

    this.impl=impl;
    this.onlyPublic=onlyPublic;

    // PERF: perhaps a lazy allocation ??
    this.dispatchMap=new DispatchMap();
  }

  /** continue initialisation.
   */
  void init(Class declaringClass) {
    // find declaring class entry
    this.hostEntry=impl.getTypeSupport().getClassEntry(declaringClass);

    // create declaring class dispatch entry
    DispatchMap.Entry hostDispatchEntry=createHostDispatchEntry(
      declaringClass);

    if (hostDispatchEntry==null)
      throw new IllegalArgumentException("no method "+getName()
        +" with "+getParameterLength()+" parameter(s) in "
        +getDeclaringClass());

    // end data structure construction
    impl.endsConstruction();

    // init declaring dispatch entry field
    this.hostDispatchEntry=hostDispatchEntry;
  }

  /** call to update implementation
   */
  void update(Impl impl) {
    //System.out.println("MultiMethod.update: "+impl);
    this.impl=impl;
  }

  /** return the most specific method of the multi-method according
      to the type of all items if args array on a specified target
      object.

      @param target class in which the most specific method is declared.
      @param types parameter types used to find the most specific method.

      @exception NoSuchMethodException, there is no method that
        match the parameter arguments.
      @exception MultipleMethodsException there are multiple methods that
        match the parameter arguments.

      @return the most specific method.

      @see java.lang.Class#getMethod(String,Class[])
      @see #invoke(Object, Object[], Class[])
   */
  public Method getMethod(Class target,Class[] types)
    throws NoSuchMethodException, MultipleMethodsException {

    Method method;
    if (target==null)
      method=getStaticMethod(types);
    else
      method=getTargetMethod(target,types);
    return method;
  }

  /** call the most specific method of the multi-method according
      to the type of all items if args array on a specified target
      object.

      @param target object on which the most specific method is called.
      @param args arguments of the multi-method, the types on this
       arguments is used to called the most specific method.

      @exception NoSuchMethodException, there is no method that
        match the parameter arguments.
      @exception MultipleMethodsException there are multiple methods that
        match the parameter arguments.
      @exception InvocationTargetException if the most specific
       method called raise an exception during its execution,
       this exception is wrapped into the InvocationTargetException.
      @exception IllegalArgumentException if the target class isn't
       a subtype of the declaring class.
      @exception IllegalAccessException if most specific method
       isn't accessible.
      @return the result of the called of the most specific method.

      @see java.lang.reflect.Method#invoke(Object,Object[])
      @see #invoke(Object, Object[], Class[])
   */
  public Object invoke(Object target,Object[] args)
    throws IllegalAccessException, InvocationTargetException,
           NoSuchMethodException, MultipleMethodsException {

    Method method;
    if (target==null)
      method=getStaticMethod(args);
    else
      method=getTargetMethod(target.getClass(),args);
    return method.invoke(target,args);
  }

  /** call the most specific method of the multi-method according
      to the type of all items if args array on a specified target
      object.

      @param target object on which the most specific method is called.
      @param args arguments of the multi-method,
      @param types the most specific method of the multi-method is
       called according to this types.

      @exception NoSuchMethodException, there is no method that
        match the parameter types.
      @exception MultipleMethodsException there are multiple methods
        that match the parameter arguments.
      @exception InvocationTargetException if the most specific
       method called raise an exception during its execution,
       this exception is wrapped into the InvocationTargetException.
      @exception IllegalArgumentException if the target class isn't
       a subtype of the declaring class.
      @exception IllegalAccessException if most specific method isn't
       accessible.
      @return the result of the called of the most specific method.

      @see java.lang.reflect.Method#invoke(Object,Object[])
   */
  public Object invoke(Object target,Object[] args,Class[] types)
    throws IllegalAccessException, InvocationTargetException,
           NoSuchMethodException, MultipleMethodsException {

    Method method;
    if (target==null)
      method=getStaticMethod(types);
    else
      method=getTargetMethod(target.getClass(),types);
    return method.invoke(target,args);
  }

  /**
   */
  private Method getTargetMethod(Class target,Class[] args)
    throws NoSuchMethodException, MultipleMethodsException {

    //System.out.println("getTargetMethod "+target);

    DispatchMap.Entry entry=dispatchMap.get(target);
    if (entry==null) {
      // test if the target class is a subtype of the current class
      TypeSupport.ClassEntry targetEntry=
        impl.getTypeSupport().getClassEntry(target);

      if (!hostEntry.isAssignableFrom(targetEntry))
        throw new IllegalArgumentException("bad target class");

      // get infos from implementation and
      // process dispatch table of target class
      entry=createDispatchEntry(target,new ArrayList());

      // end data structure construction
      impl.endsConstruction();
    }

    int index=impl.getMethodIndex(args,entry.visibility);
    return entry.dispatchTable[index];
  }

  /**
   */
  private Method getStaticMethod(Class[] args)
    throws NoSuchMethodException, MultipleMethodsException {

    //System.out.println("getStaticMethod "+hostDispatchEntry.clazz);

    DispatchMap.Entry entry=hostDispatchEntry;
    int index=impl.getMethodIndex(args,entry.staticVisibility);

    return entry.dispatchTable[index];
  }

  /**
   */
  private Method getTargetMethod(Class target,Object[] args)
    throws NoSuchMethodException, MultipleMethodsException {

    //System.out.println("getMethod "+target);

    DispatchMap.Entry entry=dispatchMap.get(target);
    if (entry==null) {
      // test if the target class is a subtype of the current class
      TypeSupport.ClassEntry targetEntry=
        impl.getTypeSupport().getClassEntry(target);

      if (!hostEntry.isAssignableFrom(targetEntry))
        throw new IllegalArgumentException("bad target class");

      // get infos from implementation and
      // process dispatch table of target class
      entry=createDispatchEntry(target,new ArrayList());

      // end data structure construction
      impl.endsConstruction();
    }

    int index=impl.getMethodIndex(args,entry.visibility);
    return entry.dispatchTable[index];
  }

  /**
   */
  private Method getStaticMethod(Object[] args)
    throws NoSuchMethodException, MultipleMethodsException {

    //System.out.println("getStaticMethod "+hostDispatchEntry.clazz);

    DispatchMap.Entry entry=hostDispatchEntry;
    int index=impl.getMethodIndex(args,entry.staticVisibility);

    //System.out.println("getIndex "+index);

    return entry.dispatchTable[index];
  }

  /** return null if there is no method with the good name
      and the good parameter length.
   */
  private DispatchMap.Entry createHostDispatchEntry(Class target) {

    // DEBUG
    // System.out.println("create host dispatch entry "+target);

    String name=impl.getName();
    int argLength=impl.getArgLength();
    
    BitMask visibility=new Bit32Mask();
    BitMask staticVisibility=new Bit32Mask();

    ArrayList list=new ArrayList();

    for(Class type=target;type!=null;
      type=type.getSuperclass()) {

      Method[] methods=getDeclaredMethods(type);
      for(int i=methods.length;--i>=0;) {

        Method method=methods[i];
        if (!method.getName().equals(name))
          continue;

        Class[] parameterTypes=method.getParameterTypes();
        if (parameterTypes.length!=argLength)
          continue;

        // find index of the method
        // OLD VERSION
        // int index=methodMap.getIndex(parameterTypes);
        int index=impl.getMapIndex(parameterTypes);

        // DEBUG
        // System.out.println(method+" "+index);

        // if its a new method and if the method is visible
        int listSize=list.size();
        if ((index>=listSize || list.get(index)==null)
           && isVisible(target,type,method)) {

          // process annotation and partial order
          // for the current method
          impl.addMethod(parameterTypes,index);

          // insert method in list
          if (index>=listSize) {
            list.ensureCapacity(index+1);
            for(int j=listSize;j<index;j++)
              list.add(null);
            list.add(method);
          } else {
            list.set(index,method);
          }

          // add bit that corresponding to the current
          // method in the visibility mask
          if (Modifier.isStatic(method.getModifiers()))
            staticVisibility=staticVisibility.set(index);

          visibility=visibility.set(index);
        }
      }

      // don't need to traverse hierarchy to obtain public
      // methods in relective API
      if (onlyPublic)
        break;
    }

    // DEBUG
    //System.out.println("end of create dispatch table");

    int listSize=list.size();
    if (listSize==0)
      return null;

    Method[] table=(Method[])list.toArray(
      new Method[listSize]);
      
    bypassSecurity(table);
    
    return dispatchMap.put(target,visibility,
      staticVisibility,table);
  }

  /**
   */
  private DispatchMap.Entry createDispatchEntry(Class target,ArrayList list) {

    // DEBUG
    // System.out.println("create dispatch entry "+target);

    BitMask visibility=new Bit32Mask();
    BitMask staticVisibility=new Bit32Mask();

    Class type=target.getSuperclass();
    if (type!=null) {
      DispatchMap.Entry parent;
      Object o=dispatchMap.get(type);
      if (o==null) {
        parent=createDispatchEntry(type,list);

      } else {
        parent=(DispatchMap.Entry)o;

        //Method[] dispatch=parent.dispatchTable;
        //int size=dispatch.length;
        //for(int j=0;j<size;j++)
        //  list.add(dispatch[j]);
        list.addAll(Arrays.asList(parent.dispatchTable));
      }

      visibility=visibility.or(parent.visibility);
      staticVisibility=staticVisibility.or(parent.staticVisibility);
    }

    String name=impl.getName();
    int argLength=impl.getArgLength();
    //MethodMap methodMap=impl.getMethodMap();

    // DEBUG
    //System.out.println("create dispatch table "+target);


    // PREF, preprocess list size before used it
    // and use an array instead

    Method[] methods=getDeclaredMethods(target);
    for(int i=methods.length;--i>=0;) {

       Method method=methods[i];
       if (!method.getName().equals(name))
         continue;

       Class[] parameterTypes=method.getParameterTypes();
       if (parameterTypes.length!=argLength)
         continue;

       // find index of the method
       int index=impl.getMapIndex(parameterTypes);

       // DEBUG
       // System.out.println("index "+index);

       // if its a new method
       if (index>=list.size()) {

        // process annotation and partial order
        // for the current method
        impl.addMethod(parameterTypes,index);

         // insert method in list
         list.ensureCapacity(index+1);
         for(int j=list.size();j<index;j++)
           list.add(null);
         list.add(method);
       }
       // replace an old method
       else {

         // MUST deal with visibility
         Method old=(Method)list.get(index);

         // if there is no old method or
         // if the old method is visible from the new method
         if (old==null || isVisible(target,method,old)) {

           // replace an old method
           list.set(index,method);
         }
       }

       // add bit that corresponding to the current
       // method in the visibility mask
       if (Modifier.isStatic(method.getModifiers()))
         staticVisibility=staticVisibility.set(index);

       visibility=visibility.set(index);
    }


    // DEBUG
    // System.out.println("end of create dispatch table");

    Method[] table;
    int size=list.size();
    if (size!=0) {
      table=(Method[])list.toArray(new Method[size]);
    
      bypassSecurity(table);
    }
    else
      table=null;

    return dispatchMap.put(target,visibility,
      staticVisibility,table);
  }
  
  /** bypass JLS modifiers protection
   *  this method is still under discussion
   */
  private static void bypassSecurity(Method[] methods) {
    
    try {
      //AccessibleObject.setAccessible(methods, true);
      
      for(int i=0;i<methods.length;i++) {
        Method method=methods[i];
        if (method!=null)
          method.setAccessible(true);
      }
    }
    catch(SecurityException e) {
    } 
  }

  /** return delared method of a class.
   */
  private Method[] getDeclaredMethods(Class clazz) {
    if (onlyPublic)
      return clazz.getMethods();
    else
      return clazz.getDeclaredMethods();
  }

  /** test is the method is visible from the hostClass.
   */
  private static boolean isVisible(Class hostClass,
    Class methodClass,Method method) {
    if (hostClass==methodClass)
      return true;

    int modifiers=method.getModifiers();
    if (Modifier.isPrivate(modifiers))
      return false;

    if (Modifier.isPublic(modifiers) ||
        Modifier.isProtected(modifiers))
      return true;

    return hostClass.getPackage()==methodClass.getPackage();
  }

  /** test is the old method is visible for the new one.
   */
  private static boolean isVisible(Class newClass,Method newOne,
    Method oldOne) {
    int oldModifiers=oldOne.getModifiers();
    if (Modifier.isPrivate(oldModifiers))
      return false;

    if (Modifier.isPublic(oldModifiers) ||
        Modifier.isProtected(oldModifiers))
      return true;

    return oldOne.getDeclaringClass().getPackage()==
      newClass.getPackage();
  }

  /** return a string representation of the multi-method.
   */
  public String toString() {
    return super.toString()+" ("+
      getDeclaringClass().getName()+','+getName()+','+
      getParameterLength()+')';
  }

  /** return true is the multi-method is constructed
      using the onlyPublic flag.

      @see MultiFactory#create(Class,String,int,boolean)
   */
  public boolean isOnlyPublic() {
    return onlyPublic;
  }

  /** return the number of parameters of the current
      multi-method.
   */
  public int getParameterLength() {
    return impl.getArgLength();
  }

  /** return the name of the current multi-method.
   */
  public String getName() {
    return impl.getName();
  }

  /** return the class on which the multi-method is
      declared.
   */
  public Class getDeclaringClass() {
    return hostEntry.clazz;
  }

  Impl impl;
  boolean onlyPublic;
  DispatchMap dispatchMap;

  DispatchMap.Entry hostDispatchEntry;
  TypeSupport.ClassEntry hostEntry;

  /** construct a multi-method by taking all method named name
      with parameterLength parameter(s) in class clazz.

      @param clazz class which owns the named methods.
      @param name name of the methods.
      @param parameterLength number of parameters.

      @exception IllegalArgumentException if the class
       clazz contains no method named name or all methods
       found don't have the same number of parameters.

      @see MultiFactory
      @see MultiFactory#create(Class,String,int)
   */
  public static MultiMethod create(Class clazz,String name,
    int parameterLength) {
    return MultiFactory.getDefaultFactory().
      create(clazz,name,parameterLength);
  }
}
