package io.iohk.cvp.utils;

import android.content.Context;
import android.text.format.DateFormat;

import com.crashlytics.android.Crashlytics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Optional;

import io.iohk.cvp.core.exception.AssetNotFoundException;
import io.iohk.cvp.core.exception.ErrorCode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DateUtils {

  private final Context context;

  public String format(Calendar calendar) {
    java.text.DateFormat dateFormat = DateFormat.getDateFormat(context);
    return dateFormat.format(calendar.getTime());
  }
}
