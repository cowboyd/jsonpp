/*
 * Java MultiMethod Framework API 0.8
 *
 * $Id: Bit32Mask.java,v 1.1 2003/07/09 12:45:19 forax Exp $
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
final class Bit32Mask extends BitMask {
  public Bit32Mask() {
  }

  public Bit32Mask(int value) {
    this.value=value;
  }

  public int type() {
    return BIT32_MASK;
  }
  public int length() {
    int i=0;
    for(int v=value; v!=0; v>>>=1, i++);
    return i;
  }
  public int intValue() {
    return value;
  }
  public long longValue() {
    return value;
  }
  public BitSetMask bitSetValue() {
    return new BitSetMask(value);
  }

  public boolean get(int index) {
    if (index>31)
      return false;
    else
      return (value & 1<<index)!=0;
  }

  public BitMask set(int index) {
    if (index>31)
      if (index>63)
        return new BitSetMask(value).set(index);
      else
        return new Bit64Mask(value).set(index);

    value|=1<<index;
    return this;
  }

  public BitMask or(BitMask mask) {
    switch(mask.type()) {
      case BIT32_MASK:
        value|=mask.intValue();
        return this;
      case BIT64_MASK:
        return new Bit64Mask(value|mask.longValue());
      default:
        return new BitSetMask(mask.bitSetValue()).or(this);
    }
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer(32);
    String separator = "";
    buffer.append('{');

    int i=0;
    for(int v=value; v!=0; v>>>=1, i++) {
      if ((v & 1)!=0) {
        buffer.append(separator);
        separator = ", ";
        buffer.append(i);
      }
    }

    buffer.append('}');
    return buffer.toString();
  }

  int value;
}
