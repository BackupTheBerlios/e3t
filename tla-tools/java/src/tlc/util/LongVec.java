// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Mon Dec  4 16:20:19 PST 2000 by yuanyu
package tlc.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.zambrovski.tla.RuntimeConfiguration;

public class LongVec implements Cloneable, Serializable {
  private long[] elementData;
  private int elementCount;
         
  public LongVec() { this(10); }

  public LongVec(int initialCapacity) {
    this.elementCount = 0;
    this.elementData = new long[initialCapacity];
  }

  public final void addElement(long x) {
    if (this.elementCount == this.elementData.length) {
      ensureCapacity(this.elementCount+1);
    }
    this.elementData[this.elementCount++] = x;
  }

  public final long elementAt(int index) {
    return this.elementData[index];
  }

  public final void removeElement(int index) {
    this.elementData[index] = this.elementData[this.elementCount-1];
    this.elementCount--;
  }
  
  public final int size() { return this.elementCount; }

  public final void ensureCapacity(int minCapacity) { 
    if (elementData.length < minCapacity) {
      int newCapacity = elementData.length + elementData.length;
      if (newCapacity < minCapacity) {
	newCapacity = minCapacity;
      }
      long oldBuffer[] = this.elementData;
      this.elementData = new long[newCapacity];

      System.arraycopy(oldBuffer, 0, elementData, 0, elementCount);
    }
  }

  public final void reset() { this.elementCount = 0; }

  private void readObject(ObjectInputStream ois)
  throws IOException, ClassNotFoundException {
    this.elementCount = ois.readInt();
    this.elementData = new long[this.elementCount];
    for (int i = 0; i < this.elementCount; i++) {
      this.elementData[i] = ois.readLong();
    }
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.writeInt(this.elementCount);
    for (int i = 0; i < this.elementCount; i++) {
      oos.writeLong(this.elementData[i]);
    }
  }

  public final String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("<");
    if (this.elementCount != 0) {
      sb.append(this.elementData[0]);
    }
    for (int i = 1; i < this.elementCount; i++) {
      sb.append(", ");
      sb.append(this.elementData[i]);
    }
    sb.append(">");
    return sb.toString();
  }
  
  public static void main(String args[]) throws Exception {
    LongVec vec = new LongVec(1000);
    vec.addElement(1);
    vec.addElement(3);
    vec.addElement(5);
    RuntimeConfiguration.get().getErrStream().println(vec.size());
    FileOutputStream fos = new FileOutputStream("XXX");
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(vec);
    FileInputStream fis = new FileInputStream("XXX");
    ObjectInputStream ois = new ObjectInputStream(fis);
    LongVec vec1 = (LongVec)ois.readObject();
    RuntimeConfiguration.get().getErrStream().println(vec.size());
    RuntimeConfiguration.get().getErrStream().println(vec.elementAt(0));
    RuntimeConfiguration.get().getErrStream().println(vec.elementAt(1));
    RuntimeConfiguration.get().getErrStream().println(vec.elementAt(2));
  }

}
