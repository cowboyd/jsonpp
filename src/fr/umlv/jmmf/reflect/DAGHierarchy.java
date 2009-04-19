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


/** this class represents subtyping relations between classes.

    @author Remi Forax
    @version 0.9
 */
final class DAGHierarchy {
  /**
   */
  public DAGHierarchy() {
    this.table=new Entry[8];
  }

  /**
   */
  private void rehash() {
    int oldCapacity = table.length;
    Entry oldMap[] = table;

    int newCapacity = oldCapacity<<1;
    Entry newMap[] = new Entry[newCapacity];
    int mask=newCapacity-1;

    for (int i=oldCapacity; i-->0;) {
      for (Entry old=oldMap[i];old!=null;) {
        Entry e=old;
        old=old.next;

        int index=e.clazz.hashCode() & mask;
        e.next=newMap[index];
        newMap[index]=e;
      }
    }
    
    this.table = newMap;
  }

  /** create an entry corresponding to the class.
   */
  public Entry add(Class clazz) {
    if (count > (table.length>>1)) {
      // Rehash the table if the threshold is exceeded
      rehash();
    }

    Entry[] tab=table;
    int index=clazz.hashCode() & (tab.length-1);

    // Creates the new entry.
    count++;
    return tab[index]=new Entry(clazz, tab[index]);
  }

  /** return the entry corresponding to the class.
      the entry is created if necessary.
   */
  public Entry get(Class clazz) {
    Entry[] tab=table;

    int index=clazz.hashCode() & (tab.length-1);
    for(Entry e=tab[index];e!=null;e=e.next)
      if (e.clazz==clazz)
        return e;

    return null;
  }

  /**
   * HashMap collision list entry.
   */
  static final class Entry {
    final Class clazz;
    Entry[] subtypes;
    Entry next;

    Entry(Class clazz, Entry next) {
      this.clazz=clazz;
      this.next=next;
    }

    public void addSubtypeIfNotExists(Entry subtype) {
      Entry[] subtypes=this.subtypes;
      if (subtypes==null) {
        this.subtypes=new Entry[]{subtype};
        return;
      }
      
      // look if it not exists
      int size=subtypes.length;
      for(int i=size;--i>=0;)
        if (subtypes[i].clazz==subtype.clazz)
          return;
          
      Entry[] newTypes=new Entry[size+1];
      System.arraycopy(subtypes,0,newTypes,0,size);
      newTypes[size]=subtype;

      this.subtypes=newTypes;
    }

    public void addSubtype(Entry subtype) {
      Entry[] subtypes=this.subtypes;
      if (subtypes==null)
        this.subtypes=new Entry[]{subtype};
      else {
        int size=subtypes.length;
        Entry[] newTypes=new Entry[size+1];
        System.arraycopy(subtypes,0,newTypes,0,size);
        newTypes[size]=subtype;

        this.subtypes=newTypes;
      }
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
