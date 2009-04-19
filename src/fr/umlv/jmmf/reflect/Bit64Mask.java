/*
 * Java MultiMethod Framework API 0.8
 *
 * $Id: Bit64Mask.java,v 1.1 2003/07/09 12:45:19 forax Exp $
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

     @author Remi Forax
     @version 0.9
 */
final class Bit64Mask extends BitMask {
  public Bit64Mask() {
  }
  public Bit64Mask(int value) {
    this.value=((long)(value&0x7FFFFFFF))|
      ((value<0)?0x80000000L:0L);
  }
  public Bit64Mask(long value) {
    this.value=value;
  }

  public int type() {
    return BIT64_MASK;
  }
  public int length() {
    int i=0;
    for(long v=value; v!=0; v>>>=1L, i++);
    return i;
  }
  public int intValue() {
    throw new UnsupportedOperationException("bad type");
  }
  public long longValue() {
    return value;
  }
  public BitSetMask bitSetValue() {
    return new BitSetMask(value);
  }

  public boolean get(int index) {
    if (index>63)
      return false;
    else
      return (value & 1L<<index)!=0;
  }

  public BitMask set(int index) {
    if (index>63)
      return new BitSetMask(value).set(index);

    value|=1L<<index;
    return this;
  }

  public BitMask or(BitMask mask) {
    switch(mask.type()) {
      case BIT32_MASK:
      case BIT64_MASK:
        value|=mask.longValue();
        return this;
      default:
        return new BitSetMask(mask.bitSetValue()).or(this);
    }
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer(64);
    String separator = "";
    buffer.append('{');

    int i=0;
    for(long v=value; v!=0; v>>>=1L, i++) {
      if ((v & 1L)!=0) {
        buffer.append(separator);
        separator = ", ";
        buffer.append(i);
      }
    }

    buffer.append('}');
    return buffer.toString();
  }

  long value;
}
