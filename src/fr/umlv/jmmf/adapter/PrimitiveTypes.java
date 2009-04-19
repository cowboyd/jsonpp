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

/** @author Remi Forax
    @version 0.5.4
 */
final class PrimitiveTypes
{
  private PrimitiveTypes()
  { }

  private static int processPerfectHash(Class[] keys)
  {
    int length=keys.length;
    BitSet set=new BitSet(MAX_VALUE);
    BitSet zero=new BitSet(0);

    for(int j=length+1;j<MAX_VALUE;j++)
    {
      // clear set
      set.and(zero);

      int i=0;
      for(;i<length;i++)
      {
        int value=keys[i].hashCode()%j;
        if (set.get(value))
          break;
        set.set(value);
      }

      // no collision ??, create return the perfect hash value
      if (i==length)
        return j;

    }

    throw new Error("internal error, must use a bigger MAX_VALUE");
  }

  private static Entry[] processHashArray(int hash,Class[] keys,
    Class[] values)
  {
    Entry[] entries=new Entry[hash];

    for(int i=keys.length;--i>=0;)
    {
      Class key=keys[i];
      entries[key.hashCode()%hash]=new Entry(key,values[i]);
    }

    return entries;
  }

  /** return the primitive class for a given wrapper type.
      @param clazz a wrapper class.
      @return null if it's not a wrapper class or
       the corresponding primitive type.
   */
  public static Class getPrimitiveType(Class clazz)
  {
    Entry entry=entries[clazz.hashCode()%entries.length];
    if (entry!=null && entry.key==clazz)
      return entry.value;
    else
      return null;
  }

  final static class Entry
  {
    public Entry(Class key,Class value)
    {
      this.key=key;
      this.value=value;
    }

    Class key;
    Class value;
  }

  // process map table
  private static Entry[] entries;

  private static final Class clazz(String name)
  {
    try
    {
      return Class.forName("java.lang."+name);
    }
    catch(ClassNotFoundException e)
    {
      throw new Error("internal error "+e.getMessage());
    }
  }

  private final static int MAX_VALUE=101;

  private static Class[] wrappers={
    clazz("Boolean"),clazz("Character"),
    clazz("Byte"),clazz("Short"),clazz("Integer"),clazz("Long"),
    clazz("Float"),clazz("Double")};

  private static Class[] primitives={
    Boolean.TYPE,Character.TYPE,
    Byte.TYPE,Short.TYPE,Integer.TYPE,Long.TYPE,
    Float.TYPE,Double.TYPE};

  // MUST be the last block of static
  static
  {
    entries=processHashArray(processPerfectHash(wrappers),
      wrappers,primitives);

    // garbage static tabs
    wrappers=null;
    primitives=null;
  }

  /** test.
   */
  public static void main(String[] args)
  {
    System.out.println("Boolean");
    System.out.println(getPrimitiveType(clazz("Boolean")));
    System.out.println("Long");
    System.out.println(getPrimitiveType(clazz("Long")));
    System.out.println("Object");
    System.out.println(getPrimitiveType(clazz("Object")));
    System.out.println("int");
    System.out.println(getPrimitiveType(Integer.TYPE));
  }
}