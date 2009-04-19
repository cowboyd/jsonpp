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

import fr.umlv.jmmf.util.NameLengthPair;

/** Factory class for multi-method implementation.

    @author Remi Forax
    @version 0.9
 */
public class MultiFactory
{
  /** Construct a multi-method factory with a type model.
      All multi-method created with this factory have the
      same type model and the same hook.

      @see #getDefaultFactory()
   */
  public MultiFactory() {
    this.support=new TypeSupport();
  }

  /** construct a multi-method by taking all method named name
      with argLength parameter(s) in class clazz.

      @param clazz class which owns the named methods.
      @param name name of the methods.
      @param length number of parameters.

      @exception IllegalArgumentException if the class clazz contains
       no method named name or all methods found don't have the same
       number of parameters.
   */
  public MultiMethod create(Class clazz,String name,int length) {
    return create(clazz,name,length,false);
  }

  /** construct a multi-method by taking all method named name
      with argLength parameter(s) in class clazz.

      @param clazz class which owns the named methods.
      @param name name of the methods.
      @param length number of parameters.
      @param onlyPublic true if only the public methods must be used.

      @exception IllegalArgumentException if the class clazz contains
       no method named name or all methods found don't have the same
       number of parameters.
   */
  public MultiMethod create(Class clazz,String name,int length,
    boolean onlyPublic)
  {
    NameLengthPair pair=new NameLengthPair(name,length);

    Impl impl;

    // don't share implementation to permit to bench !!!
    /*
    Object o=map.get(pair);
    if (o==null) {
      impl=new Bit32Impl(name,length,support);
      map.put(pair,impl);
    }
    else {
      impl=(Impl)o;
    }*/
    
    impl=new Bit32Impl(name,length,support);

    return impl.getMultiMethod(clazz,onlyPublic);
  }

  private TypeSupport support;
  private HashMap map=new HashMap();

  /** return the default factory instance.
   */
  public static MultiFactory getDefaultFactory() {
    return defaultFactory;
  }

  private final static MultiFactory defaultFactory=
    new MultiFactory();
}
