package io.iohk.cvp.utils;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.Calendar;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DateUtils {

  private final Context context;

  public String format(Calendar calendar) {
    java.text.DateFormat dateFormat = DateFormat.getDateFormat(context);
    return dateFormat.format(calendar.getTime());
  }
}
