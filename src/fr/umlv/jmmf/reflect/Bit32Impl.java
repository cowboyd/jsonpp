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
final class Bit32Impl extends AbstractImpl {

  public Bit32Impl(String name,int argLength,TypeSupport support) {

    super(name,argLength,support);

    Annotation32Map[] annotations=new Annotation32Map[argLength];
    for(int i=argLength;--i>=0;)
      annotations[i]=new Annotation32Map();

    this.partialOrders=new int[MAX_PARTIAL_ORDER_SIZE];
    this.annotations=annotations;
  }

  /**
   */
  public int getMapIndex(Class[] types) {

    MethodMap.Entry entry=methodMap.getEntry(types);
    int index=entry.slot;

    // change the current implementation
    if (index>=MAX_PARTIAL_ORDER_SIZE)
      update(new Bit64Impl(this));

    return index;
  }


  /**
   */
  public void addMethod(Class[] types,int methodIndex) {

   // DEBUG
   //System.out.println("methodIndex "+methodIndex);

    Annotation32Map[] annotations=this.annotations;

    // process index from methodIndex
    int index=1<<methodIndex;

    for(int i=types.length;--i>=0;) {

      Class type=types[i];
      Annotation32Map annotation=annotations[i];

      // create annotation for the type
      // and set the bit corresponding to methodIndex
      if (annotation.or(type,index)==-1)
        createAnnotation(annotation,type,null,index,false);

      // DEBUG
      //System.out.println("i "+i+" type "+type+" "+new Bit32Mask(annotation.get(type)));

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
    int[] partialOrders=this.partialOrders;

    int size=methodMap.size();
    for(int i=size;--i>=0;) {
      MethodMap.Entry e=methodMap.getEntry(i);
      int index=1<<e.slot;
      int pOrder=computeApplicableMethod(e.types);

      for(int j=0;pOrder!=0;pOrder>>>=1,j++) {
        //System.out.println("j "+j);
        if ((pOrder & 1)!=0)
          partialOrders[j]|=index;
      }
    }

    // process size
    partialOrderSize=size;
  }

  

  /** compute applicable method if all classes are already
      annotated.
   */
  private int computeApplicableMethod(Class[] args) {
    Annotation32Map[] annotations=this.annotations;

    int bits=0x7FFFFFFF;
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

    Annotation32Map[] annotations=this.annotations;

    // reduce annotation map size
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

  private void processNullAnnotation(Annotation32Map annotation,
    Class clazz,int index) {

    // don't annotate if it's a primitive type
    if (clazz.isPrimitive())
      return;

    // create null class if necessary
    Class nullClass=NULL_CLASS;
    if (annotation.or(nullClass,index)==-1)
      annotation.put(nullClass,index);
  }

  private int createAnnotation(Annotation32Map annotation,Class clazz,
    DAGHierarchy.Entry child,int value,boolean lazy)
  {

    DAGHierarchy.Entry entry;
    DAGHierarchy.Entry old=hierarchy.get(clazz);
    if (old==null)
      entry=hierarchy.add(clazz);
    else
      entry=old;

    if (child!=null) {
    	
      // DEBUG
      //System.out.println("add child to class "+clazz.getName()+" "+child.clazz.getName());
    	
      entry.addSubtype(child);
    }

    child=(old==null)?entry:null;

    // must create a new entry and update all annotations
    Class[] types=TypeModel.getSuperTypes(clazz);
    
    for(int i=types.length;--i>=0;) {

      // compute annotation
      Class type=types[i];
      
      //DEBUG
      //System.out.println("superTypes("+clazz.getName()+")="+type.getName());

      int ann=annotation.get(type);
      if (ann==-1)
        ann=createAnnotation(annotation,type,child,0,lazy);
      else
        // register current entry as subtype of the parent entry
        hierarchy.get(type).addSubtypeIfNotExists(entry);
        

      //DEBUG
      //System.out.println("superType "+type+" annotation "+new Bit32Mask(ann));

      value|=ann;
    }

    if (!(lazy && value==0))
      annotation.put(clazz,value);

    // DEBUG
    //System.out.println("annotation "+clazz+ " value"+
    //  new Bit32Mask(value));
    return value;
  }
  
  private int createAnnotation2(Annotation32Map annotation,Class clazz,
    DAGHierarchy.Entry child)
  {

    DAGHierarchy.Entry entry;
    DAGHierarchy.Entry old=hierarchy.get(clazz);
    if (old==null) {
      entry=hierarchy.add(clazz);
      if (child!=null)
        entry.addSubtype(child);
      child=entry;
    }
    else {
      entry=old;
      if (child!=null)
        entry.addSubtype(child);
      child=null;
    }

    int value=0;
    // must create a new entry and update all annotations
    Class[] types=TypeModel.getSuperTypes(clazz);
    
    for(int i=types.length;--i>=0;) {

      // compute annotation
      Class type=types[i];
      
      //DEBUG
      //System.out.println("superTypes("+clazz.getName()+")="+type.getName());

      int ann=annotation.get(type);
      if (ann==-1)
        ann=createAnnotation2(annotation,type,child);
      else
        // register current entry as subtype of the parent entry
        hierarchy.get(type).addSubtypeIfNotExists(entry);
        

      //DEBUG
      //System.out.println("superType "+type+" annotation "+new Bit32Mask(ann));

      value|=ann;
    }

    if (value!=0)
      annotation.put(clazz,value);

    return value;
  }

  /** @param index is a bit set with only one bit set corresponding
      to the methodIndex.
      NOTE: this method don't process the first entry of the
       hierarchy.
   */
  private void propagate(DAGHierarchy.Entry entry,Annotation32Map map,
    int index) {

    // propagate to other subtypes
    DAGHierarchy.Entry[] children=entry.subtypes;
    if (children==null)
      return;

    for(int i=children.length;--i>=0;) {

      DAGHierarchy.Entry child=children[i];

      // process annotations if their exist
      int bits=map.or(child.clazz,index);
      if (bits==-1)
        // Don't propagate if child has no annotation
        continue;

      // DEBUG
      //System.out.println("propagate on "+child.clazz+" "+new Bit32Mask(bits));

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
    int bits=mask.intValue();

    // if there is no argument
    if (args==null)
      return fbs(bits);

    Annotation32Map[] annotations=this.annotations;

    for(int i=annotations.length;--i>=0;) {
      Annotation32Map annotation=annotations[i];

      int ann;
      Class clazz=args[i];

      // compute annotation
      if (clazz==null) {
        // get special annotation if null
        ann=annotation.get(NULL_CLASS);

      } else {
        // get annotation
        ann=annotation.get(clazz);
        if (ann==-1)
          ann=createAnnotation(annotation,clazz,null,0,true);
      }

      bits&=ann;
    }

    if (bits==0)
      throw createNoSuchMethodException();

    // if there is one bit set
    if ((bits & (bits-1))==0)
      return fbs(bits);

    // disambiguation process
    return disambiguate(bits);
  }

  public int getMethodIndex(Object[] args,BitMask mask)
    throws NoSuchMethodException, MultipleMethodsException
  {
    //System.out.println("mask "+mask);
    int bits=mask.intValue();

    // if there is no argument
    if (args==null)
      return fbs(bits);

    Annotation32Map[] annotations=this.annotations;
    for(int i=annotations.length;--i>=0;) {
      Annotation32Map annotation=annotations[i];

      int ann;
      Object arg=args[i];

      // compute annotation
      if (arg==null) {
        
        // get special annotation if null
        ann=annotation.get(NULL_CLASS);
      } else {
        
        // get annotation
        Class clazz=arg.getClass();
        ann=annotation.get(clazz);
        if (ann==-1)
          //ann=createAnnotation(annotation,clazz,null,0,true);
          ann=createAnnotation2(annotation,clazz,null);
      }

      bits&=ann;
    }

    if (bits==0)
      throw createNoSuchMethodException();

    // if there is one bit set
    if ((bits & (bits-1))==0)
      return fbs(bits);

    // disambiguation process
    return disambiguate(bits);
  }


  /** disambiguation
   */
  private int disambiguate(int bits)
    throws MultipleMethodsException {

    // disambiguation with null values
    int value=applyPartialOrders(bits);

    //System.out.println("value "+new Bit32Mask(value));
    if (value==0)
      throw createMultipleMethodsException(new Bit32Mask(bits));

    // find the first bit set
    return fbs(value);
  }
  
  private final int applyPartialOrders(int bits) {

    /* NON OPTIMIZED VERSION
    int value=bits;
    for(int i=0;value!=0;value>>>=1,i++) {
      if ((value & 1)!=0)
        bits&=partialOrders[i];
    }

    return bits;
    */

    // use local copy
    int[] partialOrders=this.partialOrders;

    int value=bits;
    switch(partialOrderSize) {
      //case n:
      // if ((value & 2^n)!=0)
      //  bits&=partialOrders[n-1];
      case 32:
        if ((value & 0x80000000)!=0)
          bits&=partialOrders[31];
      case 31:
        if ((value & 0x40000000)!=0)
          bits&=partialOrders[30];
      case 30:
        if ((value & 0x20000000)!=0)
          bits&=partialOrders[29];
      case 29:
        if ((value & 0x10000000)!=0)
          bits&=partialOrders[28];
      case 28:
        if ((value & 0x8000000)!=0)
          bits&=partialOrders[27];
      case 27:
        if ((value & 0x4000000)!=0)
          bits&=partialOrders[26];
      case 26:
        if ((value & 0x2000000)!=0)
          bits&=partialOrders[25];
      case 25:
        if ((value & 0x1000000)!=0)
          bits&=partialOrders[24];
      case 24:
        if ((value & 0x800000)!=0)
          bits&=partialOrders[23];
      case 23:
        if ((value & 0x400000)!=0)
          bits&=partialOrders[22];
      case 22:
        if ((value & 0x200000)!=0)
          bits&=partialOrders[21];
      case 21:
        if ((value & 0x100000)!=0)
          bits&=partialOrders[20];
      case 20:
        if ((value & 0x80000)!=0)
          bits&=partialOrders[19];
      case 19:
        if ((value & 0x40000)!=0)
          bits&=partialOrders[18];
      case 18:
        if ((value & 0x20000)!=0)
          bits&=partialOrders[17];
      case 17:
        if ((value & 0x10000)!=0)
          bits&=partialOrders[16];
      case 16:
        if ((value & 0x8000)!=0)
          bits&=partialOrders[15];
      case 15:
        if ((value & 0x4000)!=0)
          bits&=partialOrders[14];
      case 14:
        if ((value & 0x2000)!=0)
          bits&=partialOrders[13];
      case 13:
        if ((value & 0x1000)!=0)
          bits&=partialOrders[12];
      case 12:
        if ((value & 0x800)!=0)
          bits&=partialOrders[11];
      case 11:
        if ((value & 0x400)!=0)
          bits&=partialOrders[10];
      case 10:
        if ((value & 0x200)!=0)
          bits&=partialOrders[9];
      case 9:
        if ((value & 0x100)!=0)
          bits&=partialOrders[8];
      case 8:
        if ((value & 0x80)!=0)
          bits&=partialOrders[7];
      case 7:
        if ((value & 0x40)!=0)
          bits&=partialOrders[6];
      case 6:
        if ((value & 0x20)!=0)
          bits&=partialOrders[5];
      case 5:
        if ((value & 0x10)!=0)
          bits&=partialOrders[4];
      case 4:
        if ((value & 0x8)!=0)
          bits&=partialOrders[3];
      case 3:
        if ((value & 0x4)!=0)
          bits&=partialOrders[2];
      case 2:
        if ((value & 0x2)!=0)
          bits&=partialOrders[1];
      case 1:
        if ((value & 0x1)!=0)
          bits&=partialOrders[0];
      case 0:
    }
    return bits;
  }

  /** return the first bit set.
   */
  private final static int fbs(int bits) {

    /* NON OPTIMIZED VERSION
    for(int i=0;bits!=0;bits>>>=1,i++)
      if ((bits & 1)!=0)
        return i;

    return 0;
    */

    if ((bits & 0xFFFF)!=0) {        // 16
      if ((bits & 0xFF)!=0) {        // 8
        if ((bits & 0xF)!=0) {       // 4
          if ((bits & 0x3)!=0) {     // 2
            if ((bits & 0x1)==0x1)
              return 0;
            else     // 0x2
              return 1;
          } else {
            if ((bits & 0x4)==0x4)
              return 2;
            else     // 0x8
              return 3;
          }
        } else {
          if ((bits & 0x30)!=0) {
            if ((bits & 0x10)==0x10)
              return 4;
            else     // 0x20
              return 5;
          } else {
            if ((bits & 0x40)==0x40)
              return 6;
            else     // 0x80
              return 7;
          }
        }
      } else {
        if ((bits & 0xF00)!=0) {
          if ((bits & 0x300)!=0) {
            if ((bits & 0x100)==0x100)
              return 8;
            else     // 0x200
              return 9;
          } else {
            if ((bits & 0x400)==0x400)
              return 10;
            else     // 0x800
              return 11;
          }
        } else {
          if ((bits & 0x3000)!=0) {     // 2
            if ((bits & 0x1000)==0x1000)
              return 12;
            else     // 0x2000
              return 13;
          } else {
            if ((bits & 0x4000)==0x4000)
              return 14;
            else     // 0x8000
              return 15;
          }
        }
      }
    } else {
      if ((bits & 0xFF0000)!=0) {        // 8
        if ((bits & 0xF0000)!=0) {       // 4
          if ((bits & 0x30000)!=0) {     // 2
            if ((bits & 0x10000)==0x10000)
              return 16;
            else     // 0x20000
              return 17;
          } else {
            if ((bits & 0x40000)==0x40000)
              return 18;
            else     // 0x80000
              return 19;
          }
        } else {
          if ((bits & 0x300000)!=0) {
            if ((bits & 0x100000)==0x100000)
              return 20;
            else     // 0x200000
              return 21;
          } else {
            if ((bits & 0x400000)==0x400000)
              return 22;
            else     // 0x800000
              return 23;
          }
        }
      } else {
        if ((bits & 0xF000000)!=0) {
          if ((bits & 0x3000000)!=0) {
            if ((bits & 0x1000000)==0x1000000)
              return 24;
            else     // 0x2000000
              return 25;
          } else {
            if ((bits & 0x4000000)==0x4000000)
              return 26;
            else     // 0x8000000
              return 27;
          }
        } else {
          if ((bits & 0x30000000)!=0) {     // 2
            if ((bits & 0x10000000)==0x10000000)
              return 28;
            else     // 0x20000000
              return 29;
          } else {
            if ((bits & 0x40000000)==0x40000000)
              return 30;
            else     // 0x80000000
              return 31;
          }
        }
      }
    }

  }

  int partialOrderSize;
  final int[] partialOrders;
  final Annotation32Map[] annotations;

  private static final int MAX_PARTIAL_ORDER_SIZE=31;
}
