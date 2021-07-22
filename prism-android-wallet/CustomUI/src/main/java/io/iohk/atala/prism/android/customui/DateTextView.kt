package io.iohk.atala.prism.android.customui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class DateTextView : AppCompatTextView {

    companion object {
        private const val SECOND_AGO_TEXT = "%1\$d second ago"
        private const val SECONDS_AGO_TEXT = "%1\$d seconds ago"
        private const val MINUTE_AGO_TEXT = "%1\$d minute ago"
        private const val MINUTES_AGO_TEXT = "%1\$d minutes ago"
        private const val HOUR_AGO_TEXT = "%1\$d hour ago"
        private const val HOURS_AGO_TEXT = "%1\$d hours ago"

        private const val MILLISECONDS_IN_A_SECOND: Long = 1000

        private const val MILLISECONDS_IN_A_MINUTE: Long = MILLISECONDS_IN_A_SECOND * 60

        private const val MILLISECONDS_IN_A_HOUR: Long = MILLISECONDS_IN_A_MINUTE * 60

        private const val MILLISECONDS_IN_A_DAY: Long = MILLISECONDS_IN_A_HOUR * 24

        @SuppressLint("SimpleDateFormat")
        private val DEFAULT_DATE_FORMAT = SimpleDateFormat("dd/MM/YYYY")

        private const val REFRESH_TIME_MILL = 1000L
    }

    private var date: Date? = null
    private var dateFormat: SimpleDateFormat = DEFAULT_DATE_FORMAT
    private var secondAgoText: String = SECOND_AGO_TEXT
    private var secondsAgoText: String = SECONDS_AGO_TEXT
    private var minuteAgoText: String = MINUTE_AGO_TEXT
    private var minutesAgoText: String = MINUTES_AGO_TEXT
    private var hourAgoText: String = HOUR_AGO_TEXT
    private var hoursAgoText: String = HOURS_AGO_TEXT
    private var dateFormatWrapperText: String? = null

    fun setDate(date: Date, dateFormat: SimpleDateFormat? = null) {
        this.date = date
        dateFormat?.let { this.dateFormat = it }
        refreshText()
    }

    fun setDate(time: Long, dateFormat: SimpleDateFormat? = null) = setDate(Date(time), dateFormat)

    constructor(context: Context) : super(context)

    constructor(context: Context, attr: AttributeSet) : super(context, attr) {
        initParams(attr)
    }

    constructor(context: Context, attr: AttributeSet, defStyle: Int) : super(context, attr, defStyle) {
        initParams(attr)
    }

    private fun initParams(attr: AttributeSet) {
        val styledAttributes = context.theme.obtainStyledAttributes(attr, R.styleable.DateTextView, R.attr.dateTextViewStyle, 0)
        secondAgoText = styledAttributes.getString(R.styleable.DateTextView_secondAgoText) ?: secondAgoText
        secondsAgoText = styledAttributes.getString(R.styleable.DateTextView_secondsAgoText) ?: secondsAgoText
        minuteAgoText = styledAttributes.getString(R.styleable.DateTextView_minuteAgoText) ?: minuteAgoText
        minutesAgoText = styledAttributes.getString(R.styleable.DateTextView_minutesAgoText) ?: minutesAgoText
        hourAgoText = styledAttributes.getString(R.styleable.DateTextView_hourAgoText) ?: hourAgoText
        hoursAgoText = styledAttributes.getString(R.styleable.DateTextView_hoursAgoText) ?: hoursAgoText
        dateFormatWrapperText = styledAttributes.getString(R.styleable.DateTextView_dateFormatWrapperText)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            startRefreshingText()
        } else {
            stopRefreshingText()
        }
    }

    private var refreshTextScope: CoroutineScope? = null

    private fun startRefreshingText() {
        refreshTextScope = CoroutineScope(Dispatchers.Main).apply {
            launch {
                while (true) {
                    refreshText()
                    delay(REFRESH_TIME_MILL)
                }
            }
        }
    }

    private fun stopRefreshingText() {
        if (refreshTextScope?.isActive == true) { refreshTextScope?.cancel(null) }
    }

    private fun refreshText() {
        date?.let {
            val timeElapsed = Date().time - it.time
            when {
                timeElapsed > MILLISECONDS_IN_A_DAY || timeElapsed <= 0 -> renderSimpleDateFormat()
                timeElapsed > MILLISECONDS_IN_A_HOUR -> renderHoursAgo(timeElapsed)
                timeElapsed > MILLISECONDS_IN_A_MINUTE -> renderMinutesAgo(timeElapsed)
                else -> renderSecondsAgo(timeElapsed)
            }
        }
    }

    private fun renderSimpleDateFormat() {
        date?.let {
            text = when {
                dateFormatWrapperText?.contains("%1\$d") == true -> dateFormatWrapperText?.replace("%1\$d", dateFormat.format(it))
                dateFormatWrapperText?.contains("%1\$s") == true -> dateFormatWrapperText?.replace("%1\$s", dateFormat.format(it))
                else -> dateFormat.format(it)
            }
        }
    }

    private fun renderHoursAgo(timeElapsed: Long) {
        val hours = timeElapsed / MILLISECONDS_IN_A_HOUR
        text = if (hours == 1L) {
            hourAgoText.replace("%1\$d", "$hours")
        } else {
            hoursAgoText.replace("%1\$d", "$hours")
        }
    }

    private fun renderMinutesAgo(timeElapsed: Long) {
        val minutes = timeElapsed / MILLISECONDS_IN_A_MINUTE
        text = if (minutes == 1L) {
            minuteAgoText.replace("%1\$d", "$minutes")
        } else {
            minutesAgoText.replace("%1\$d", "$minutes")
        }
    }

    private fun renderSecondsAgo(timeElapsed: Long) {
        val seconds = timeElapsed / MILLISECONDS_IN_A_SECOND
        text = if (seconds == 1L) {
            secondAgoText.replace("%1\$d", "$seconds")
        } else {
            secondsAgoText.replace("%1\$d", "$seconds")
        }
    }
}

@BindingAdapter("date")
fun setDate(textView: DateTextView, date: Date?) {
    date?.let {
        textView.setDate(it)
    }
}
