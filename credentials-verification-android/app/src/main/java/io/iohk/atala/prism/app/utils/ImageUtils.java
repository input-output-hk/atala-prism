package io.iohk.atala.prism.app.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageUtils {

  public static Bitmap getBitmapFromByteArray(byte[] bytes) {
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
  }
}
