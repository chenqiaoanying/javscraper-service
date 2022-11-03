package com.github.javscraper.service.model

import com.github.javscraper.utils.getAllPossibleKeywords
import kotlin.text.RegexOption.IGNORE_CASE

enum class Matcher(
    private val type: JavIdType,
    private vararg val regexes: Regex
) {
    Carib(
        JavIdType.Uncensored,
        """(?<id>\d{4,8}-\d{1,6})-(1pon|carib|paco|mura)""".toRegex(IGNORE_CASE),
        """(1Pondo|Caribbean|Pacopacomama|muramura)-(?<id>\d{4,8}-\d{1,8})($|\D)""".toRegex(IGNORE_CASE)
    ),
    Heyzo(
        JavIdType.Uncensored,
        """Heyzo(|-| |.com)(HD-|)(?<id>\d{2,8})($|\D)""".toRegex(IGNORE_CASE)
    ) {
        override fun numberFrom(matchResult: MatchResult): String {
            return "heyzo-${idFrom(matchResult)}"
        }
    },
    FC2(
        JavIdType.Suren,
        """FC2-*(PPV|)\D{1,3}(?<id>\d{2,10})($|\D)""".toRegex(IGNORE_CASE)
    ) {
        override fun numberFrom(matchResult: MatchResult): String {
            return "fc2-${idFrom(matchResult)}"
        }
    },
    Musume(
        JavIdType.Suren,
        """(?<id>\d{4,8}-\d{1,6})-(10mu)""".toRegex(IGNORE_CASE),
        """(10Musume)-(?<id>\d{4,8}-\d{1,6})""".toRegex(IGNORE_CASE)
    ),
    Censored(
        JavIdType.Censored,
        """(^|[^a-z0-9])(?<id>[a-z]{3,4}-\d{3})($|\D)""".toRegex(IGNORE_CASE) // SSIS-001, ABW-100, PPPD-326
    ),
    Suren(
        JavIdType.Suren,
        """(^|[^a-z0-9])(?<id>\d{3}[a-z]{3,4}-\d{3})($|\D)""".toRegex(IGNORE_CASE) // 390JAC-132, 300MAAN-783
    ),
    Uncensored(
        JavIdType.Uncensored,
        """(^|[^a-z0-9])(?<id>\d{6}-\d{1,2})($|\D)""".toRegex(IGNORE_CASE), // 123456_999, 587234-01
        """(^|[^a-z0-9])(?<id>[a-z]{8}-\d{4}-\d{3})($|\D)""".toRegex(IGNORE_CASE), // heydouga-1234-321
        """(^|[^a-z0-9])(?<id>[a-z]\d{4}-[a-z]{2}([a-z]|[0-9])\d{5})($|\D)""".toRegex(IGNORE_CASE) // c0930-ki897634, h4610-ori98321
    ),
    General(
        JavIdType.Unknown,
        """(^|[^a-z0-9])(?<id>[a-z0-9]{2,10}-\d{2,8})($|\D)""".toRegex(IGNORE_CASE), // 标准 AAA-111
        """(^|[^a-z0-9])(?<id>[a-z]{2,10}-[a-z]{1,5}\d{2,8})($|\D)""".toRegex(IGNORE_CASE), // 第二段带字母 AAA-B11
        """(^|[^a-z0-9])(?<id>[a-z]{1,10}\d{2,8})($|\D)""".toRegex(IGNORE_CASE), // 没有横杠的 AAA111
        """(?<id>\d{6,8}-\d{1,6})""".toRegex(IGNORE_CASE)
    );

    protected open fun idFrom(matchResult: MatchResult) = matchResult.groups["id"]!!.value.lowercase()

    protected open fun numberFrom(matchResult: MatchResult) = idFrom(matchResult).lowercase()

    fun match(name: String): JavId? =
        regexes.firstNotNullOfOrNull { regex ->
            regex.find(name)?.let { JavId(number = numberFrom(it), id = idFrom(it), type = type, matcher = this) }
        }

    companion object {
        fun match(name: String): JavId? {
            values().forEach {
                val result = it.match(name)
                if (result != null) {
                    return result
                }
            }
            return null
        }
    }
}

data class JavId(
    val number: String,
    val id: String,
    val type: JavIdType,
    val matcher: Matcher
) {
    val allPossibleNumbers by lazy {
        getAllPossibleKeywords(number)
    }

    companion object {

        private val spliterator_regex = """_|\s|\.""".toRegex()

        // 移除视频编码 1080p,720p 2k 之类的
        private val video_code_regex = """(?<=^|\D)\d{3,5}p|\d{1,2}k(?=$|[^a-z])""".toRegex(IGNORE_CASE)
        private val surrounding_regex = """ts6\d+|-*whole\d*|-*full$|-c$|-ch$|-hd(?=$|-)|^h-|^hd-|-uncensored|tokyo-hot|(?<=^|\D)\d{2,4}-\d{1,2}-\d{1,2}(?=$|[^a-z])""".toRegex(IGNORE_CASE)

        fun from(name: String): JavId? {
            var trimmedName = name
                .replace(spliterator_regex, "-")
                .replace(video_code_regex, "")

            var length = Int.MAX_VALUE
            while (length != trimmedName.count()) {
                trimmedName = trimmedName.replace(surrounding_regex, "")
                length = trimmedName.count()
            }

            return Matcher.match(trimmedName)
        }
    }
}