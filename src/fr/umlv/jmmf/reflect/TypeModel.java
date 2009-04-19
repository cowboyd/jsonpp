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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.IdentityHashMap;

/** default java subtyping rules.
   
    @author Remi Forax
    @version 0.9.0
 */
final class TypeModel
{
  /** return all super types of a given class.
   */
  public static Class[] getSuperTypes(Class c)
  {
    // case of primitive types
    if (c.isPrimitive())
      return (Class[])primitives.get(c);

    // case of array types
    if (c.isArray())
    {
      Class type=c.getComponentType();
      if (type==OBJECT_CLASS || type.isPrimitive())
        return ARRAY_OBJECT_SUPERTYPES;
      else
      {
        // reuse the class array
        Class[] supertypes=getSuperTypes(type);
        int length=supertypes.length;
        
        // use arraycopy
        for(int i=0;i<length;i++)
          supertypes[i]=Array.newInstance(supertypes[i],0).getClass();
          
        return supertypes;
      }
    }

    Class[] interfaces=c.getInterfaces();
    int length=interfaces.length;

    // case of interfaces
    if (c.isInterface())
      if (length==0)
        return OBJECT_SUPERTYPES;
      else
        return interfaces;

    // case of objects
    Class superclass=c.getSuperclass();
    if (superclass==null)
      return interfaces;
    else
    {
      Class[] classes=new Class[length+1];
      System.arraycopy(interfaces,0,classes,1,length);
      classes[0]=superclass;
      return classes;
    }
  }

  // avoid lazy allocation !!
  private static final Class OBJECT_CLASS=Object.class;

  private static final Class[] EMPTY_ARRAY_CLASS=new Class[0];
  private static final Class[] OBJECT_SUPERTYPES=new Class[]{OBJECT_CLASS};
  private static final Class[] ARRAY_OBJECT_SUPERTYPES=new Class[]{
    OBJECT_CLASS,
    Cloneable.class,
    Serializable.class
  };

  private static final IdentityHashMap primitives=new IdentityHashMap();

  static {
    Class[] shortArray=new Class[]{Short.TYPE};
    
    Object[] array={
      Boolean.TYPE,   EMPTY_ARRAY_CLASS,
      Character.TYPE, shortArray,
      Byte.TYPE,      shortArray,
      Short.TYPE,     new Class[]{Integer.TYPE},
      Integer.TYPE,   new Class[]{Long.TYPE},
      Long.TYPE,      new Class[]{Float.TYPE},
      Float.TYPE,     new Class[]{Double.TYPE},
      Double.TYPE,    EMPTY_ARRAY_CLASS
    };
    
    for(int i=0;i<array.length;i+=2) {
      primitives.put(array[i],array[i+1]);
    }
  }

  
}
