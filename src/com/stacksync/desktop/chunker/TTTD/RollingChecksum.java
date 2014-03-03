/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.chunker.TTTD;

/**
 *
 * @author cotes
 */
/**
 * A simple 32-bit "rolling" checksum. This checksum algorithm is based
 * upon the algorithm outlined in the paper "The rsync algorithm" by
 * Andrew Tridgell and Paul Mackerras. The algorithm works in such a way
 * that if one knows the sum of a block
 * <em>X<sub>k</sub>...X<sub>l</sub></em>, then it is a simple matter to
 * compute the sum for <em>X<sub>k+1</sub>...X<sub>l+1</sub></em>.
 *
 * @author Casey Marshall
 * @version $Revision: 188 $
 */

public class RollingChecksum {
  protected final int char_offset;

  /**
   * The first half of the checksum.
   *
   * @since 1.1
   */
  protected long a;

  /**
   * The second half of the checksum.
   *
   * @since 1.1
   */
  protected long b;

  /**
   * The place from whence the current checksum has been computed.
   *
   * @since 1.1
   */
  protected int k;

  /**
   * The place to where the current checksum has been computed.
   *
   * @since 1.1
   */
  protected int l;

  /**
   * The block from which the checksum is computed.
   *
   * @since 1.1
   */
  protected byte[] block;

  /**
   * The index in {@link #new_block} where the newest byte has
   * been stored.
   *
   * @since 1.1
   */
  protected int index;
  
  private static double M = Math.pow(2, 16);
  private CircularByteQueue queue;

// Constructors.
  // -----------------------------------------------------------------

  /**
   * Creates a new rolling checksum. The <i>char_offset</i> argument
   * affects the output of this checksum; rsync uses a char offset of
   * 0, librsync 31.
   */
  public RollingChecksum(int char_offset)
  {
    this.char_offset = char_offset;
    a = b = 0;
    k = 0;
    block = new byte[48];
    index = 0;
    queue = new CircularByteQueue(48+1);
  }

  public RollingChecksum()
  {
    this(0);
  }

 // Public instance methods.
  // -----------------------------------------------------------------

  /**
   * Return the value of the currently computed checksum.
   *
   * @return The currently computed checksum.
   * @since 1.1
   */
  public long getValue()
  {
        return (a & 0xffff) | (b << 16);
  }

  /**
   * Reset the checksum.
   *
   * @since 1.1
   */
  public void reset()
  {
    k = 0;
    a = b = 0;
    l = 0;
    index = 0;
  }

  /**
   * "Roll" the checksum. This method takes a single byte as byte
   * <em>X<sub>l+1</sub></em>, and recomputes the checksum for
   * <em>X<sub>k+1</sub>...X<sub>l+1</sub></em>. This is the
   * preferred method for updating the checksum.
   *
   * @param bt The next byte.
   * @since 1.1
   */
  /*public void roll(byte bt)
  {
    a -= block[k] + char_offset;
    b -= l * (block[k] + char_offset);
    a += bt + char_offset;
    b += a;
    block[k] = bt;
    k++;
    if (k == l) k = 0;
  }*/
  
  
  public long calculcateChecksum(byte bt) {
      
      int blockSize = 48;
      
      if (index < 48) {
          queue.add(bt);
          block[index] = bt;
          index++;
          if (index != 48)
            return 0;
      }
      
      if (k == 0) { // s(l, k) = a(l, k) + M * b(l, k)
        //queue.add(bt);         
        byte[] chunk = new byte[Math.min(blockSize, block.length)];

        for (int i=0; i<chunk.length; i++)
                chunk[i] = block[i];

        for (int i=0; i<chunk.length; i++)
                a += chunk[i];
        a = (long) (a % M);

        for (int i=0; i<chunk.length; i++)
                b += (chunk.length-1 - i + 1) * chunk[i];
        b = (long) (b % M);
        k++;

        return a + (long) (M * b);

       }

        else {

            //int kAt = index-blockSize;
            //int lAt = index;

            byte l = bt;
            byte k = queue.poll();
            queue.add(bt);

            a = (long) ((a - k + l) % M);
            //b = (long) ((b - ((lAt-1) - kAt + 1)*k + a) % M);
            b = (long) ((b - (48)*k + a) % M);

            return a + (long) (M * b);
        }
      
  }

    @Override
  public Object clone()
  {
    try
      {
        return super.clone();
      }
    catch (CloneNotSupportedException cnse)
      {
        throw new Error();
      }
  }

    @Override
  public boolean equals(Object o)
  {
        return ((RollingChecksum)o).a == a && ((RollingChecksum)o).b == b;
  }
}