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

/**
    not an interface for performance reasons !!!!

    @author Remi Forax
    @version 0.9.0
 */
abstract class Impl {

  /** return the name of the multi-method.
   */
  public abstract String getName();

  /** return the number of argument.
   */
  public abstract int getArgLength();


  /** return the method map.
   */
  public abstract MethodMap getMethodMap();

  /** return the type support.
    */
  public abstract TypeSupport getTypeSupport();

  /**
   */
  public abstract int getMethodIndex(Class[] args,BitMask mask)
    throws NoSuchMethodException, MultipleMethodsException;

  /**
   */
  public abstract int getMethodIndex(Object[] args,BitMask mask)
    throws NoSuchMethodException, MultipleMethodsException;

  /**
   */
  public abstract int getMapIndex(Class[] types);

  /**
   */
  public abstract void addMethod(Class[] types,int methodIndex);

  /** call after addMethod() when all methods of a class are added.
     */
  public abstract void endsConstruction();


  /** return the multi-method of a given class.
   */
  public abstract MultiMethod getMultiMethod(Class clazz,boolean onlyPublic);


  /** this class represent the class of a null value.
   */
  protected static Class NULL_CLASS=Void.TYPE;
}
