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
final class BitSetImpl extends AbstractImpl {

  /** for performance only.
   */
  public BitSetImpl(String name,int argLength,TypeSupport support) {

    super(name,argLength,support);

    AnnotationSetMap[] annotations=new AnnotationSetMap[argLength];
    for(int i=argLength;--i>=0;)
      annotations[i]=new AnnotationSetMap();

    this.annotations=annotations;
    this.partialOrders=new BitSetMask[0];
  }

  public BitSetImpl(Bit64Impl impl) {

    super(impl);

    Annotation64Map[] implAnnotations=impl.annotations;
    AnnotationSetMap[] annotations=new AnnotationSetMap[argLength];
    for(int i=argLength;--i>=0;)
      annotations[i]=new AnnotationSetMap(implAnnotations[i]);
    this.annotations=annotations;

    int size=impl.partialOrderSize;
    //int capacity=(size*3)/2;
    int capacity=size;

    long[] implPartialOrders=impl.partialOrders;
    BitSetMask[] partialOrders=new BitSetMask[capacity];
    for(int i=size;--i>=0;)
      partialOrders[i]=new BitSetMask(implPartialOrders[i]);

    this.partialOrders=partialOrders;
    this.partialOrderSize=size;
  }

  /**
   */
  public int getMapIndex(Class[] types) {

    MethodMap.Entry entry=methodMap.getEntry(types);

    int index=entry.slot;

    // change the current implementation
    // no update here

    return index;
  }

  /**
   */
  public void addMethod(Class[] types,int index) {

    AnnotationSetMap[] annotations=this.annotations;

    for(int i=types.length;--i>=0;) {

      Class type=types[i];
      AnnotationSetMap annotation=annotations[i];

      // create annotation for the type
      // and set the bit corresponding to index
      BitSetMask ann=annotation.set(type,index);
      if (ann==null) {
        ann=createAnnotation(annotation,type,null,
          new BitSetMask(),false);
        ann.set(index);
      }

      // DEBUG
      //System.out.println(type+" "+annotation.get(type));

      DAGHierarchy.Entry entry=hierarchy.get(type);

      //propagate
      propagate(entry,annotation,index);

      // create a special "null" annotation
      createNullAnnotation(annotation,type,index);

      //DEBUG
      //if (!ann.onlyOneBitSet())
      //  throw new InternalError("error in algorithm");
    }
  }


  /** compute partial orders.
      THE ALGORITHM COULD BE OPTIMIZED BUT
      it needs more operations ??
   */
  private void computePartialOrders() {
    MethodMap methodMap=this.methodMap;
    BitSetMask[] partialOrders=this.partialOrders;

    int size=methodMap.size();

    // ensure capacity of the partial order array
    int length=partialOrders.length;
    if (size>length) {
      BitSetMask[] newPartialOrders=new BitSetMask[size];
      if (length!=0)
        System.arraycopy(partialOrders,0,newPartialOrders,0,length);

      for(int i=size;--i>=length;)
        newPartialOrders[i]=new BitSetMask();

      this.partialOrders=partialOrders=newPartialOrders;
    }

    // process partial orders
    for(int i=size;--i>=0;) {
      MethodMap.Entry e=methodMap.getEntry(i);
      int index=e.slot;

      BitSetMask pOrder=computeApplicableMethod(e.types);

      for(int j=pOrder.length();--j>=0;) {
        //System.out.println("j "+j);
        if (pOrder.get(j))
          partialOrders[j].set(index);
      }
    }

    // process size
    partialOrderSize=size;
  }

  /** compute applicable method if all classes are already
      annotated.
   */
  private BitSetMask computeApplicableMethod(Class[] args) {
    AnnotationSetMap[] annotations=this.annotations;

    BitSetMask bits=new BitSetMask(
      annotations[0].get(args[0]));

    for(int i=args.length;--i>0;) {

      // compute annotation
      bits.and(annotations[i].get(args[i]));
    }
    return bits;
  }

  /** this method compute partial order table.
      MUST be called after addMethod() when all methods of a class are added.
   */
  public void endsConstruction() {

    // optimize annotation map
    AnnotationSetMap[] annotations=this.annotations;
    for(int i=annotations.length;--i>=0;) {
      annotations[i].compact();

      // DEBUG
      //System.out.println("annotation "+i);
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
    //  System.out.println("po "+partialOrders[i]);
    //}
  }

  private void createNullAnnotation(AnnotationSetMap annotation,
    Class clazz,int index) {

    // don't annotate if it'a a primitive type
    if (clazz.isPrimitive())
      return;

    // create null class if necessary
    Class nullClass=NULL_CLASS;
    if (annotation.set(nullClass,index)==null) {
      BitSetMask ann=new BitSetMask();
      ann.set(index);
      annotation.put(nullClass,ann);
    }
  }

  private BitSetMask createAnnotation(AnnotationSetMap annotation,Class clazz,
    DAGHierarchy.Entry child,BitSetMask value,boolean lazy)
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

      BitSetMask ann=annotation.get(type);
      if (ann==null)
        ann=createAnnotation(annotation,type,child,
          new BitSetMask(),lazy);
      else
        // register current entry as subtype of the parent entry
        hierarchy.get(type).addSubtypeIfNotExists(entry);

      value.or(ann);
    }

    if (!(lazy && value.length()==0))
      annotation.put(clazz,value);

    // DEBUG
    //System.out.println("annotation "+clazz+ " value "+value);
    return value;
  }

  /** @param index is the value of the methodIndex.
   */
  private void propagate(DAGHierarchy.Entry entry,AnnotationSetMap map,
    int index) {

    // propagate to other subtypes
    DAGHierarchy.Entry[] children=entry.subtypes;
    if (children==null)
      return;

    for(int i=children.length;--i>=0;) {

      DAGHierarchy.Entry child=children[i];

      // process annotations if their exist
      BitSetMask bits=map.set(child.clazz,index);
      if (bits==null)
        // Don't propagate if child have no annotation
        continue;

      // DEBUG
      //System.out.println("propagate on "+child.clazz+" "+bits);

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
    BitSetMask bits=mask.bitSetValue();

    AnnotationSetMap[] annotations=this.annotations;
    for(int i=annotations.length;--i>=0;) {
      AnnotationSetMap annotation=annotations[i];

      BitSetMask ann;
      Class clazz=args[i];

      // compute annotation
      if (clazz==null) {
        // get special annotation if null
        ann=annotation.get(NULL_CLASS);
        
      } else {
        // get annotation
        ann=annotation.get(clazz);
        if (ann==null)
          ann=createAnnotation(annotation,clazz,null,
            new BitSetMask(),true);
      }

      bits.and(ann);
    }

    int length=bits.length();
    if (length==0)
      throw createNoSuchMethodException();

    // if there is one bit set
    if (bits.onlyOneBitSet())
      return length-1;

    // disambiguation process
    return disambiguate(bits);
  }

  /** return the index of most specific method.
   */
  public int getMethodIndex(Object[] args,BitMask mask)
    throws NoSuchMethodException, MultipleMethodsException
  {
    //System.out.println("mask "+mask);
    BitSetMask bits=mask.bitSetValue();

    AnnotationSetMap[] annotations=this.annotations;
    for(int i=annotations.length;--i>=0;) {
      AnnotationSetMap annotation=annotations[i];

      BitSetMask ann;
      Object arg=args[i];

      // compute annotation
      if (arg==null) {
        // get special annotation if null
        ann=annotation.get(NULL_CLASS);
        
      } else {
        // get annotation
        Class clazz=arg.getClass();
        ann=annotation.get(clazz);
        if (ann==null)
          ann=createAnnotation(annotation,clazz,null,
            new BitSetMask(),true);
      }

      bits.and(ann);
    }

    //System.out.println("bits "+bits);

    int length=bits.length();
    if (length==0)
      throw createNoSuchMethodException();

    // if there is one bit set
    if (bits.onlyOneBitSet())
      return length-1;

    // disambiguation process
    return disambiguate(bits);
  }

  /** disambiguation
   */
  private int disambiguate(BitSetMask bits)
    throws MultipleMethodsException {

    // disambiguation
    BitSetMask value=applyPartialOrders(bits);
    //System.out.println("value "+value);

    int length=value.length();
    if (length==0)
      throw createMultipleMethodsException(bits);

    // return the first bit set
    return length-1;
  }

  private final BitSetMask applyPartialOrders(BitSetMask bits) {

    /* NON OPTIMIZED VERSION */
    BitSetMask[] partialOrders=this.partialOrders;

    BitSetMask value=new BitSetMask(bits);
    for(int i=value.length();--i>=0;) {
      if (value.get(i))
        bits.and(partialOrders[i]);
    }

    return bits;
  }

  int partialOrderSize;
  BitSetMask[] partialOrders;
  final AnnotationSetMap[] annotations;
}
