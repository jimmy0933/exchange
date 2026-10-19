import java.util.regex.Pattern

fun extractTimeFromOfficial(officialTimeStr: String?): String {
    if (officialTimeStr.isNullOrBlank()) {
        return "NOW"
    }
    val match = Regex("""\d{2}:\d{2}""").find(officialTimeStr)
    return match?.value ?: "NOW"
}

fun main() {
    println(extractTimeFromOfficial("2026/10/13 09:23:31"))
}
