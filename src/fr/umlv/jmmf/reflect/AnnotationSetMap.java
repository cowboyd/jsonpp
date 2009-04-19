/*
 * Java MultiMethod Framework API 0.8
 *
 * $Id: AnnotationSetMap.java,v 1.1 2003/07/09 12:45:19 forax Exp $
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
final class AnnotationSetMap {

  /** for performance only.
   */
  public AnnotationSetMap() {
    this.table=new Entry[4];
  }

  /**
   */
  public AnnotationSetMap(Annotation64Map map) {

    count=map.count;

    // copy table
    Annotation64Map.Entry[] tab=map.table;
    int size=tab.length;
    Entry[] table=new Entry[size];

    for(int i=size;--i>=0;) {
      Annotation64Map.Entry entry64=tab[i];

      if (entry64==null)
        continue;

      Entry lastEntry=table[i]=
        new Entry(entry64.clazz,new BitSetMask(entry64.bits),null);

      for(entry64=entry64.next;entry64!=null;entry64=entry64.next) {
        Entry e=new Entry(entry64.clazz,new BitSetMask(entry64.bits),null);
        lastEntry.next=e;
        lastEntry=e;
      }
    }
    this.table=table;
  }

  /**
   */
  private void rehash() {
    Entry oldMap[] = table;
    int oldCapacity = oldMap.length;

    int newCapacity = oldCapacity<<1;
    Entry newMap[] = new Entry[newCapacity];
    int mask = newCapacity -1;
    

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
  public void put(Class clazz,BitSetMask bits)
  {
    if (count > (table.length>>1)) {
      // Rehash the table if the threshold is exceeded
      rehash();
    }

    Entry[] tab=table;
    int index=clazz.hashCode() & (tab.length-1);

    // Creates the new entry.
    tab[index]=new Entry(clazz, bits, tab[index]);
    count++;
  }

  /** .
   */
  public BitSetMask get(Class clazz) {
    Entry[] tab=table;

    int index=clazz.hashCode() & (tab.length-1);
    for(Entry e=tab[index];e!=null;e=e.next)
      if (e.clazz==clazz)
        return e.bits;

    return null;
  }

  /*
  public int get(Class clazz) {
    Entry[] tab=table;

    int index=clazz.hashCode() & mask;
    Entry e=tab[index];
    for(;;) {
      try {
        if (e.clazz==clazz)
          return e.bits;
        e=e.next;
      } catch(NullPointerException ex) {
        ex.printStackTrace();
        return -1;
      }
    }
  }*/

  /** @return null if clazz has no annotation.
   */
  /*public BitSetMask or(Class clazz,BitSetMask bits) {
    Entry[] tab=table;

    for(Entry e=tab[clazz.hashCode() & mask];e!=null;e=e.next)
      if (e.clazz==clazz)
        return e.bits.or(bits);

    return null;
  }*/

  /** @return null if clazz has no annotation.
   */
  public BitSetMask set(Class clazz,int bitIndex) {
    Entry[] tab=table;

    int index=clazz.hashCode() & (tab.length-1);
    for(Entry e=tab[index];e!=null;e=e.next)
      if (e.clazz==clazz) {
        BitSetMask mask=e.bits;
        mask.set(bitIndex);
        return mask;
      }

    return null;
  }

  /** remove entry with value 0.
   */
  public void compact() {
    Entry[] tab=table;

    for (int i=tab.length;--i>=0;) {
      for (Entry e=tab[i],prev=null; e!=null;) {
        BitSetMask bits=e.bits;
        if (bits==null || bits.length()==0) {
          if (prev!=null)
            e=prev.next=e.next;
          else
            e=tab[i]=e.next;

          count--;
        }
        else {
          prev=e;
          e=e.next;
        }
      }
    }
  }

  /** DEBUG.
   */
  public void debug() {
    Entry[] tab=table;

    for (int i=tab.length;--i>=0;) {
      for (Entry e=tab[i]; e!=null; e=e.next) {
        System.out.println(e+" "+e.clazz+" "+e.bits);
      }
    }
  }

  /**
   * HashMap collision list entry.
   */
  static final class Entry {
    final Class clazz;
    BitSetMask bits;
    Entry next;

    Entry(Class clazz, BitSetMask bits, Entry next) {
      this.clazz=clazz;
      this.bits=bits;
      this.next=next;
    }
  }

  /**
   * The hash table data.
   */
  Entry[] table;

  /**
   * The total number of mappings in the hash table.
   */
  int count;
}
