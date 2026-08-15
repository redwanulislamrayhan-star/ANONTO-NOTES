package com.example.util

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.util.Locale

data class TagCategory(
    val nameEn: String,
    val nameBn: String,
    val iconEmoji: String,
    val color: Color,
    val isIncomeCategory: Boolean = false
)

data class ParsedLine(
    val originalText: String,
    val extractedAmount: Double,
    val detectedTags: List<TagCategory>
)

data class LedgerParseResult(
    val totalSum: Double,
    val lines: List<ParsedLine>,
    val allTags: List<TagCategory>,
    val itemSummaries: List<String>
)

object LedgerParser {

    // Expense Categories
    val CATEGORY_GROCERY = TagCategory("Grocery", "বাজার", "🛒", Color(0xFFF59E0B)) // Amber
    val CATEGORY_HEALTH = TagCategory("Health", "চিকিৎসা", "🩺", Color(0xFFEF4444)) // Red
    val CATEGORY_UTILITY = TagCategory("Utility", "ইউটিলিটি", "⚡", Color(0xFF3B82F6)) // Blue
    val CATEGORY_FAMILY = TagCategory("Family", "পরিবার", "💜", Color(0xFF8B5CF6)) // Purple
    val CATEGORY_TRANSPORT = TagCategory("Transport", "যাতায়াত", "🚕", Color(0xFF06B6D4)) // Cyan
    val CATEGORY_EDUCATION = TagCategory("Education", "শিক্ষা", "📚", Color(0xFF10B981)) // Mint Green
    val CATEGORY_FOOD = TagCategory("Food & Snacks", "খাবার", "🍔", Color(0xFFF97316)) // Orange
    val CATEGORY_OTHER_EXPENSE = TagCategory("Other Expense", "অন্যান্য খরচ", "🏷️", Color(0xFF6B7280)) // Gray

    // Income Categories
    val CATEGORY_SALARY = TagCategory("Salary", "বেতন", "💼", Color(0xFF10B981), isIncomeCategory = true) // Mint Green
    val CATEGORY_FREELANCE = TagCategory("Freelancing", "ফ্রিল্যান্সিং", "💻", Color(0xFF0EA5E9), isIncomeCategory = true) // Sky Blue
    val CATEGORY_RENT_INCOME = TagCategory("House Rent Income", "ভাড়া জমা", "🏠", Color(0xFF8B5CF6), isIncomeCategory = true) // Purple
    val CATEGORY_BUSINESS_INCOME = TagCategory("Business Income", "ব্যবসা আয়", "📈", Color(0xFFF59E0B), isIncomeCategory = true) // Amber
    val CATEGORY_BONUS = TagCategory("Bonus & Gift", "বোনাস/উপহার", "🎁", Color(0xFFEC4899), isIncomeCategory = true) // Pink
    val CATEGORY_OTHER_INCOME = TagCategory("Other Income", "অন্যান্য আয়", "💵", Color(0xFF059669), isIncomeCategory = true) // Dark Green

    val ALL_EXPENSE_CATEGORIES = listOf(
        CATEGORY_GROCERY,
        CATEGORY_HEALTH,
        CATEGORY_UTILITY,
        CATEGORY_FAMILY,
        CATEGORY_TRANSPORT,
        CATEGORY_EDUCATION,
        CATEGORY_FOOD,
        CATEGORY_OTHER_EXPENSE
    )

    val ALL_INCOME_CATEGORIES = listOf(
        CATEGORY_SALARY,
        CATEGORY_FREELANCE,
        CATEGORY_RENT_INCOME,
        CATEGORY_BUSINESS_INCOME,
        CATEGORY_BONUS,
        CATEGORY_OTHER_INCOME
    )

    val ALL_CATEGORIES = ALL_EXPENSE_CATEGORIES + ALL_INCOME_CATEGORIES

    private val BENGALI_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    /**
     * Converts Bengali digits in text to standard ASCII English digits.
     */
    fun convertBengaliDigitsToEnglish(input: String): String {
        var result = input
        for (i in 0..9) {
            result = result.replace(BENGALI_DIGITS[i], ('0' + i))
        }
        return result
    }

    /**
     * Converts English ASCII digits to Bengali digits.
     */
    fun convertEnglishDigitsToBengali(input: String): String {
        var result = input
        for (i in 0..9) {
            result = result.replace(('0' + i), BENGALI_DIGITS[i])
        }
        return result
    }

    /**
     * Formats currency with commas and proper symbol/numerals.
     */
    fun formatCurrency(
        amount: Double,
        currencySymbol: String = "৳",
        useBengaliNumerals: Boolean = true
    ): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = 2
        val formattedNumber = formatter.format(amount)

        val numberString = if (useBengaliNumerals) {
            convertEnglishDigitsToBengali(formattedNumber)
        } else {
            formattedNumber
        }

        return "$currencySymbol $numberString"
    }

    /**
     * Generates a deterministic pleasant Compose Color for any dynamic custom tag string based on hash code.
     */
    fun getHashColor(word: String): Color {
        val colors = listOf(
            Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF8B5CF6),
            Color(0xFF06B6D4), Color(0xFF10B981), Color(0xFFF97316), Color(0xFFEC4899),
            Color(0xFF0EA5E9), Color(0xFF84CC16), Color(0xFFD97706), Color(0xFF6366F1),
            Color(0xFF14B8A6), Color(0xFFF43F5E), Color(0xFF8B5CF6), Color(0xFF0284C7)
        )
        val index = kotlin.math.abs(word.hashCode()) % colors.size
        return colors[index]
    }

    /**
     * Smartly infers a fitting emoji for any dynamic Bengali or English word tag.
     */
    fun getEmojiForWord(word: String): String {
        val lower = word.lowercase()
        return when {
            containsAny(lower, "চকলেট", "মিষ্টি", "আইসক্রিম", "chocolate", "sweet", "icecream") -> "🍫"
            containsAny(lower, "জামা", "কাপড়", "শার্ট", "প্যান্ট", "জুতো", "dress", "clothes", "shoes", "shopping") -> "🛍️"
            containsAny(lower, "চা", "কফি", "নাস্তা", "tea", "coffee", "snack") -> "☕"
            containsAny(lower, "মোবাইল", "রিচার্জ", "ফ্লেক্সি", "নেট", "sim", "mobile", "recharge") -> "📱"
            containsAny(lower, "সিনেমা", "ছবি", "মুভি", "হল", "movie", "cinema") -> "🎬"
            containsAny(lower, "পার্লার", "সেলুন", "চুল", "saloon", "parlor", "beauty") -> "💇"
            containsAny(lower, "পেট্রোল", "অকটেন", "তেল", "গাড়ি", "fuel", "car", "bike") -> "⛽"
            containsAny(lower, "উপহার", "গিফট", "gift", "present") -> "🎁"
            containsAny(lower, "বই", "খাতা", "কলম", "book", "pen", "paper") -> "📚"
            containsAny(lower, "মেডিসিন", "ওষুধ", "ডাক্তার", "medicine", "doctor") -> "🩺"
            containsAny(lower, "বাসা", "ভাড়া", "rent", "house") -> "🏠"
            containsAny(lower, "পিৎজা", "বার্গার", "চিকেন", "pizza", "burger") -> "🍕"
            containsAny(lower, "ব্যাংক", "লোন", "ডিপিএস", "bank", "dps", "loan") -> "🏦"
            else -> "📌"
        }
    }

    /**
     * Parses a tag string like "NameEn|NameBn" or "Name" into a TagCategory.
     */
    fun parseTagFromString(tagStr: String): TagCategory {
        val parts = tagStr.split("|")
        val enName = parts.firstOrNull()?.trim() ?: "Tag"
        val bnName = if (parts.size > 1) parts[1].trim() else enName

        val known = ALL_CATEGORIES.find {
            it.nameEn.equals(enName, ignoreCase = true) || it.nameBn.equals(bnName, ignoreCase = true)
        }
        if (known != null) return known

        return TagCategory(
            nameEn = enName,
            nameBn = bnName,
            iconEmoji = getEmojiForWord(bnName),
            color = getHashColor(bnName)
        )
    }

    /**
     * Detects built-in categories AND extracts dynamic unlimited word tags.
     */
    fun detectTags(lineText: String, isIncomeMode: Boolean = false): List<TagCategory> {
        val lower = lineText.lowercase()
        val tags = mutableSetOf<TagCategory>()

        if (isIncomeMode) {
            if (containsAny(lower, "বেতন", "স্যালারি", "salary", "job", "চাকরি")) tags.add(CATEGORY_SALARY)
            if (containsAny(lower, "ফ্রিল্যান্সিং", "প্রজেক্ট", "freelance", "upwork", "fiverr", "client", "ক্লায়েন্ট")) tags.add(CATEGORY_FREELANCE)
            if (containsAny(lower, "ভাড়া জমা", "বাসা ভাড়া জমা", "দোকান ভাড়া", "rent income", "rent")) tags.add(CATEGORY_RENT_INCOME)
            if (containsAny(lower, "ব্যবসা", "সেলস", "লাভ", "প্রফিট", "business", "profit", "sales")) tags.add(CATEGORY_BUSINESS_INCOME)
            if (containsAny(lower, "বোনাস", "উপহার", "ঈদি", "গিফট", "bonus", "gift", "eidi")) tags.add(CATEGORY_BONUS)
        } else {
            // Expense keywords
            if (containsAny(lower, "চাল", "মাছ", "গোশত", "মাংস", "বাজার", "শাক", "সবজি", "মুরগি", "ডিম", "তেল", "মসলা", "ফল", "rice", "fish", "meat", "grocery", "oil", "bazar", "bazaar", "vegetable")) tags.add(CATEGORY_GROCERY)
            if (containsAny(lower, "ডাক্তার", "ওষুধ", "ঔষধ", "মেডিসিন", "ফার্মেসি", "রোগী", "ডাক্তার ফি", "হাসপাতাল", "টেস্ট", "pharmacy", "doctor", "medicine", "hospital", "health", "pharma")) tags.add(CATEGORY_HEALTH)
            if (containsAny(lower, "বিদ্যুৎ", "গ্যাস", "পানি", "ইন্টারনেট", "ওয়াইফাই", "বিল", "কারেন্ট", "ভাড়া", "বাসা ভাড়া", "rent", "bill", "utility", "gas", "electricity", "wifi", "current")) tags.add(CATEGORY_UTILITY)
            if (containsAny(lower, "আম্মু", "আব্দুল", "আম্মা", "আব্বু", "বাবা", "মা", "ইভা", "রিমশা", "বাচ্চা", "পরিবার", "উপহার", "গিফট", "gift", "family", "mother", "father", "parents", "baby")) tags.add(CATEGORY_FAMILY)
            if (containsAny(lower, "রিকশা", "বাস", "উবার", "পাঠাও", "সিএনজি", "পেট্রোল", "অকটেন", "ডিজেল", "টোল", "transport", "bus", "uber", "fuel", "fare", "rickshaw", "cng")) tags.add(CATEGORY_TRANSPORT)
            if (containsAny(lower, "বই", "স্কুল", "কলেজ", "ভার্সিটি", "টিউশন", "খাতা", "কলম", "ফি", "tuition", "school", "books", "fee", "pen", "paper")) tags.add(CATEGORY_EDUCATION)
            if (containsAny(lower, "হোটেল", "রেস্তোরাঁ", "চা", "নাস্তা", "কফি", "খাবার", "লাঞ্চ", "ডিনার", "বিরিয়ানি", "ফুড", "food", "snack", "tea", "coffee", "lunch", "dinner", "restaurant")) tags.add(CATEGORY_FOOD)
        }

        // Dynamic Custom Word Extraction (Unlimited dynamic tags)
        val cleanText = convertBengaliDigitsToEnglish(lineText)
            .replace(Regex("[0-9]+\\.[0-9]+|[0-9]+"), " ")
            .replace(Regex("[+\\-*/=÷×x,]"), " ")
            .replace("হাজার", " ")
            .replace("000", " ")
            .replace("কে", " ")
            .replace("k", " ")
            .replace("K", " ")

        val words = cleanText.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { word ->
                word.length >= 2 &&
                !word.lowercase().matches(Regex("^[0-9]+$")) &&
                word.lowercase() !in listOf("টাকা", "tk", "bdt", "জন", "টি", "টা", "টি", "kg", "কেজি")
            }

        for (w in words) {
            val alreadyMatched = tags.any {
                it.nameBn.equals(w, ignoreCase = true) || it.nameEn.equals(w, ignoreCase = true)
            }
            if (!alreadyMatched) {
                tags.add(
                    TagCategory(
                        nameEn = w,
                        nameBn = w,
                        iconEmoji = getEmojiForWord(w),
                        color = getHashColor(w),
                        isIncomeCategory = isIncomeMode
                    )
                )
            }
        }

        if (tags.isEmpty()) {
            tags.add(if (isIncomeMode) CATEGORY_OTHER_INCOME else CATEGORY_OTHER_EXPENSE)
        }

        return tags.toList()
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it) }
    }

    /**
     * Smartly parses a full multi-line note string.
     * Evaluates math expressions like "চাল ২০০০ + মাছ ১৫০০ + ওষুধ ৮০০"
     * Handles + - * / and "k"/"হাজার" multipliers.
     */
    fun parseNote(rawNote: String, isIncomeMode: Boolean = false): LedgerParseResult {
        if (rawNote.isBlank()) {
            return LedgerParseResult(
                totalSum = 0.0,
                lines = emptyList(),
                allTags = emptyList(),
                itemSummaries = emptyList()
            )
        }

        val rawLines = rawNote.split("\n")
        val parsedLines = mutableListOf<ParsedLine>()
        val itemSummaries = mutableListOf<String>()
        val allTagsSet = mutableSetOf<TagCategory>()
        var grandTotal = 0.0

        for (line in rawLines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isBlank()) continue

            val normalizedLine = convertBengaliDigitsToEnglish(trimmedLine)
            val lineSum = calculateLineMath(normalizedLine)
            val tags = detectTags(trimmedLine, isIncomeMode)

            allTagsSet.addAll(tags)
            grandTotal += lineSum

            parsedLines.add(
                ParsedLine(
                    originalText = trimmedLine,
                    extractedAmount = lineSum,
                    detectedTags = tags
                )
            )

            if (lineSum > 0) {
                itemSummaries.add("$trimmedLine -> ৳ ${lineSum.toInt()}")
            }
        }

        return LedgerParseResult(
            totalSum = grandTotal,
            lines = parsedLines,
            allTags = allTagsSet.toList(),
            itemSummaries = itemSummaries
        )
    }

    /**
     * Calculates the numerical value of a single line.
     * Handles "চাল ২০০০ + মাছ ১৫০০ + ওষুধ ৮০০", "৫k", "২ হাজার".
     */
    fun calculateLineMath(normalizedLine: String): Double {
        var processed = normalizedLine
            .replace("হাজার", "000")
            .replace("কে", "000")
            .replace("k", "000")
            .replace("K", "000")
            .replace("×", "*")
            .replace("x", "*")
            .replace("÷", "/")

        val tokenRegex = Regex("([0-9]+(?:\\.[0-9]+)?|[+\\-*/])")
        val tokens = tokenRegex.findAll(processed).map { it.value }.toList()

        if (tokens.isEmpty()) return 0.0

        var currentTotal = 0.0
        var currentOp = '+'
        var hasValidNumber = false

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            val num = token.toDoubleOrNull()
            if (num != null) {
                hasValidNumber = true
                when (currentOp) {
                    '+' -> currentTotal += num
                    '-' -> currentTotal -= num
                    '*' -> currentTotal *= num
                    '/' -> if (num != 0.0) currentTotal /= num
                }
                currentOp = '+'
            } else if (token in listOf("+", "-", "*", "/")) {
                currentOp = token[0]
            }
            i++
        }

        return if (hasValidNumber) currentTotal else 0.0
    }

    val BENGALI_MONTHS = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    val ENGLISH_MONTHS = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun getMonthName(monthIndex1Based: Int, useBengali: Boolean = true): String {
        val idx = (monthIndex1Based - 1).coerceIn(0, 11)
        return if (useBengali) BENGALI_MONTHS[idx] else ENGLISH_MONTHS[idx]
    }
}
