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
    @author Remi Forax
    @version 0.9.0
 */
final class MethodMap {
  /**
   */
  public MethodMap() {
    table=new Entry[8];
    array=new Entry[4];
  }

  /**
   */
  private void rehash() {

    // expand hashmap
    Entry oldMap[] = table;
    int oldCapacity = oldMap.length;

    int newCapacity = oldCapacity<<1;
    Entry newMap[] = new Entry[newCapacity];
    int mask=newCapacity-1;

    for (int i = oldCapacity ; i-- > 0 ;) {
      for (Entry old = oldMap[i] ; old != null ; ) {
         Entry e = old;
         old = old.next;

         int index_hash = e.hash % mask;
         e.next = newMap[index_hash];
         newMap[index_hash] = e;
      }
    }

    this.table = newMap;

    // expand array
    Entry[] newArray=new Entry[oldCapacity];

    System.arraycopy(array,0,newArray,0,this.array.length);
    this.array=newArray;
  }

  /** may only compute first and last hashCode.
   */
  private static int hashCode(Class[] types)
  {
    int hashValue=0;
    for(int i=types.length;--i>=0;)
      hashValue^=types[i].hashCode();

    return hashValue;
  }

  /** .
   */
  public Entry getEntry(Class[] types)
  {
    Entry[] tab=table;

    int hash=hashCode(types);
    int index_hash=hash & (tab.length-1);

loop:for(Entry e=tab[index_hash];e!=null;e=e.next)
      if (e.hash==hash) {

        Class[] eTypes=e.types;
        int i=types.length;
        if (eTypes.length!=i)
          break loop;

        for(;--i>=0;)
          if (types[i]!=eTypes[i])
            break loop;

        return e;
      }

    if (count >= array.length) {
      // Rehash the table if the threshold is exceeded
      rehash();

      tab=table;
    }

    int index=count++;

    // Creates the new entry.
    Entry entry=new Entry(types,hash,index,
      tab[index_hash]);

    tab[index_hash]=entry;
    array[index]=entry;

    return entry;
  }

  /** return parameter types for a specified slot.
   */
  public Entry getEntry(int slot) {
    return array[slot];
  }

  /** return number of mapping in the hash map.
   */
  public int size() {
    return count;
  }

  /**
   * HashMap collision list entry.
   */
  static final class Entry {
    Entry(Class[] types, int hash, int slot, Entry next) {
      this.slot=slot;
      this.types=types;
      this.hash=hash;
      this.next=next;
    }

    final Class[] types;
    final int slot;
    final int hash;
    Entry next;
  }

  /**
   * The hash table data.
   */
  private Entry[] table;

  /** array data
   */
  private Entry[] array;

  /**
   * The total number of mappings in the hash table.
   */
  private int count;
}
