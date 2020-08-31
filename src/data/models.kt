package dita.dev.data

import com.google.gson.annotations.SerializedName

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
    @SerializedName("exam_period") val examPeriod: ExamPeriod,
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

data class ExamPeriod(
    @SerializedName("defaultValue") val defaultValue: DefaultValue,
    @SerializedName("description") val description: String
)

data class CurrentCalendar(
    @SerializedName("defaultValue") val defaultValue: DefaultValue,
    @SerializedName("description") val description: String
)