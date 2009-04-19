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
abstract class AbstractImpl extends Impl {

  /**
   */
  public AbstractImpl(String name,int argLength,TypeSupport typeSupport,
    MethodMap methodMap,DAGHierarchy hierarchy,
    MultiMethodMap multiMethodMap) {
    this.name=name;
    this.argLength=argLength;
    this.typeSupport=typeSupport;

    this.methodMap=methodMap;
    this.hierarchy=hierarchy;
    this.multiMethodMap=multiMethodMap;
  }

  /**
   */
  public AbstractImpl(String name,int argLength,TypeSupport typeSupport) {
    this(name,argLength,typeSupport,new MethodMap(),
      new DAGHierarchy(),new MultiMethodMap());
  }

  /**
   */
  public AbstractImpl(AbstractImpl impl) {
    this(impl.name,impl.argLength,impl.typeSupport,impl.methodMap,
      impl.hierarchy,impl.multiMethodMap);
  }

  /** return the name of the multi-method.
   */
  public String getName() {
    return name;
  }

  /** return the number of argument.
   */
  public int getArgLength() {
    return argLength;
  }

   /** return the method map.
   */
  public MethodMap getMethodMap() {
    return methodMap;
  }


  /** return the type support.
   */
  public TypeSupport getTypeSupport() {
    return typeSupport;
  }

  /** return the multi-method of a given class for a type support.
   */
  public MultiMethod getMultiMethod(Class clazz,boolean onlyPublic) {
    MultiMethod mm=multiMethodMap.get(clazz,onlyPublic);
    if (mm==null) {
      mm=new MultiMethod(this,onlyPublic);
      multiMethodMap.put(clazz,mm);
      mm.init(clazz);
    }
    return mm;
  }

  /** support method.
      @see update
   */
  protected void update(Impl impl) {
    MultiMethodMap.Iterator it=multiMethodMap.iterator();
    for(MultiMethod mm;(mm=it.next())!=null;) {
      // send an update message to change implementation
      // for each MultiMethod
      mm.update(impl);
    }
  }

  protected NoSuchMethodException createNoSuchMethodException() {
    return new NoSuchMethodException("no method "+name+
      " of declaring class match");
  }

  protected MultipleMethodsException createMultipleMethodsException(BitMask bits) {
    StringBuffer buffer=new StringBuffer(128);
    buffer.append("multiple methods ");
    buffer.append(name);
    buffer.append(" of declaring class match\n");

    MethodMap methodMap=this.methodMap;
    for(int i=bits.length();--i>=0;)
      if (bits.get(i)) {
        buffer.append("  ");
        buffer.append(name);
        buffer.append('(');
        Class[] types=methodMap.getEntry(i).types;

        int length=types.length;
        for(int j=0;j<length;j++) {
          buffer.append(types[j].getName());
          buffer.append(',');
        }
        if (length!=0)
          buffer.setLength(buffer.length()-1);
        buffer.append(")\n");
      }

    return new MultipleMethodsException(buffer.toString());
  }

  /** return the index of the parameter type such as
      <pre>
      types1[index]!=types2[index] and
      for all i!=index, types1[i]==types2[i].
      </pre>

      @return -1 if multiple index value could be found.
   */
  protected final static int indexOf(Class[] types1, Class[] types2) {
    int index=-1;
    for(int i=types1.length;--i>=0;) {
      if (types1[i]!=types2[i])
        if (index!=-1)
          return -1;
        else
          index=i;
    }

    //DEBUG: ASSERT(index!=-1)
    //if (index==-1)
    //  throw new InternalError("Bad algorihtm: index==-1");

    return index;
  }

  final String name;
  final int argLength;

  protected final MethodMap methodMap;
  protected final DAGHierarchy hierarchy;
  protected final TypeSupport typeSupport;
  final MultiMethodMap multiMethodMap;
}
