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

/** this class contains annotation and partial order
    between methods for a specific multi-method.

    @author Remi Forax
    @version 0.9
 */
final class Bit64Impl extends AbstractImpl {

  public Bit64Impl(Bit32Impl impl) {

    super(impl);

    Annotation32Map[] implAnnotations=impl.annotations;
    Annotation64Map[] annotations=new Annotation64Map[argLength];
    for(int i=argLength;--i>=0;)
      annotations[i]=new Annotation64Map(implAnnotations[i]);
    this.annotations=annotations;

    int size=impl.partialOrderSize;
    int[] implPartialOrders=impl.partialOrders;
    long[] partialOrders=new long[MAX_PARTIAL_ORDER_SIZE];
    for(int i=size;--i>=0;)
      partialOrders[i]=implPartialOrders[i];

    this.partialOrderSize=size;
    this.partialOrders=partialOrders;
  }

  /**
   */
  public int getMapIndex(Class[] types) {

    MethodMap.Entry entry=methodMap.getEntry(types);

    int index=entry.slot;

    // change the current implementation
    if (index>=MAX_PARTIAL_ORDER_SIZE)
      update(new BitSetImpl(this));

    return index;
  }

  /**
   */
  public void addMethod(Class[] types,int methodIndex) {

    Annotation64Map[] annotations=this.annotations;

    // process index from methodIndex
    long index=1L<<methodIndex;

    for(int i=types.length;--i>=0;) {

      Class type=types[i];
      Annotation64Map annotation=annotations[i];

      // create annotation for the type
      // and set the bit corresponding to methodIndex
      if (annotation.or(type,index)==-1L)
        createAnnotation(annotation,type,null,index,false);

      // DEBUG
      //System.out.println(type+" "+new Bit64Mask(annotation.get(type)));

      DAGHierarchy.Entry entry=hierarchy.get(type);

      //propagate
      propagate(entry,annotation,index);


      // create a special "null" annotation
      processNullAnnotation(annotation,type,index);
    }
  }


  /** compute partial orders.
      THE ALGORITHM COULD BE OPTIMIZED BUT
      it needs more operations ??
   */
  private void computePartialOrders() {
    MethodMap methodMap=this.methodMap;
    long[] partialOrders=this.partialOrders;

    int size=methodMap.size();
    for(int i=size;--i>=0;) {
      MethodMap.Entry e=methodMap.getEntry(i);
      long index=1L<<e.slot;
      long pOrder=computeApplicableMethod(e.types);

      for(int j=0;pOrder!=0;pOrder>>>=1L,j++) {
        if ((pOrder & 1L)!=0)
          partialOrders[j]|=index;
      }
    }

    // process size
    partialOrderSize=size;
  }

  /** compute applicable method if all classes are already
      annotated.
   */
  private long computeApplicableMethod(Class[] args) {
    Annotation64Map[] annotations=this.annotations;

    long bits=0x7FFFFFFFFFFFFFFFL;
    for(int i=args.length;--i>=0;) {

      // compute annotation
      bits&=annotations[i].get(args[i]);
    }
    return bits;
  }

  /** this method compute partial order table.
      MUST be called after addMethod() when all methods of a class are added.
   */
  public void endsConstruction() {

    //System.out.println("end construction");

    // optimize annotation map
    Annotation64Map[] annotations=this.annotations;
    for(int i=annotations.length;--i>=0;) {
      annotations[i].compact();

      // DEBUG
      //System.out.println("pre-annotation "+i);
      //annotations[i].debug();
    }

    // compute partial orders
    computePartialOrders();

    // DEBUG
    //for(int i=annotations.length;--i>=0;) {
    //  System.out.println("post-annotation "+i);
    //  annotations[i].debug();
    //}

    // DEBUG PARTIAL ORDERS
    //for(int i=0;i<partialOrderSize;i++) {
    //  System.out.println("po "+new Bit32Mask(partialOrders[i]));
    //}
  }

  private void processNullAnnotation(Annotation64Map annotation,
    Class clazz,long index) {

    // don't annotate if it'a a primitive type
    if (clazz.isPrimitive())
      return;

    // create null class if necessary
    Class nullClass=NULL_CLASS;
    if (annotation.or(nullClass,index)==-1L)
      annotation.put(nullClass,index);
  }

  private long createAnnotation(Annotation64Map annotation,Class clazz,
    DAGHierarchy.Entry child,long value,boolean lazy)
  {
    DAGHierarchy.Entry entry;
    DAGHierarchy.Entry old=hierarchy.get(clazz);
    if (old==null)
      entry=hierarchy.add(clazz);
    else
      entry=old;

    if (child!=null) {
      entry.addSubtype(child);
    }

    child=(old==null)?entry:null;

    // must create a new entry and update all annotations
    Class[] types=TypeModel.getSuperTypes(clazz);
    for(int i=types.length;--i>=0;) {

      // compute annotation
      Class type=types[i];

      long ann=annotation.get(type);
      if (ann==-1L)
        ann=createAnnotation(annotation,type,child,0,lazy);
      else
        // register current entry as subtype of the parent entry
        hierarchy.get(type).addSubtypeIfNotExists(entry);

      value|=ann;
    }

    if (!(lazy && value==0))
      annotation.put(clazz,value);

    // DEBUG
    //System.out.println("annotation "+clazz+ " value"+
    //  new Bit32Mask(value));
    return value;
  }

  /** @param index is a bit set with only one bit set corresponding
      to the methodIndex.
      NOTE: this method don't process the first entry of the
       hierarchy.
   */
  private void propagate(DAGHierarchy.Entry entry,Annotation64Map map,
    long index) {

    // propagate to other subtypes
    DAGHierarchy.Entry[] children=entry.subtypes;
    if (children==null)
      return;

    for(int i=children.length;--i>=0;) {

      DAGHierarchy.Entry child=children[i];

      // process annotations if their exist
      long bits=map.or(child.clazz,index);
      if (bits==-1L)
        // Don't propagate if child have no annotation
        continue;

      // DEBUG
      //System.out.println("propagate on "+child.clazz+" "+new Bit64Mask(bits));

      // propagate to children
      propagate(child,map,index);
    }
  }

  /** return the index of most specific method.
   */
  public int getMethodIndex(Class[] args,BitMask mask)
    throws NoSuchMethodException, MultipleMethodsException
  {
    //System.out.println("mask "+mask);
    long bits=mask.longValue();

    boolean someArgsAreNull=false;
    Annotation64Map[] annotations=this.annotations;

    for(int i=annotations.length;--i>=0;) {
      Annotation64Map annotation=annotations[i];

      long ann;
      Class clazz=args[i];

      // compute annotation
      if (clazz==null) {
        // get special annotation if null
        ann=annotation.get(NULL_CLASS);
        someArgsAreNull=true;

      } else {
        // get annotation
        ann=annotation.get(clazz);
        if (ann==-1L)
          ann=createAnnotation(annotation,clazz,null,0,true);
      }

      bits&=ann;
    }

    if (bits==0)
      throw createNoSuchMethodException();

    // if there is one bit set
    if ((bits & (bits-1L))==0)
      return fbs(bits);

    // disambiguation process
    return (someArgsAreNull)?disambiguate(bits,args):
      disambiguate(bits);
  }

  /** return the index of most specific method.
   */
  public int getMethodIndex(Object[] args,BitMask mask)
    throws NoSuchMethodException, MultipleMethodsException
  {
    //System.out.println("mask "+mask);
    long bits=mask.longValue();

    // if there is no argument
    if (args==null)
      return 0;

    Annotation64Map[] annotations=this.annotations;
    for(int i=annotations.length;--i>=0;) {
      Annotation64Map annotation=annotations[i];

      long ann;
      Object arg=args[i];

      // compute annotation
      if (arg==null) {
        // get special annotation if null
        ann=annotation.get(NULL_CLASS);

      } else {
        // get annotation
        Class clazz=arg.getClass();
        ann=annotation.get(clazz);
        if (ann==-1L)
          ann=createAnnotation(annotation,clazz,null,0,true);
      }

      bits&=ann;
    }

    if (bits==0)
      throw createNoSuchMethodException();

    // if there is one bit set
    if ((bits & (bits-1L))==0)
      return fbs(bits);

    //System.out.println("bits "+new Bit64Mask(bits));

    // disambiguation process
    return (bits<0)?disambiguate(-bits,args):
      disambiguate(bits);
  }

  /** disambiguation
   */
  private int disambiguate(long bits)
    throws MultipleMethodsException {

    // disambiguation with null values
    long value=applyPartialOrders(bits);

    //System.out.println("value "+new Bit64Mask(value));
    if (value==0)
      throw createMultipleMethodsException(new Bit64Mask(bits));

    // find the first bit set
    return fbs(value);
  }

  /** disambiguation if there are some null values
   */
  private int disambiguate(long bits,Object[] args)
    throws MultipleMethodsException {
    //System.out.println("null: bits "+new Bit64Mask(bits));

    // disambiguation
    long value=applyNullPartialOrders(bits,args);

    //System.out.println("null: value "+new Bit64Mask(value));

    // if no bit set or more than one bit set
    if (value==0 || (value & (value-1L))!=0)
      throw createMultipleMethodsException(new Bit64Mask(bits));

    return fbs(value);
  }

  private long computeNullApplicableMethod(Class[] args,
    Object[] objects,long bits) {
    Annotation64Map[] annotations=this.annotations;

    for(int i=args.length;--i>=0;) {
      Annotation64Map annotation=annotations[i];

      long ann;

      // compute annotation
      if (objects[i]!=null) {
        Class clazz=args[i];
        ann=annotation.get(clazz);
        if (ann==-1L)
          ann=createAnnotation(annotation,clazz,null,0,true);
      } else {
        ann=annotation.get(NULL_CLASS);
      }

      bits&=ann;
    }
    return bits;
  }

  private long applyNullPartialOrders(long bits,Object[] args) {

    long index=0;
    MethodMap map=this.methodMap;

    int i=lbs(bits);
    long mask=1L<<i;
loop:for(long mask1=mask;mask1!=0;mask1>>>=1L,i--) {
      if ((bits & mask1)!=0) {
        Class[] types=map.getEntry(i).types;
        long pOrder=computeNullApplicableMethod(types,args,bits);

        for(long mask2=mask;mask2!=0;mask2>>>=1L) {
          //if ((bits & mask2)!=0 && (pOrder & mask2)==0) {
          if ((bits & mask2 & (~pOrder))!=0) {
            index<<=1L;
            continue loop;
          }
        }
        index=(index<<1L)|1L;
      }
      else
        index<<=1L;
    }

    return index;
  }

  private final long applyPartialOrders(long bits) {

    /* NON OPTIMIZED VERSION
    long value=bits;
    for(int i=0;value!=0;value>>>=1L,i++) {
      if ((value & 1L)!=0)
        bits&=partialOrders[i];
    }

    return bits;
    */

    // use local copy
    long[] partialOrders=this.partialOrders;

    long value=bits;
    switch(partialOrderSize) {
      //case n:
      // if ((value & 2^n)!=0)
      //  bits&=partialOrders[n-1];
      case 64:
        if ((value & 0x8000000000000000L)!=0L)
          bits&=partialOrders[63];
      case 63:
        if ((value & 0x4000000000000000L)!=0L)
          bits&=partialOrders[62];
      case 62:
        if ((value & 0x2000000000000000L)!=0L)
          bits&=partialOrders[61];
      case 61:
        if ((value & 0x1000000000000000L)!=0L)
          bits&=partialOrders[60];
      case 60:
        if ((value & 0x800000000000000L)!=0L)
          bits&=partialOrders[59];
      case 59:
        if ((value & 0x400000000000000L)!=0L)
          bits&=partialOrders[58];
      case 58:
        if ((value & 0x200000000000000L)!=0L)
          bits&=partialOrders[57];
      case 57:
        if ((value & 0x100000000000000L)!=0L)
          bits&=partialOrders[56];
      case 56:
        if ((value & 0x80000000000000L)!=0L)
          bits&=partialOrders[55];
      case 55:
        if ((value & 0x40000000000000L)!=0L)
          bits&=partialOrders[54];
      case 54:
        if ((value & 0x20000000000000L)!=0L)
          bits&=partialOrders[53];
      case 53:
        if ((value & 0x10000000000000L)!=0L)
          bits&=partialOrders[52];
      case 52:
        if ((value & 0x8000000000000L)!=0L)
          bits&=partialOrders[51];
      case 51:
        if ((value & 0x4000000000000L)!=0L)
          bits&=partialOrders[50];
      case 50:
        if ((value & 0x2000000000000L)!=0L)
          bits&=partialOrders[49];
      case 49:
        if ((value & 0x1000000000000L)!=0L)
          bits&=partialOrders[48];
      case 48:
        if ((value & 0x800000000000L)!=0L)
          bits&=partialOrders[47];
      case 47:
        if ((value & 0x400000000000L)!=0L)
          bits&=partialOrders[46];
      case 46:
        if ((value & 0x200000000000L)!=0L)
          bits&=partialOrders[45];
      case 45:
        if ((value & 0x100000000000L)!=0L)
          bits&=partialOrders[44];
      case 44:
        if ((value & 0x80000000000L)!=0L)
          bits&=partialOrders[43];
      case 43:
        if ((value & 0x40000000000L)!=0L)
          bits&=partialOrders[42];
      case 42:
        if ((value & 0x20000000000L)!=0L)
          bits&=partialOrders[41];
      case 41:
        if ((value & 0x10000000000L)!=0L)
          bits&=partialOrders[40];
      case 40:
        if ((value & 0x8000000000L)!=0L)
          bits&=partialOrders[39];
      case 39:
        if ((value & 0x4000000000L)!=0L)
          bits&=partialOrders[38];
      case 38:
        if ((value & 0x2000000000L)!=0L)
          bits&=partialOrders[37];
      case 37:
        if ((value & 0x1000000000L)!=0L)
          bits&=partialOrders[36];
      case 36:
        if ((value & 0x800000000L)!=0L)
          bits&=partialOrders[35];
      case 35:
        if ((value & 0x400000000L)!=0L)
          bits&=partialOrders[34];
      case 34:
        if ((value & 0x200000000L)!=0L)
          bits&=partialOrders[33];
      case 33:
        if ((value & 0x100000000L)!=0L)
          bits&=partialOrders[32];
      case 32:
        if ((value & 0x80000000L)!=0L)
          bits&=partialOrders[31];
      case 31:
        if ((value & 0x40000000L)!=0L)
          bits&=partialOrders[30];
      case 30:
        if ((value & 0x20000000L)!=0L)
          bits&=partialOrders[29];
      case 29:
        if ((value & 0x10000000L)!=0L)
          bits&=partialOrders[28];
      case 28:
        if ((value & 0x8000000L)!=0L)
          bits&=partialOrders[27];
      case 27:
        if ((value & 0x4000000L)!=0L)
          bits&=partialOrders[26];
      case 26:
        if ((value & 0x2000000L)!=0L)
          bits&=partialOrders[25];
      case 25:
        if ((value & 0x1000000L)!=0L)
          bits&=partialOrders[24];
      case 24:
        if ((value & 0x800000L)!=0L)
          bits&=partialOrders[23];
      case 23:
        if ((value & 0x400000L)!=0L)
          bits&=partialOrders[22];
      case 22:
        if ((value & 0x200000L)!=0L)
          bits&=partialOrders[21];
      case 21:
        if ((value & 0x100000L)!=0L)
          bits&=partialOrders[20];
      case 20:
        if ((value & 0x80000L)!=0L)
          bits&=partialOrders[19];
      case 19:
        if ((value & 0x40000L)!=0L)
          bits&=partialOrders[18];
      case 18:
        if ((value & 0x20000L)!=0L)
          bits&=partialOrders[17];
      case 17:
        if ((value & 0x10000L)!=0L)
          bits&=partialOrders[16];
      case 16:
        if ((value & 0x8000L)!=0L)
          bits&=partialOrders[15];
      case 15:
        if ((value & 0x4000L)!=0L)
          bits&=partialOrders[14];
      case 14:
        if ((value & 0x2000L)!=0L)
          bits&=partialOrders[13];
      case 13:
        if ((value & 0x1000L)!=0L)
          bits&=partialOrders[12];
      case 12:
        if ((value & 0x800L)!=0L)
          bits&=partialOrders[11];
      case 11:
        if ((value & 0x400L)!=0L)
          bits&=partialOrders[10];
      case 10:
        if ((value & 0x200L)!=0L)
          bits&=partialOrders[9];
      case 9:
        if ((value & 0x100L)!=0L)
          bits&=partialOrders[8];
      case 8:
        if ((value & 0x80L)!=0L)
          bits&=partialOrders[7];
      case 7:
        if ((value & 0x40L)!=0L)
          bits&=partialOrders[6];
      case 6:
        if ((value & 0x20L)!=0L)
          bits&=partialOrders[5];
      case 5:
        if ((value & 0x10L)!=0L)
          bits&=partialOrders[4];
      case 4:
        if ((value & 0x8L)!=0L)
          bits&=partialOrders[3];
      case 3:
        if ((value & 0x4L)!=0L)
          bits&=partialOrders[2];
      case 2:
        if ((value & 0x2L)!=0L)
          bits&=partialOrders[1];
      case 1:
        if ((value & 0x1L)!=0L)
          bits&=partialOrders[0];
      case 0:
    }
    return bits;
  }

  /** return the last bit set.
     */
  private final static int lbs(long bits) {

    /* NON OPTIMIZED VERSION */
    int i=63;
    for(long mask=0x8000000000000000L;mask!=0;mask>>>=1,i--)
      if ((bits & mask)!=0)
        return i;

    throw new InternalError("bad algorithm");
  }

  /** return the first bit set.
   */
  private final static int fbs(long bits) {

    /* NON OPTIMIZED VERSION
    for(int i=0;bits!=0;bits>>>=1L,i++)
      if ((bits & 1L)!=0)
        return i;

    return 0;
    */

    if ((bits & 0xFFFFFFFFL)!=0L) {      // 32

      if ((bits & 0xFFFFL)!=0L) {        // 16
        if ((bits & 0xFFL)!=0L) {        // 8
          if ((bits & 0xFL)!=0L) {       // 4
            if ((bits & 0x3L)!=0L) {     // 2
              if ((bits & 0x1L)==0x1L)
                return 0;
              else     // 0x2
                return 1;
            } else {
              if ((bits & 0x4L)==0x4L)
                return 2;
              else     // 0x8
                return 3;
            }
          } else {
            if ((bits & 0x30L)!=0L) {
              if ((bits & 0x10L)==0x10L)
                return 4;
              else     // 0x20
                return 5;
            } else {
              if ((bits & 0x40L)==0x40L)
                return 6;
              else     // 0x80
                return 7;
            }
          }
        } else {
          if ((bits & 0xF00L)!=0L) {
            if ((bits & 0x300L)!=0L) {
              if ((bits & 0x100L)==0x100L)
                return 8;
              else     // 0x200
                return 9;
            } else {
              if ((bits & 0x400L)==0x400L)
                return 10;
              else     // 0x800
                return 11;
            }
          } else {
            if ((bits & 0x3000L)!=0L) {     // 2
              if ((bits & 0x1000L)==0x1000L)
                return 12;
              else     // 0x2000
                return 13;
            } else {
              if ((bits & 0x4000L)==0x4000L)
                return 14;
              else     // 0x8000
                return 15;
            }
          }
        }
      } else {
        if ((bits & 0xFF0000L)!=0L) {        // 8
          if ((bits & 0xF0000L)!=0L) {       // 4
            if ((bits & 0x30000L)!=0L) {     // 2
              if ((bits & 0x10000L)==0x10000L)
                return 16;
              else     // 0x20000
                return 17;
            } else {
              if ((bits & 0x40000L)==0x40000L)
                return 18;
              else     // 0x80000
                return 19;
            }
          } else {
            if ((bits & 0x300000L)!=0L) {
              if ((bits & 0x100000L)==0x100000L)
                return 20;
              else     // 0x200000
                return 21;
            } else {
              if ((bits & 0x400000L)==0x400000L)
                return 22;
              else     // 0x800000
                return 23;
            }
          }
        } else {
          if ((bits & 0xF000000L)!=0L) {
            if ((bits & 0x3000000L)!=0L) {
              if ((bits & 0x1000000L)==0x1000000L)
                return 24;
              else     // 0x2000000
                return 25;
            } else {
              if ((bits & 0x4000000L)==0x4000000L)
                return 26;
              else     // 0x8000000
                return 27;
            }
          } else {
            if ((bits & 0x30000000L)!=0L) {     // 2
              if ((bits & 0x10000000L)==0x10000000L)
                return 28;
              else     // 0x20000000
                return 29;
            } else {
              if ((bits & 0x40000000L)==0x40000000L)
                return 30;
              else     // 0x80000000
                return 31;
            }
          }
        }
      }
    } else {



        if ((bits & 0xFFFF00000000L)!=0L) {        // 16
          if ((bits & 0xFF00000000L)!=0L) {        // 8
            if ((bits & 0xF00000000L)!=0L) {       // 4
              if ((bits & 0x300000000L)!=0L) {     // 2
                if ((bits & 0x100000000L)==0x100000000L)
                  return 32;
                else     // 0x200000000
                  return 33;
              } else {
                if ((bits & 0x4000000000L)==0x4000000000L)
                  return 34;
                else     // 0x800000000
                  return 35;
              }
            } else {
              if ((bits & 0x3000000000L)!=0L) {
                if ((bits & 0x1000000000L)==0x1000000000L)
                  return 36;
                else     // 0x2000000000
                  return 37;
              } else {
                if ((bits & 0x4000000000L)==0x4000000000L)
                  return 38;
                else     // 0x8000000000
                  return 39;
              }
            }
          } else {
            if ((bits & 0xF0000000000L)!=0L) {
              if ((bits & 0x30000000000L)!=0L) {
                if ((bits & 0x10000000000L)==0x10000000000L)
                  return 40;
                else     // 0x20000000000
                  return 41;
              } else {
                if ((bits & 0x40000000000L)==0x40000000000L)
                  return 42;
                else     // 0x80000000000
                  return 43;
              }
            } else {
              if ((bits & 0x300000000000L)!=0L) {     // 2
                if ((bits & 0x100000000000L)==0x100000000000L)
                  return 44;
                else     // 0x200000000000
                  return 45;
              } else {
                if ((bits & 0x400000000000L)==0x400000000000L)
                  return 46;
                else     // 0x800000000000
                  return 47;
              }
            }
          }
        } else {
          if ((bits & 0xFF000000000000L)!=0L) {        // 8
            if ((bits & 0xF000000000000L)!=0L) {       // 4
              if ((bits & 0x3000000000000L)!=0L) {     // 2
                if ((bits & 0x1000000000000L)==0x1000000000000L)
                  return 48;
                else     // 0x2000000000000
                  return 49;
              } else {
                if ((bits & 0x4000000000000L)==0x4000000000000L)
                  return 50;
                else     // 0x8000000000000
                  return 51;
              }
            } else {
              if ((bits & 0x30000000000000L)!=0L) {
                if ((bits & 0x10000000000000L)==0x10000000000000L)
                  return 52;
                else     // 0x20000000000000
                  return 53;
              } else {
                if ((bits & 0x40000000000000L)==0x40000000000000L)
                  return 54;
                else     // 0x80000000000000
                  return 55;
              }
            }
          } else {
            if ((bits & 0xF00000000000000L)!=0L) {
              if ((bits & 0x300000000000000L)!=0L) {
                if ((bits & 0x100000000000000L)==0x100000000000000L)
                  return 56;
                else     // 0x200000000000000
                  return 57;
              } else {
                if ((bits & 0x400000000000000L)==0x400000000000000L)
                  return 58;
                else     // 0x800000000000000
                  return 59;
              }
            } else {
              if ((bits & 0x3000000000000000L)!=0L) {     // 2
                if ((bits & 0x1000000000000000L)==0x1000000000000000L)
                  return 60;
                else     // 0x2000000000000000
                  return 61;
              } else {
                if ((bits & 0x4000000000000000L)==0x4000000000000000L)
                  return 62;
                else     // 0x8000000000000000
                  return 63;
              }
            }
        }
      }
    }
  }

  /** DEBUG: internal test
   */
  /*public static void main(String[] args) {
    // test fbs
    for(int i=0;i<64;i++) {
      System.out.print(fbs(1L<<i)+" ");
    }
    System.out.println();

  }*/

  int partialOrderSize;
  final long[] partialOrders;
  final Annotation64Map[] annotations;

  private static final int MAX_PARTIAL_ORDER_SIZE=63;
}
