package dita.dev.data

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.util.*

open class DateRange(val start: Date, val end: Date)

data class RemoteConfig(
    @SerializedName("parameters") val parameters: Parameters,
    @SerializedName("version") val version: Version
)

data class DefaultValue(
    @SerializedName("value") var value: String
)

data class Version(
    @SerializedName("versionNumber") val versionNumber: Int,
    @SerializedName("updateTime") val updateTime: String,
    @SerializedName("updateOrigin") val updateOrigin: String,
    @SerializedName("updateType") val updateType: String
)

data class Parameters(
    @SerializedName("feedback_email") val feedbackEmail: FeedbackEmail,
    @SerializedName("myportal_web_url") val webUrl: MyPortalWebUrl,
    @SerializedName("exam_timetable_available") val examTimetableAvailable: ExamTimetableAvailable,
    @SerializedName("exam_period") val examPeriod: _ExamPeriod,
    @SerializedName("current_calendar") val currentCalendar: CurrentCalendar,
)

data class FeedbackEmail(
    @SerializedName("defaultValue") val defaultValue: DefaultValue
)

data class MyPortalWebUrl(
    @SerializedName("defaultValue") val defaultValue: DefaultValue
)

data class ExamTimetableAvailable(
    @SerializedName("defaultValue") val defaultValue: DefaultValue
)

data class _ExamPeriod(
    @SerializedName("defaultValue") val defaultValue: DefaultValue,
    @SerializedName("description") val description: String
)

data class CurrentCalendar(
    @SerializedName("defaultValue") val defaultValue: DefaultValue,
    @SerializedName("description") val description: String
)

data class ExamPeriod(
    @SerializedName("start_date") val startDate: Date,
    @SerializedName("end_date") val endDate: Date
) : DateRange(startDate, endDate)

fun RemoteConfig.getExamPeriod(): ExamPeriod {
    val gson = GsonBuilder().setDateFormat("dd/MM/yy").create()
    val value = parameters.examPeriod.defaultValue.value
    return gson.fromJson(value, ExamPeriod::class.java)
}

fun RemoteConfig.disableExamTimetableAvailability() {
    parameters.examTimetableAvailable.defaultValue.value = "false"
}

fun RemoteConfig.enableExamTimetableAvailability() {
    parameters.examTimetableAvailable.defaultValue.value = "true"
}

fun RemoteConfig.setCurrentCalendar(calendar: String) {
    parameters.currentCalendar.defaultValue.value = calendar
}

data class Calendar(val name: String?, val period: DateRange?)


fun Calendar.isEmpty(): Boolean = period == null

data class Exam(val name: String, val room: String, val date: Long, val shift: String)

data class User(val uid: String, val email: String)