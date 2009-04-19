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
    @version 0.9
 */
final class TypeSupport
{
  /** return the class entry of a class.
      if the class entry doen't exist, it is create first.
   */
  public ClassEntry getClassEntry(Class clazz) {
    Object o=classMap.get(clazz);
    if (o==null) {
      BitMask mask=new Bit32Mask();

      Class[] types=TypeModel.getSuperTypes(clazz);
      for(int i=types.length;--i>=0;) {
        ClassEntry parent=getClassEntry(types[i]);

        mask=mask.or(parent.mask);
      }

      int value=classMap.size();
      mask.set(value);

      return classMap.put(clazz,value,mask);
    }
    else
      return (ClassEntry)o;
  }

  /** represent a class and an entry in the hash map.
   */
  static final class ClassEntry {
    public ClassEntry(Class clazz,int classValue,
      BitMask mask,ClassEntry next)
    {
      this.clazz=clazz;
      this.classValue=classValue;
      this.mask=mask;
      this.next=next;
    }

    /** DEBUG
     */
    public String toString() {
      return "value "+classValue+" mask "+mask;
    }

    public boolean isAssignableFrom(ClassEntry entry) {
      return entry.mask.get(classValue);
    }

    Class clazz;
    BitMask mask;
    int classValue;
    ClassEntry next;
  }

  /**
   */
  static final class ClassEntryMap {
    /**
     */
    public ClassEntryMap() {
      // init table
      table=new ClassEntry[11];
      threshold = (int)(11 * 0.75);
    }

    /**
     */
    public int size() {
      return count;
    }

    /**
     */
    private void rehash() {
      ClassEntry oldMap[] = table;
      int oldCapacity = oldMap.length;

      int newCapacity = oldCapacity * 2 + 1;
      ClassEntry newMap[] = new ClassEntry[newCapacity];

      threshold = (int)(newCapacity * 0.75);
      table = newMap;

      for (int i = oldCapacity ; i-- > 0 ;) {
        for (ClassEntry old = oldMap[i] ; old != null ; ) {
          ClassEntry e = old;
          old = old.next;

          int index = (e.clazz.hashCode() & 0x7FFFFFFF) % newCapacity;
          e.next = newMap[index];
          newMap[index] = e;
        }
      }
    }

    /** the class mustn't be already inserted.
     */
    public ClassEntry put(Class clazz,int value,BitMask mask)
    {
      if (count >= threshold) {
        // Rehash the table if the threshold is exceeded
        rehash();
      }

      ClassEntry[] tab=table;
      int index=(clazz.hashCode()& 0x7FFFFFFF)%tab.length;

      // Creates the new entry.
      count++;
      return tab[index]=new ClassEntry(clazz,value,mask,tab[index]);
    }

    /** .
     */
    public ClassEntry get(Class clazz) {
      ClassEntry[] tab=table;

      int index=(clazz.hashCode() & 0x7FFFFFFF)%tab.length;
      for(ClassEntry e=tab[index];e!=null;e=e.next)
        if (e.clazz==clazz)
          return e;

      return null;
    }

    /**
     * The hash table data.
     */
    private ClassEntry[] table;

    /**
     * The total number of mappings in the hash table.
     */
    private int count;

    /**
     * The table is rehashed when its size exceeds this threshold.  (The
     * value of this field is (int)(capacity * loadFactor).)
     *
     */
    private int threshold;
  }

  /** contains associations between class and classEntry.
   */
  private final ClassEntryMap classMap=new ClassEntryMap();
}
