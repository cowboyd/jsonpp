/*
 * Java MultiMethod Framework API 0.8
 *
 * $Id: DispatchMap.java,v 1.2 2003/07/09 12:45:19 forax Exp $
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

import java.lang.reflect.Method;

/**
    @author Remi Forax
    @version 0.9.0
 */
final class DispatchMap {
  /**
   */
  public DispatchMap() {
    table=new Entry[8];
  }

  /**
   */
  private void rehash() {
    int oldCapacity = table.length;
    Entry oldMap[] = table;

    int newCapacity = oldCapacity<<1;
    Entry newMap[] = new Entry[newCapacity];
    int mask=newCapacity-1;

    for (int i = oldCapacity ; i-- > 0 ;) {
      for (Entry old = oldMap[i] ; old != null ; ) {
        Entry e = old;
        old = old.next;

        int index = e.clazz.hashCode() & mask;
        e.next = newMap[index];
        newMap[index] = e;
      }
    }
    
    this.table = newMap;
  }

  /** the class mustn't be already inserted.
   */
  public Entry put(Class clazz,BitMask visibility,
    BitMask staticVisibility,Method[] dispatchTable)
  {
    if (count > (table.length>>1)) {
      // Rehash the table if the threshold is exceeded
      rehash();
    }

    Entry[] tab=table;
    int index=clazz.hashCode()& (tab.length-1);

    // Creates the new entry.
    count++;
    return tab[index]=new Entry(clazz,visibility,
      staticVisibility,dispatchTable,tab[index]);
  }

  /** .
   */
  public Entry get(Class clazz) {
    Entry[] tab=table;

    int index=clazz.hashCode() & (tab.length-1);
    for(Entry e=tab[index];e!=null;e=e.next)
      if (e.clazz==clazz)
        return e;

    return null;
  }
  
  final class Entry {
    final Class clazz;

    final BitMask visibility;
    final BitMask staticVisibility;
    final Method[] dispatchTable;

    Entry next;

    public Entry(Class clazz,
      BitMask visibility,BitMask staticVisibility,
      Method[] dispatchTable,
      Entry next)
    {
      this.clazz=clazz;
      this.visibility=visibility;
      this.staticVisibility=staticVisibility;
      this.dispatchTable=dispatchTable;
      this.next=next;
    }
  }

  /**
   * The hash table data.
   */
  private Entry[] table;

  /**
   * The total number of mappings in the hash table.
   */
  private int count;
}
