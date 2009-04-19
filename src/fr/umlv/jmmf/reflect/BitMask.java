/*
 * Java MultiMethod Framework API 0.8
 *
 * $Id: BitMask.java,v 1.1 2003/07/09 12:45:19 forax Exp $
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
public abstract class BitMask {

  abstract public int type();

  abstract public int length();

  abstract public int intValue();
  abstract public long longValue();
  abstract public BitSetMask bitSetValue();

  abstract public boolean get(int index);
  abstract public BitMask set(int index);
  abstract public BitMask or(BitMask mask);

  public static final int BIT32_MASK=0;
  public static final int BIT64_MASK=1;
  public static final int BITSET_MASK=2;
}
