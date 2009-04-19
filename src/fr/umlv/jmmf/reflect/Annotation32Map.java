/*
 * Java MultiMethod Framework API 0.8
 *
 * $Id: Annotation32Map.java,v 1.1 2003/07/09 12:45:19 forax Exp $
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
    @version 0.9.0
 */
final class Annotation32Map {
  /**
   */
  public Annotation32Map() {
    table=new Entry[16];
  }

  /**
   */
  private void rehash() {
    Entry oldMap[] = table;
    int oldCapacity = oldMap.length;

    int newCapacity = oldCapacity<<1;
    Entry newMap[] = new Entry[newCapacity];
    int mask=newCapacity-1;

    for (int i = oldCapacity ; i-- > 0 ;) {
      for (Entry old = oldMap[i] ; old != null ; ) {
        Entry e = old;
        old = old.next;

        int index = e.clazz.hashCode() & mask;
        //int index=(e.clazz.hashCode() & 0x7FFFFFFF) % newCapacity;
        
        e.next = newMap[index];
        
        //if (newMap[index]!=null)
        //  System.err.println("collision ");
        
        newMap[index] = e;
      }
    }
    
    this.table = newMap;
  }

  /** the class mustn't be already inserted.
   */
  public void put(Class clazz,int bits)
  {
    if (count > (table.length>>1) ) {
      // Rehash the table if the threshold is exceeded
      rehash();
    }

    Entry[] tab=table;
    int index=clazz.hashCode() & (tab.length-1);
    //int index=(clazz.hashCode() & 0x7FFFFFFF) % tab.length;

    //if (tab[index]!=null)
    //  System.err.println("collision "+clazz);

    // Creates the new entry.
    tab[index]=new Entry(clazz, bits, tab[index]);
    count++;
  }

  /** .
   */
  public int get(Class clazz) {
    
    /*
    int index=clazz.hashCode() & mask;
    Entry e = table[index]; 
    while (true) {
      if (e==null)
        return -1;
      if (e.clazz == clazz)
        return e.bits;
      e = e.next;
    }*/
    
    /*
    Entry[] tab=table;
    //int index=(clazz.hashCode() & 0x7FFFFFFF) % tab.length;
    int index=clazz.hashCode() & (tab.length-1);
    Entry e = tab[index]; 
    while (true) {
      if (e==null)
        return -1;
      if (e.clazz == clazz)
        return e.bits;
      
      e = e.next;
    }*/
    
    Entry[] tab=table;
    //int index=(clazz.hashCode() & 0x7FFFFFFF) % tab.length;
    int index=clazz.hashCode() & (tab.length-1);
    for(Entry e=tab[index];e!=null;e=e.next)
      if (e.clazz==clazz)
        return e.bits;

    return -1;
  }


  /*public int get(Class clazz) {
    Entry[] tab=table;

    try
    {
      Entry e=tab[clazz.hashCode() & mask];
      for(;;e=e.next);
        if (e.clazz==clazz)
          return e.bits;

    } catch(NullPointerException e) {
      //e.printStackTrace();
      return -1;
    }
  }*/

  /** @return -1 if clazz has no annotation.
   */
  public int or(Class clazz,int bits) {
    Entry[] tab=table;

    int index=clazz.hashCode() & (tab.length-1);
    for(Entry e=tab[index];e!=null;e=e.next)
      if (e.clazz==clazz)
        return e.bits|=bits;
        
    return -1;

    /*
    Entry[] tab=table;
    
    int index=clazz.hashCode() & (tab.length-1);
    //int index=(clazz.hashCode() & 0x7FFFFFFF) % tab.length;
    
    Entry e = tab[index]; 
    while (true) {
      if (e==null)
        return -1;
      if (e.clazz == clazz)
        return e.bits|=bits;
      e = e.next;
    }*/
  }

  /** remove entry with value 0.
   */
  public void compact() {
    Entry[] tab=table;

    for (int i=tab.length;--i>=0;) {
      for (Entry e=tab[i],prev=null; e!=null;) {
        if (e.bits==0) {
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

    System.err.println("table length "+tab.length);

    for (int i=tab.length;--i>=0;) {
      for (Entry e=tab[i]; e!=null; e=e.next) {
        System.err.println("index "+i+" "+e+" "+e.clazz+" "+new Bit32Mask(e.bits));
      }
    }
  }

  /**
   * HashMap collision list entry.
   */
  static final class Entry {
    final Class clazz;
    int bits;
    Entry next;

    public Entry(Class clazz, int bits, Entry next) {
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
