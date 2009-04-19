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

package fr.umlv.jmmf.adapter;

import java.util.*;
import java.lang.reflect.*;

import fr.umlv.jmmf.util.*;
import fr.umlv.jmmf.reflect.*;

/** a factory for a generated specialized adapter.

    There 3 parts in the factory interface :
    <ul>
     <li>the methods setKeepAdapteeInterfaces, isKeepAdapteeInterfaces,
       and addInterfaces, this methods permit to describe the
       interface of a generated adapter.
     <li>the methods addMultiMethod provide implementation methods
       of a generated adapter.
     <li>the method create, create a specilized adapter for an object.
    </ul>

    All adapter created by this factory implements the Adapter interface.

    <pre>
  code code code
    </pre>

    TO DO: cache generated classes.
           must improve object allocations.

    @author Remi Forax
    @version 0.8.4

    @see Adapter
 */
public class AdapterFactory
{
  class Handler implements InvocationHandler
  {
    Handler(Object adaptee,HashMap vhash)
    {
      this.vhash=vhash;
      this.adaptee=adaptee;
    }

    /** return the current adaptee object.
     */
    private Object getAdaptee()
    { return adaptee; }

    /** return the current adapter factory in use.
     */
    private AdapterFactory getFactory()
    { return AdapterFactory.this; }

    public Object invoke(Object proxy,Method method,Object[] args)
      throws Throwable
    {
      HashMap vhash=this.vhash;
      Object adaptee=this.adaptee;

      Object o=vhash.get(method);
      if (o==NO_MULTIMETHOD)
        return defaultTraversal(method,adaptee,proxy,args);

      if (o==null)
      {
        o=processImpl(adaptee,proxy,method,vhash);
        vhash.put(method,o);

        if (o==NO_MULTIMETHOD)
          return defaultTraversal(method,adaptee,proxy,args);
      }

      // invoke multi-method
      try
      {
        return ((Impl)o).invoke(adaptee,proxy,args);
      }
      catch(InvocationTargetException e)
      { throw e.getTargetException(); }
    }

    private Object processImpl(Object adaptee,Object adapter,
      Method method,HashMap vhash)
    {
      // process multimethod
      String name=method.getName();

      // process Adapter special methods
      if (name.equals("getAdaptee"))
      {
        return createImpl(VALUE_GET_ADAPTEE,null,null);
      }
      else
        if (name.equals("getFactory"))
        {
          return createImpl(VALUE_GET_FACTORY,null,null);
        }

      Class returnType=method.getReturnType();
      Class[] types=method.getParameterTypes();
      int argLength=types.length;

      // process skeletons
      Class[] skeletons=null;
      if (types!=null)
        skeletons=processSkeletons(types);

      // process wrappability
      boolean wrappable=returnType.isInterface();

      // process global multi-methods
      NameLengthPair pair=new NameLengthPair(name,argLength);
      Object o=multimethods.get(pair);
      if (o==null)
      {
        // process fields
        int methodValue=VALUE_NONE;
        if (name.length()>3)  // search setXXX and getXXX
        {
          if (argLength==0 && name.startsWith("get"))
            methodValue=VALUE_GET;
          else
            if (argLength==1 && name.startsWith("set"))
              methodValue=VALUE_SET;
        }

        if (methodValue>VALUE_NONE)
        {
          String fieldName=Character.toLowerCase(name.charAt(3))
            +name.substring(4);

          HashMap fields=AdapterFactory.this.fields;
          if (fields!=null)
          {
            Object ofe=fields.get(fieldName);
            if (ofe!=null)
            {
              FieldEntry fieldEntry=(FieldEntry)ofe;
              return createImpl(methodValue,fieldEntry.map,
                fieldEntry.defaultValue);
            }
          }
        }

        // process local multi-methods
        try
        {
          MultiFactory factory=multiFactory;
          if (factory==null)
          {
            multiFactory=MultiFactory.getDefaultFactory();
            factory=multiFactory;
          }

          return createImpl(
            factory.create(adaptee.getClass(),name,argLength),
            skeletons,true,wrappable,SEND_NONE,null);
        }
        catch(IllegalArgumentException e)
        {
          return NO_MULTIMETHOD;
        }
      }
      else
      {
        MMEntry mmEntry=(MMEntry)o;
        return createImpl(mmEntry.mm,skeletons,false,wrappable,
          mmEntry.invocationOption,mmEntry.target);
      }
    }

    private Class[] processSkeletons(Class[] types)
    {
      Class[] skeletons=null;

      int length=types.length;
      for(int i=length;--i>=0;)
      {
        Class type=types[i];
        if (type.isPrimitive() || Modifier.isFinal(type.getModifiers()))
        {
          if (skeletons==null)
            skeletons=new Class[length];
          skeletons[i]=types[i];
        }
      }

      return skeletons;
    }

    private HashMap vhash;
    private Object adaptee;
  }


  /** construct a default adapter factory.
   */
  public AdapterFactory()
  {
    // all adapter implements Adapter interface
    addInterface(Adapter.class);
  }

  /** garantee unicity of association between adaptee and adapter.
      There is only one adapter for an adaptee.
      adapter unicity is false by default.
   */
  public void setAdapterUnicity(boolean unicity)
  {
    if (unicity)
    {
      if (instances==null)
        instances=new WeakHashMap();
    }
    else
      instances=null;
  }

  /** set the multi-method factory for adaptee.
   */
  public void setAdapteeMultiFactory(MultiFactory multiFactory)
  { this.multiFactory=multiFactory; }

  /** set the transparency flag.
      if this flag is true, the interfaces of the adaptee is
      implemented by the generic adapter.

      @param transparency the new value of the flag.
   */
  public void setTransparency(boolean transparency)
  { this.transparency=transparency; }

  /** get the transparency flag.
      if this flag is true, the interfaces of the adaptee is
      implemented by the generic adapter.

      @return the value of the transparency flag.
   */
  public boolean getTransparency()
  { return transparency; }

  /**
   */
  public void setWrappable(Class aClass,boolean wrappable)
  {
    wrappables.put(aClass,(wrappable)?Boolean.TRUE:Boolean.FALSE);
  }

  /** add an interface for all generated adapter.
      @param interfaze interface that all generated adapter must implements.
   */
  public void addInterface(Class interfaze)
  {
    // trick, interface of all adaptee are stored in java.lang.Object
    addInterface(Object.class,interfaze,false);
  }

  /** add an interface for all adapter for which the adaptee
      implements adapteeClass.
      @param adapteeClass class of one adaptee.
      @param interfaze all generated adapter that wrapped an adaptee
       that implements the adapteeClass must implements this interface.
   */
  public void addInterface(Class adapteeClass,Class interfaze)
  {
    addInterface(adapteeClass,interfaze,true);
  }

  /** add an interface for all adapter for which the adaptee
      implements adapteeClass.
      @param adapteeClass class of one adaptee.
      @param interfaze all generated adapter that wrapped an adaptee
       that implements the adapteeClass must implements this interface.
      @param wrappable a flag that

      @see #setWrappable(Class,boolean)
   */
  public void addInterface(Class adapteeClass,Class interfaze,
    boolean wrappable)
  {
    if (!interfaze.isInterface())
      throw new IllegalArgumentException(""+interfaze+" isn't an interface");

    ArrayList list=(ArrayList)interfaces.get(adapteeClass);
    if (list==null)
    {
      list=new ArrayList();
      interfaces.put(adapteeClass,list);
    }
    list.add(interfaze);

    setWrappable(adapteeClass,wrappable);
  }

  /** add multimethod as an implementation of all adapter.
      @param target target of the multi-method, the target may be null.
      @param mm multi-method.
   */
  public void addMultiMethod(Object target,MultiMethod mm)
  {
    int argLength=mm.getParameterLength();
    NameLengthPair pair=new NameLengthPair(mm.getName(),argLength);
    if (multimethods==null)
      multimethods=new HashMap();

    multimethods.put(pair,new MMEntry(target,mm,SEND_NONE));
  }

  /** add multimethod for an adaptee class.
      @param target target of the multi-method, the target may be null.
      @param mm multi-method.
      @param invocationOption which first parameter is send
       to the multi-method.
       <br>
       This flag could contains three differents values :
       <dl>
        <dt>SEND_NONE <dd>if there no new first parameter.
        <dt>SEND_ADAPTEE <dd>if the adaptee is send as first parameter.
        <dt>SEND_ADAPTER <dd>if the adapter is send as first parameter.
       </dl>
   */
  public void addMultiMethod(Object target,MultiMethod mm,int invocationOption)
  {
    int argLength=mm.getParameterLength();
    if (invocationOption==SEND_ADAPTEE ||
        invocationOption==SEND_ADAPTER)
      argLength--;

    NameLengthPair pair=new NameLengthPair(mm.getName(),argLength);
    if (multimethods==null)
      multimethods=new HashMap();

    multimethods.put(pair,new MMEntry(target,mm,invocationOption));
  }

  /** ask to create a new field for all adapters.
      @param name name of the field.
   */
  public void addField(String name)
  {
    addField(name,null,new HashMap());
  }

  /** ask to create a new field for all adapters.
      @param name name of the field.
      @param defaultValue initial value of the field.
   */
  public void addField(String name,Object defaultValue)
  {
    addField(name,defaultValue,new HashMap());
  }

  /** ask to create a new field for all adapters.
      @param name name of the field.
      @param defaultValue initial value of the field.
      @param map associative map between adaptee and field value.
   */
  public void addField(String name,Object defaultValue,Map map)
  {
    if (fields==null)
      fields=new HashMap();

    FieldEntry entry=new FieldEntry(defaultValue,map);
    fields.put(name,entry);
  }

  /** generate a specialized adapter.
      @param adaptee object wrapped in the adapter.
      @return a specialized adapter that implements all interfaces.
   */
  // The return type of this method could be Adapter
  // but a cast in Adapter object isn't necessary in 99% of use cases.
  public Object create(Object adaptee)
  {
    // search in instances cache
    Object adapter;
    WeakHashMap instances=this.instances;
    if (instances!=null)
    {
      adapter=instances.get(adaptee);
      if (adapter!=null)
        return adapter;
    }

    Class adapteeClass=adaptee.getClass();

    // get local multimethods hashmap
    HashMap vhash=(HashMap)vhashes.get(adapteeClass);
    if (vhash==null)
    {
      vhash=new HashMap(11);
      vhashes.put(adapteeClass,vhash);
    }

    Constructor constructor;
    Object cons=cache.get(adapteeClass);
    if (cons==null)
    {
      Class[] proxyInterfaces=getInterfaces(adapteeClass);

      //for(int i=0;i<proxyInterfaces.length;i++)
      //  System.err.println("-> "+proxyInterfaces[i]);

      Class proxyClass=Proxy.getProxyClass(
        getClass().getClassLoader(), proxyInterfaces);

      try
      {
        constructor=proxyClass.getConstructor(
          INVOCATION_HANDLER_CLASS_ARRAY);

        // put in cache
        cache.put(adapteeClass,constructor);
      }
      catch(NoSuchMethodException e)
      {
        e.printStackTrace();
        throw new Error("internal error");
      }
    }
    else
      constructor=(Constructor)cons;

    try
    {
      adapter=constructor.newInstance(
        new Object[] { new Handler(adaptee,vhash) });
    }
    catch(Exception e)
    {
      e.printStackTrace();
      throw new Error("internal error");
    }

    // put in instances cache
    if (instances!=null)
      instances.put(adaptee,adapter);

    return adapter;
  }

  protected boolean isWrappable(Object object)
  {
    if (object==null ||
        (object instanceof Adapter &&
        ((Adapter)object).getFactory()==this))
      return false;

    // false by default
    return isWrappable(object.getClass());
  }

  /** return true if the class object is register as wrappable.
      @see #setWrappable(Class,boolean)
   */
  public boolean isWrappable(Class clazz)
  {
    Object o=wrapCache.get(clazz);
    if (o==null)
    {
      boolean b=_isWrappable(clazz)==1;
      wrapCache.put(clazz,(b)?Boolean.TRUE:Boolean.FALSE);
      return b;
    }
    else
      return (o==Boolean.TRUE);
  }

  private int _isWrappable(Class c)
  {
    if (c==null)
      return -1;

    Boolean bool=(Boolean)wrappables.get(c);
    if (bool!=null)
      return (bool==Boolean.TRUE)?1:0;

    Class[] interfaces=c.getInterfaces();
    for(int i=0;i<interfaces.length;i++)
    {
      int a=_isWrappable(interfaces[i]);
      if (a!=-1)
        return a;
    }
    return _isWrappable(c.getSuperclass());
  }

  protected Class[] getInterfaces(Class adapteeClass)
  {
    ArrayList list=new ArrayList();

    // add adaptee interfaces
    if (transparency)
      list.addAll(Arrays.asList(adapteeClass.getInterfaces()));

    for(Iterator it=interfaces.entrySet().iterator();it.hasNext();)
    {
      Map.Entry entry=(Map.Entry)it.next();
      Class clazz=(Class)entry.getKey();
      if (clazz.isAssignableFrom(adapteeClass))
      {
        // add all interfaces to the list
        list.addAll((Collection)entry.getValue());
      }
    }

    return (Class[])list.toArray(new Class[list.size()]);
  }

  /** called if no method matching.
      Default implementation throw a UnsupportedOperationException.
      @exception UnsupportedOperationException always thrown.
   */
  protected Object defaultTraversal(Method method,Object adaptee,
    Object adapter,Object[] args)
  {
    throw new UnsupportedOperationException("no method "+
      method.getName()+" for adaptee "+
      adaptee.getClass()+" with "+
      args.length+" parameter(s)");
  }

  Impl createImpl(int methodValue,final Map map,final Object defaultValue)
  {
    switch(methodValue)
    {
      case VALUE_GET_ADAPTEE:
        return new Impl() {
          Object invoke(Object adaptee,Object adapter,Object[] args)
            throws IllegalAccessException,IllegalArgumentException,
                   NoSuchMethodException,MultipleMethodsException,
                   InvocationTargetException
          { return adaptee; }
        };

      case VALUE_GET_FACTORY:
        return new Impl() {
          Object invoke(Object adaptee,Object adapter,Object[] args)
            throws IllegalAccessException,IllegalArgumentException,
                   NoSuchMethodException,MultipleMethodsException,
                   InvocationTargetException
          { return AdapterFactory.this; }
        };

      case VALUE_GET:
        return new Impl() {
          Object invoke(Object adaptee,Object adapter,Object[] args)
            throws IllegalAccessException,IllegalArgumentException,
                   NoSuchMethodException,MultipleMethodsException,
                   InvocationTargetException
          {
            Object value=map.get(adaptee);
            if (value==null)
              return defaultValue;
            else
              return value;
          }
        };

      case VALUE_SET:
        return new Impl() {
          Object invoke(Object adaptee,Object adapter,Object[] args)
            throws IllegalAccessException,IllegalArgumentException,
                   NoSuchMethodException,MultipleMethodsException,
                   InvocationTargetException
          {
            Object value=args[0];
            if (value!=defaultValue)
              return map.put(adaptee,value);
            else
              return value;
          }
        };
    }
    return null;
  }

  Impl createImpl(final MultiMethod mm,final Class[] skeletons,
    boolean isLocal,boolean isWrappable,int option,final Object target)
  {
    if (isLocal)
    {
      if (isWrappable)
      {
        if (skeletons==null)
        {
          return new Impl() {
            Object invoke(Object adaptee,Object adapter,Object[] args)
              throws IllegalAccessException,IllegalArgumentException,
                     NoSuchMethodException,MultipleMethodsException,
                     InvocationTargetException
            {
              Object result=mm.invoke(adaptee,args);
              if (isWrappable(result))
                return create(result);
              else
                return result;
            }
          };
        }
        else // if skeleton
        {
          return new Impl() {
            Object invoke(Object adaptee,Object adapter,Object[] args)
              throws IllegalAccessException,IllegalArgumentException,
                     NoSuchMethodException,MultipleMethodsException,
                     InvocationTargetException
            {
              Class[] types=null;
              if (args!=null)
              {
                int length=args.length;
                types=new Class[length];
                for(int i=length;--i>=0;)
                {
                  Object value=args[i];
                  if (value==null)
                    types[i]=null;
                  else
                  {
                    Class s=skeletons[i];
                    if (s==null)
                      types[i]=value.getClass();
                    else
                      types[i]=s;
                  }
                }
              }
              Object result=mm.invoke(adaptee,args,types);
              if (isWrappable(result))
                return create(result);
              else
                return result;
            }
          };
        }
      }
      else // is not wrappable
      {
        if (skeletons==null)
        {
          return new Impl() {
            Object invoke(Object adaptee,Object adapter,Object[] args)
              throws IllegalAccessException,IllegalArgumentException,
                     NoSuchMethodException,MultipleMethodsException,
                     InvocationTargetException
            {
              Object result=mm.invoke(adaptee,args);
              if (isWrappable(result))
                return create(result);
              else
                return result;
            }
          };
        }
        else // if skeleton
        {
          return new Impl() {
            Object invoke(Object adaptee,Object adapter,Object[] args)
              throws IllegalAccessException,IllegalArgumentException,
                     NoSuchMethodException,MultipleMethodsException,
                     InvocationTargetException
            {
              Class[] types=null;
              if (args!=null)
              {
                int length=args.length;
                types=new Class[length];
                for(int i=length;--i>=0;)
                {
                  Object value=args[i];
                  if (value==null)
                    types[i]=null;
                  else
                  {
                    Class s=skeletons[i];
                    if (s==null)
                      types[i]=value.getClass();
                    else
                      types[i]=s;
                  }
                }
              }

              Object result=mm.invoke(adaptee,args,types);
              if (isWrappable(result))
                return create(result);
              else
                return result;
            }
          };
        }
      }
    }
    else // if not local i.e if global
    {
      if (isWrappable)
      {
        if (skeletons==null)
        {
          switch(option)
          {
            case SEND_NONE:
              return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                {
                  Object result=mm.invoke(target,args);
                  if (isWrappable(result))
                    return create(result);
                  else
                    return result;
                }
              };
            case SEND_ADAPTEE:
              return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                {
                  Object[] arguments;
                  if (args!=null)
                  {
                    int length=args.length+1;
                    arguments=new Object[length];
                    for(int i=length;--i>0;)
                      arguments[i]=args[i-1];
                  }
                  else
                    arguments=new Object[1];

                  arguments[0]=adaptee;

                  Object result=mm.invoke(target,arguments);
                  if (isWrappable(result))
                    return create(result);
                  else
                    return result;
                }
              };
            case SEND_ADAPTER:
              return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                {
                  Object[] arguments;
                  if (args!=null)
                  {
                    int length=args.length+1;
                    arguments=new Object[length];
                    for(int i=length;--i>0;)
                      arguments[i]=args[i-1];
                  }
                  else
                    arguments=new Object[1];

                  arguments[0]=adapter;

                  Object result=mm.invoke(target,arguments);
                  if (isWrappable(result))
                    return create(result);
                  else
                    return result;
                }
              };
          }
        }
        else  // skeleton not null
        {
          switch(option)
          {
            case SEND_NONE:
              return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                {
                  Class[] types=null;
                  if (args!=null)
                  {
                    int length=args.length;
                    types=new Class[length];
                    for(int i=length;--i>=0;)
                    {
                      Object value=args[i];
                      if (value==null)
                        types[i]=null;
                      else
                      {
                        Class s=skeletons[i];
                        if (s==null)
                          types[i]=value.getClass();
                        else
                          types[i]=s;
                      }
                    }
                  }

                  Object result=mm.invoke(target,args,types);
                  if (isWrappable(result))
                    return create(result);
                  else
                    return result;
                }
              };

            case SEND_ADAPTEE:
             return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                {
                  Class[] types;
                  Object[] arguments;
                  if (args!=null)
                  {
                    int length=args.length+1;
                    types=new Class[length];
                    arguments=new Object[length];
                    for(int i=length;--i>0;)
                    {
                      Object value=args[i-1];
                      arguments[i]=value;
                      if (value==null)
                        types[i]=null;
                      else
                      {
                        Class s=skeletons[i-1];
                        if (s==null)
                          types[i]=value.getClass();
                        else
                          types[i]=s;
                      }
                    }
                  }
                  else
                  {
                    arguments=new Object[1];
                    types=new Class[1];
                  }

                  arguments[0]=adaptee;
                  types[0]=adaptee.getClass();

                  Object result=mm.invoke(target,arguments,types);
                  if (isWrappable(result))
                    return create(result);
                  else
                    return result;
                }
              };

            case SEND_ADAPTER:
              return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                {
                  Class[] types;
                  Object[] arguments;
                  if (args!=null)
                  {
                    int length=args.length+1;
                    types=new Class[length];
                    arguments=new Object[length];
                    for(int i=length;--i>0;)
                    {
                      Object value=args[i-1];
                      arguments[i]=value;
                      if (value==null)
                        types[i]=null;
                      else
                      {
                        Class s=skeletons[i-1];
                        if (s==null)
                          types[i]=value.getClass();
                        else
                          types[i]=s;
                      }
                    }
                  }
                  else
                  {
                    arguments=new Object[1];
                    types=new Class[1];
                  }

                  arguments[0]=adapter;
                  types[0]=adapter.getClass();

                  Object result=mm.invoke(target,arguments,types);
                  if (isWrappable(result))
                    return create(result);
                  else
                    return result;
                }
              };
          }  // end of switch
        }  // end of else skeleton
      }
      else // if not wrappable
      {
        if (skeletons==null)
        {
          switch(option)
          {
            case SEND_NONE:
              return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                { return mm.invoke(target,args); }
              };

            case SEND_ADAPTEE:
              return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                {
                  Object[] arguments;
                  if (args!=null)
                  {
                    int length=args.length+1;
                    arguments=new Object[length];
                    for(int i=length;--i>0;)
                      arguments[i]=args[i-1];
                  }
                  else
                    arguments=new Object[1];

                  arguments[0]=adaptee;
                  return mm.invoke(target,arguments);
                }
              };

            case SEND_ADAPTER:
              return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                {
                  Object[] arguments;
                  if (args!=null)
                  {
                    int length=args.length+1;
                    arguments=new Object[length];
                    for(int i=length;--i>0;)
                      arguments[i]=args[i-1];
                  }
                  else
                    arguments=new Object[1];

                  arguments[0]=adapter;
                  return mm.invoke(target,arguments);
                }
              };
          }
        }
        else  // skeleton not null
        {
          switch(option)
          {
            case SEND_NONE:
              return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                {
                  Class[] types=null;
                  if (args!=null)
                  {
                    int length=args.length;
                    types=new Class[length];
                    for(int i=length;--i>=0;)
                    {
                      Object value=args[i];
                      if (value==null)
                        types[i]=null;
                      else
                      {
                        Class s=skeletons[i];
                        if (s==null)
                          types[i]=value.getClass();
                        else
                          types[i]=s;
                      }
                    }
                  }
                  return mm.invoke(target,args,types);
                }
              };

            case SEND_ADAPTEE:
             return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                {
                  Class[] types;
                  Object[] arguments;
                  if (args!=null)
                  {
                    int length=args.length+1;
                    types=new Class[length];
                    arguments=new Object[length];
                    for(int i=length;--i>0;)
                    {
                      Object value=args[i-1];
                      arguments[i]=value;
                      if (value==null)
                        types[i]=null;
                      else
                      {
                        Class s=skeletons[i-1];
                        if (s==null)
                          types[i]=value.getClass();
                        else
                          types[i]=s;
                      }
                    }
                  }
                  else
                  {
                    arguments=new Object[1];
                    types=new Class[1];
                  }

                  arguments[0]=adaptee;
                  types[0]=adaptee.getClass();

                  return mm.invoke(target,arguments,types);
                }
              };

            case SEND_ADAPTER:
              return new Impl() {
                Object invoke(Object adaptee,Object adapter,Object[] args)
                  throws IllegalAccessException,IllegalArgumentException,
                         NoSuchMethodException,MultipleMethodsException,
                         InvocationTargetException
                {
                  Class[] types;
                  Object[] arguments;
                  if (args!=null)
                  {
                    int length=args.length+1;
                    types=new Class[length];
                    arguments=new Object[length];
                    for(int i=length;--i>0;)
                    {
                      Object value=args[i-1];
                      arguments[i]=value;
                      if (value==null)
                        types[i]=null;
                      else
                      {
                        Class s=skeletons[i-1];
                        if (s==null)
                          types[i]=value.getClass();
                        else
                          types[i]=s;
                      }
                    }
                  }
                  else
                  {
                    arguments=new Object[1];
                    types=new Class[1];
                  }

                  arguments[0]=adapter;
                  types[0]=adapter.getClass();

                  return mm.invoke(target,arguments,types);
                }
              };
          }  // end of switch
        }  // end of else skeleton
      } // end of else wrappable
    }  // end of else local

    return null;
  }

  static abstract class Impl
  {
    abstract Object invoke(Object adaptee,Object adapter,Object[] args)
      throws IllegalAccessException,IllegalArgumentException,
             NoSuchMethodException,MultipleMethodsException,
             InvocationTargetException;
  }

  static class MMEntry {
    MMEntry(Object target,MultiMethod mm,int invocationOption)
    {
      this.mm=mm;
      this.target=target;
      this.invocationOption=invocationOption;
    }

    Object target;
    MultiMethod mm;
    int invocationOption;
  }

  static class FieldEntry {
    FieldEntry(Object defaultValue,Map map) {
      this.map=map;
      this.defaultValue=defaultValue;
    }

    Map map;
    Object defaultValue;
  }



  // rewrap ??
  HashMap wrappables=new HashMap(17);

  HashMap wrapCache=new HashMap(17);

  boolean transparency=false;

  HashMap multimethods;
  HashMap fields;

  HashMap vhashes=new HashMap(17);

  // cache between adaptee and adapter
  WeakHashMap instances;

  // adapter cache, cache between adaptee class
  // and constructor of the proxy class
  HashMap cache=new HashMap(17);

  // MUST CHANGE IT TO A MAP THAT DOES USE EQUALS TO TEST BUT ==
  HashMap interfaces=new HashMap(17);

  // factory for local multi-methods
  MultiFactory multiFactory;

  private static final String NO_MULTIMETHOD="NO_MULTIMETHOD";

  public static final int SEND_NONE=0;
  public static final int SEND_ADAPTEE=1;
  public static final int SEND_ADAPTER=2;

  private static final int VALUE_NONE=0;
  private static final int VALUE_GET=1;
  private static final int VALUE_SET=2;
  private static final int VALUE_GET_ADAPTEE=3;
  private static final int VALUE_GET_FACTORY=4;

  static final Class HANDLER_CLASS=Adapter.class;
  static final Class ADAPTER_CLASS=Adapter.class;
  static final Class[] INVOCATION_HANDLER_CLASS_ARRAY=
    new Class[] {InvocationHandler.class};
}
