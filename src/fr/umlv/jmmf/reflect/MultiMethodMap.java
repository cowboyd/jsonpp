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

/** this class contains annotations.
    @author Remi Forax
    @version 0.9
 */
final class MultiMethodMap {
  /**
   */
  public MultiMethodMap() {
    table=new Entry[4];
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
  public void put(Class clazz,MultiMethod mm)
  {
    if (count > (table.length>>1)) {
      // Rehash the table if the threshold is exceeded
      rehash();
    }

    Entry[] tab=table;
    int index=clazz.hashCode() & (tab.length-1);

    // Creates the new entry.
    tab[index]=new Entry(clazz, mm, tab[index]);
    count++;
  }

  /** .
   */
  public MultiMethod get(Class clazz,boolean onlyPublic) {
    Entry[] tab=table;

    int index=clazz.hashCode() & (tab.length-1);
    for(Entry e=tab[index];e!=null;e=e.next)
      if (e.clazz==clazz) {
        MultiMethod mm=e.mm;
        if (mm.isOnlyPublic()==onlyPublic)
          return mm;
      }

    return null;
  }

  /** return a special entry iterator on all entries.
   */
  public Iterator iterator() {
    return new Iterator();
  }

  /**
   * HashMap collision list entry.
   */
  static final class Entry {
    final Class clazz;
    final MultiMethod mm;
    Entry next;

    Entry(Class clazz, MultiMethod mm, Entry next) {
      this.clazz=clazz;
      this.mm=mm;
      this.next=next;
    }
  }

  final class Iterator {
    Entry[] table=MultiMethodMap.this.table;
    int index=table.length;
    Entry entry=null;

    public MultiMethod next() {
      Entry et=entry;
      int i=index;
      Entry tab[]=table;

      // Use locals for faster loop iteration
      while (et==null && i>0) {
        et=tab[--i];
      }

      entry=et;
      index=i;
      if (et!=null) {
        Entry e=entry;
        entry=e.next;
        return e.mm;
      }
      else
        return null;
    }
  }

  /**
   * The hash table data.
   */
  Entry[] table;

  /**
   * The total number of mappings in the hash table.
   */
  private int count;
}
