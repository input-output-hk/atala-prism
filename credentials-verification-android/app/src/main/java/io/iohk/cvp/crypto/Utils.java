package io.iohk.cvp.crypto;

import android.util.Log;
import com.google.common.base.Joiner;
import com.google.common.collect.Ordering;
import com.google.common.io.BaseEncoding;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class Utils {

  /**
   * Joiner for concatenating words with a space inbetween.
   */
  public static final Joiner SPACE_JOINER = Joiner.on(" ");

  /**
   * Hex encoding used throughout the framework. Use with HEX.encode(byte[]) or
   * HEX.decode(CharSequence).
   */
  public static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

  /**
   * Parse 4 bytes from the byte array (starting at the offset) as unsigned 32-bit integer in big
   * endian format.
   */
  public static long readUint32BE(byte[] bytes, int offset) {
    return ((bytes[offset] & 0xffL) << 24) |
        ((bytes[offset + 1] & 0xffL) << 16) |
        ((bytes[offset + 2] & 0xffL) << 8) |
        (bytes[offset + 3] & 0xffL);
  }

  /**
   * Returns a copy of the given byte array in reverse order.
   */
  public static byte[] reverseBytes(byte[] bytes) {
    // We could use the XOR trick here but it's easier to understand if we don't. If we find this is really a
    // performance issue the matter can be revisited.
    byte[] buf = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++) {
      buf[i] = bytes[bytes.length - 1 - i];
    }
    return buf;
  }

  /**
   * MPI encoded numbers are produced by the OpenSSL BN_bn2mpi function. They consist of a 4 byte
   * big endian length field, followed by the stated number of bytes representing the number in big
   * endian format (with a sign bit).
   *
   * @param hasLength can be set to false if the given array is missing the 4 byte length field
   */
  public static BigInteger decodeMPI(byte[] mpi, boolean hasLength) {
    byte[] buf;
    if (hasLength) {
      int length = (int) readUint32BE(mpi, 0);
      buf = new byte[length];
      System.arraycopy(mpi, 4, buf, 0, length);
    } else {
      buf = mpi;
    }
    if (buf.length == 0) {
      return BigInteger.ZERO;
    }
    boolean isNegative = (buf[0] & 0x80) == 0x80;
    if (isNegative) {
      buf[0] &= 0x7f;
    }
    BigInteger result = new BigInteger(buf);
    return isNegative ? result.negate() : result;
  }

  /**
   * If non-null, overrides the return value of now().
   */
  public static volatile Date mockTime;

  /**
   * Advances (or rewinds) the mock clock by the given number of milliseconds.
   */
  public static Date rollMockClockMillis(long millis) {
    if (mockTime == null) {
      throw new IllegalStateException("You need to use setMockClock() first.");
    }
    mockTime = new Date(mockTime.getTime() + millis);
    return mockTime;
  }


  private static class Pair implements Comparable<Pair> {

    int item, count;

    public Pair(int item, int count) {
      this.count = count;
      this.item = item;
    }

    // note that in this implementation compareTo() is not consistent with equals()
    @Override
    public int compareTo(Pair o) {
      return -Integer.compare(count, o.count);
    }
  }

  public static int maxOfMostFreq(int... items) {
    ArrayList<Integer> list = new ArrayList<>(items.length);
    for (int item : items) {
      list.add(item);
    }
    return maxOfMostFreq(list);
  }

  public static int maxOfMostFreq(List<Integer> items) {
    if (items.isEmpty()) {
      return 0;
    }
    // This would be much easier in a functional language (or in Java 8).
    items = Ordering.natural().reverse().sortedCopy(items);
    LinkedList<Pair> pairs = new LinkedList<>();
    pairs.add(new Pair(items.get(0), 0));
    for (int item : items) {
      Pair pair = pairs.getLast();
      if (pair.item != item) {
        pairs.add((pair = new Pair(item, 0)));
      }
      pair.count++;
    }
    // pairs now contains a uniqified list of the sorted inputs, with counts for how often that item appeared.
    // Now sort by how frequently they occur, and pick the max of the most frequent.
    Collections.sort(pairs);
    int maxCount = pairs.getFirst().count;
    int maxItem = pairs.getFirst().item;
    for (Pair pair : pairs) {
      if (pair.count != maxCount) {
        break;
      }
      maxItem = Math.max(maxItem, pair.item);
    }
    return maxItem;
  }

  private enum Runtime {
    ANDROID, OPENJDK, ORACLE_JAVA
  }

  private enum OS {
    LINUX, WINDOWS, MAC_OS
  }

  private static Runtime runtime = null;
  private static OS os = null;

  static {
    String runtimeProp = System.getProperty("java.runtime.name", "").toLowerCase(Locale.US);
    if (runtimeProp.equals("")) {
      runtime = null;
    } else if (runtimeProp.contains("android")) {
      runtime = Runtime.ANDROID;
    } else if (runtimeProp.contains("openjdk")) {
      runtime = Runtime.OPENJDK;
    } else if (runtimeProp.contains("java(tm) se")) {
      runtime = Runtime.ORACLE_JAVA;
    } else {
      Log.e("Unknown java.runtime.name '{}'", runtimeProp);
    }

    String osProp = System.getProperty("os.name", "").toLowerCase(Locale.US);
    if (osProp.equals("")) {
      os = null;
    } else if (osProp.contains("linux")) {
      os = OS.LINUX;
    } else if (osProp.contains("win")) {
      os = OS.WINDOWS;
    } else if (osProp.contains("mac")) {
      os = OS.MAC_OS;
    } else {
      Log.e("Unknown os.name '{}'", runtimeProp);
    }
  }

  public static boolean isAndroidRuntime() {
    return runtime == Runtime.ANDROID;
  }

}
