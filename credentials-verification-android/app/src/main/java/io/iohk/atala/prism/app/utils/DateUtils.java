package io.iohk.atala.prism.app.utils;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.Calendar;

import io.iohk.atala.prism.protos.Date;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DateUtils {

  private final Context context;

  public String format(Calendar calendar) {
    java.text.DateFormat dateFormat = DateFormat.getDateFormat(context);
    return dateFormat.format(calendar.getTime());
  }

  public String format(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.set(date.getYear(), date.getMonth() - 1,
        date.getDay()); // Calendar's months range is 0 - 11
    return format(calendar);
  }

  public String format(Long time) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    return format(calendar);
  }
}
