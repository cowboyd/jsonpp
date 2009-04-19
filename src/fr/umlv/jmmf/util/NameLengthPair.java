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

package fr.umlv.jmmf.util;

/** Pair composed by the method name and number of parameters.
    The pair is a mutable object to avoid object allocation.
    The method hashCode and equals are redifined to allow using
    of NameLengthPair object as key in map.
    The pair is cloneable.

    @version 4.0pre7
    @author Remi Forax
 */
public final class NameLengthPair implements Cloneable
{
  /** Default constructor.
   */
  public NameLengthPair()
  { }

  public NameLengthPair(NameLengthPair pair)
  {
    name=pair.name;
    length=pair.length;
  }

  /** construct a pair with a name and a number of parameters.
   */
  public NameLengthPair(String name,int length)
  {
    this.name=name;
    this.length=length;
  }

  /** create a new NameLengthPair object.
      @deprecated use NameLengthPair(NameLengthPair)
   */
  public Object clone()
  {
    NameLengthPair pair=new NameLengthPair();
    pair.name=name;
    pair.length=length;
    return pair;
  }

  /** The pair is a mutable object to avoid
      object allocation.
   */
  public void init(String name,int length)
  {
    this.name=name;
    this.length=length;
  }

  public String toString()
  {
    StringBuffer buffer=new StringBuffer();
    buffer.append('<');
    buffer.append(name);
    buffer.append(',');
    buffer.append(length);
    buffer.append('>');
    return buffer.toString();
  }

  public int hashCode()
  { return name.hashCode()*(1+length); }

  public boolean equals(Object obj)
  {
    if (obj==null || !(obj instanceof NameLengthPair))
      return false;

    NameLengthPair pair=((NameLengthPair)obj);
    return pair.length==length && pair.name.equals(name);
  }

  public String getName()
  { return name; }

  public int getLength()
  { return length; }

  private String name;
  private int length;
}