/*
 * Java MultiMethod Framework API 0.8
 *
 * $Id: BitSetMask.java,v 1.1 2003/07/09 12:45:19 forax Exp $
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
class BitSetMask extends BitMask {

  private BitSetMask(int[] unit,int unitsInUse) {
    this.unit=unit;
    this.unitsInUse=unitsInUse;
  }

  public BitSetMask() {
    unit=new int[2];
  }

  public BitSetMask(int value) {
    unit=new int[] {value,0};
    unitsInUse=(value==0)?0:1;
  }

  public BitSetMask(long value) {

    int lowValue=(int)(value & 0x7FFFFFFFL)|
      (((value & 0x80000000L)!=0)?0x80000000:0);

    long hightLong=value>>>BITS_PER_UNIT;
    int hightValue=(int)(hightLong & 0x7FFFFFFFL)|
      (((hightLong & 0x80000000L)!=0)?0x80000000:0);

    unit=new int[]{ lowValue, hightValue};
    unitsInUse=(hightValue==0)?((lowValue==0)?0:1):2;
  }

  public BitSetMask(BitSetMask mask) {
    int unitsInUse=this.unitsInUse=mask.unitsInUse;
    int[] unit=new int[unitsInUse];
    System.arraycopy(mask.unit,0,unit,0,unitsInUse);
    this.unit=unit;
  }

  public int type() {
    return BITSET_MASK;
  }

  /** Ensures that the BitSet can hold enough units.
      @param unitsRequired the minimum acceptable number of units.
   */
  private void ensureCapacity(int unitsRequired) {
    int unitLength=unit.length;
    if (unitLength < unitsRequired) {
      int request = Math.max(unitLength+1, unitsRequired);
      int newUnit[] = new int[request];
      System.arraycopy(unit, 0, newUnit, 0, unitsInUse);
      unit = newUnit;
    }
  }

  /** Set the field unitsInUse with the logical size in units of the bit
      set.  WARNING:This function assumes that the number of units actually
      in use is less than or equal to the current value of unitsInUse!
   */
  private void recalculateUnitsInUse() {
    /* Traverse the bitset until a used unit is found */
    int i;
    for (i=unitsInUse-1;i>=0;i--)
      if (unit[i]!=0)
        break; //this unit is in use!

    unitsInUse=i+1; //the new logical size
  }


  public int length() {
    int unitsInUse=this.unitsInUse;
    if (unitsInUse == 0)
      return 0;

    int highestBit=(unitsInUse - 1)*BITS_PER_UNIT;
    int highestUnit = unit[unitsInUse - 1];
    do {
      highestUnit=highestUnit>>>1;
      highestBit++;
    } while(highestUnit>0);

    return highestBit;
  }

  /** true if only one bit is set.
   */
  public boolean onlyOneBitSet() {
    int highestUnitInUse=this.unitsInUse-1;
    int highestUnit=unit[highestUnitInUse];
    if ((highestUnit & (highestUnit-1))!=0)
      return false;

    for (int i=highestUnitInUse;--i>=0;)
      if (unit[i]!=0)
        return false;
    return true;
  }

  public boolean get(int index) {
    int unitIndex = unitIndex(index);
    if (unitIndex < unitsInUse)
      return ((unit[unitIndex] & bit(index)) != 0);
    else
      return false;
  }

  /** this implementation always return this.
   */
  public BitMask set(int index) {
    int unitIndex=unitIndex(index);
    int unitsRequired=unitIndex+1;

    if (unitsInUse < unitsRequired) {
      ensureCapacity(unitsRequired);
      unitsInUse = unitsRequired;
    }

    unit[unitIndex]|=bit(index);
    return this;
  }

  /** this implementation always return this.
   */
  public BitSetMask clear(int index) {
    int unitIndex=unitIndex(index);
    if (unitIndex>=unitsInUse)
        return this;

    unit[unitIndex]&=~bit(index);
    if (unit[unitsInUse-1]==0)
      recalculateUnitsInUse();

    return this;
  }

  public void and(BitSetMask set) {
    // perform logical AND on bits in common
    int unitsInUse=this.unitsInUse;
    int unitsInCommon=Math.min(unitsInUse, set.unitsInUse);

    int i;
    for(i=0;i<unitsInCommon;i++)
      unit[i]&=set.unit[i];

    // clear out units no longer used
    for(;i<unitsInUse;i++)
      unit[i]=0;

    // Recalculate units in use if necessary
    this.unitsInUse=unitsInCommon;
    if (unitsInCommon>0 && unit[unitsInCommon-1]==0)
      recalculateUnitsInUse();
  }

  public BitMask or(BitMask mask) {

    switch(mask.type()) {
      case BIT32_MASK:
        int value=mask.intValue();
        unit[0]|=value;
        if (unitsInUse==0 && value!=0)
          unitsInUse=1;
        return this;
      case BIT64_MASK:
        long l=mask.longValue();
        unit[0]=(int)(l & 0x7FFFFFFFL)|
          (((l & 0x80000000L)!=0)?0x80000000:0);
        long hightLong=l>>>BITS_PER_UNIT;
        unit[1]=(int)(hightLong & 0x7FFFFFFFL)|
          (((hightLong & 0x80000000L)!=0)?0x80000000:0);

        if (unitsInUse<2)
          unitsInUse=(unit[1]==0)?((unit[0]==0)?0:1):2;
        return this;
      default:
        return or(mask.bitSetValue());
    }
  }

  /** NOTE: this implementation always return this.
   */
  public BitSetMask or(BitSetMask set) {

    // ensure capacity
    int setUnitsInUse=set.unitsInUse;
    ensureCapacity(setUnitsInUse);

    // use local copy
    int[] unit=this.unit;
    int[] setUnits=set.unit;

    // perform logical OR on bits in common
    int unitsInUse=this.unitsInUse;
    int unitsInCommon=Math.min(unitsInUse,setUnitsInUse);

    int i;
    for(i=0; i<unitsInCommon; i++)
      unit[i]|=setUnits[i];

    // copy any remaining bits
    for(; i<setUnitsInUse; i++)
      unit[i]=setUnits[i];

    if (unitsInUse<setUnitsInUse)
      this.unitsInUse=setUnitsInUse;

    return this;
  }

  public String toString() {
    int numBits = unitsInUse << ADDRESS_BITS_PER_UNIT;
    StringBuffer buffer = new StringBuffer(8*numBits + 2);

    // DEBUG
    //buffer.append('(');
    //buffer.append(unitsInUse);
    //buffer.append(')');

    String separator = "";
    buffer.append('{');

    for(int i=0; i<numBits; i++) {
      if (get(i)) {
        buffer.append(separator);
        separator = ", ";
        buffer.append(i);
      }
    }

    buffer.append('}');
    return buffer.toString();
  }

  public int intValue() {
    throw new UnsupportedOperationException("unsupport operation");
  }
  public long longValue() {
    throw new UnsupportedOperationException("unsupport operation");
  }

  /** return a copy the current bit set.
   */
  public BitSetMask bitSetValue() {
    return new BitSetMask(this);
  }

  int unit[];
  int unitsInUse;

  /** return a BitSetMask with the size first bit set.
   */
  public static BitSetMask fill(int size) {
    int index=unitIndex(size);
    int[] unit=new int[index+1];
    for(int i=index;--i>=0;)
      unit[i]=-1;

    unit[index]=bit(size+1)-1;
    return new BitSetMask(unit,index+1);
  }

  private static int unitIndex(int bitIndex) {
    return bitIndex >> ADDRESS_BITS_PER_UNIT;
  }

  private static int bit(int bitIndex) {
    return 1 << (bitIndex & BIT_INDEX_MASK);
  }

  private final static int ADDRESS_BITS_PER_UNIT = 5;
  private final static int BITS_PER_UNIT = 1 << ADDRESS_BITS_PER_UNIT;
  private final static int BIT_INDEX_MASK = BITS_PER_UNIT - 1;
}
