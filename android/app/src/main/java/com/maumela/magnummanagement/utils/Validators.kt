package com.maumela.magnummanagement.utils

import java.math.BigDecimal

/**
 * Input validation on the device. Every function returns an error message, or null when the
 * value is valid. The rules mirror the backend (which re-checks everything), so users get
 * instant feedback and invalid forms never reach the network.
 */
object Validators {
    const val PASSWORD_MIN = 8
    const val PASSWORD_MAX = 64

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)*\\.[A-Za-z]{2,}\$")
    private val PHONE_REGEX = Regex("\\+?[0-9]{9,15}")
    private val PHONE_SEPARATORS = Regex("[\\s\\-()]")
    private val URL_REGEX = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
    private val MONEY_REGEX = Regex("[0-9]{1,10}(\\.[0-9]{1,2})?")

    // ---- Single fields
    fun fullName(value: String): String? {
        val text = value.trim()
        return when {
            text.isEmpty() -> "Enter your full name"
            text.length < 2 -> "Full name must be at least 2 characters"
            text.length > 100 -> "Full name must be at most 100 characters"
            else -> null
        }
    }

    fun email(value: String): String? {
        val text = value.trim()
        return when {
            text.isEmpty() -> "Enter your email address"
            text.length > 255 || !EMAIL_REGEX.matches(text) -> "Enter a valid email address"
            else -> null
        }
    }

    /** Optional unless [required]. Spaces, dashes and brackets are ignored; "+" is allowed at the start. */
    fun phone(value: String, required: Boolean = false): String? {
        val cleaned = value.replace(PHONE_SEPARATORS, "")
        return when {
            cleaned.isEmpty() -> if (required) "Enter your phone number" else null
            !PHONE_REGEX.matches(cleaned) -> "Phone number must have 9 to 15 digits and may start with +"
            else -> null
        }
    }

    /** Password rules for NEW passwords: 8-64 characters with at least one letter and one digit. */
    fun password(value: String): String? = when {
        value.isEmpty() -> "Enter a password"
        value.length < PASSWORD_MIN -> "Password must be at least $PASSWORD_MIN characters"
        value.length > PASSWORD_MAX -> "Password must be at most $PASSWORD_MAX characters"
        value.none { it.isLetter() } -> "Password must contain at least one letter"
        value.none { it.isDigit() } -> "Password must contain at least one digit"
        else -> null
    }

    fun confirmPassword(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Confirm your password"
        confirm != password -> "Passwords do not match"
        else -> null
    }

    // ---- Whole forms
    data class LoginErrors(val email: String? = null, val password: String? = null) {
        val hasErrors get() = email != null || password != null
    }

    /** Login only checks that fields are filled in: strength rules apply when CHOOSING a password. */
    fun login(email: String, password: String) = LoginErrors(
        email = email(email),
        password = if (password.isEmpty()) "Enter your password" else null,
    )

    data class RegisterErrors(
        val fullName: String? = null,
        val email: String? = null,
        val phone: String? = null,
        val password: String? = null,
        val confirmPassword: String? = null,
    ) {
        val hasErrors
            get() = listOf(fullName, email, phone, password, confirmPassword).any { it != null }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
    ) = RegisterErrors(
        fullName = fullName(fullName),
        email = email(email),
        phone = phone(phone),
        password = password(password),
        confirmPassword = confirmPassword(password, confirmPassword),
    )

    data class ProfileErrors(
        val fullName: String? = null,
        val email: String? = null,
        val phone: String? = null,
    ) {
        val hasErrors get() = fullName != null || email != null || phone != null
    }

    fun profile(fullName: String, email: String, phone: String) = ProfileErrors(
        fullName = fullName(fullName),
        email = email(email),
        phone = phone(phone),
    )

    data class PasswordChangeErrors(
        val current: String? = null,
        val new: String? = null,
        val confirm: String? = null,
    ) {
        val hasErrors get() = current != null || new != null || confirm != null
    }

    fun passwordChange(current: String, new: String, confirm: String) = PasswordChangeErrors(
        current = if (current.isEmpty()) "Enter your current password" else null,
        new = password(new)
            ?: if (new == current) "New password must be different from the current password" else null,
        confirm = confirmPassword(new, confirm),
    )

    // ---- Listings (product/service editor)
    fun listingName(value: String): String? {
        val text = value.trim()
        return when {
            text.isEmpty() -> "Enter a name"
            text.length < 2 -> "Name must be at least 2 characters"
            text.length > 120 -> "Name must be at most 120 characters"
            else -> null
        }
    }

    fun listingCategory(value: String): String? {
        val text = value.trim()
        return when {
            text.isEmpty() -> "Enter a category"
            text.length < 2 -> "Category must be at least 2 characters"
            text.length > 50 -> "Category must be at most 50 characters"
            else -> null
        }
    }

    /**
     * Parses a price typed by a person: digits with an optional decimal part of at most two
     * places. A single comma is accepted as the decimal separator ("10,50"). Returns null if invalid.
     */
    fun parsePrice(text: String, allowZero: Boolean = false): BigDecimal? {
        val trimmed = text.trim()
        val normalised =
            if ('.' !in trimmed && trimmed.count { it == ',' } == 1) trimmed.replace(',', '.') else trimmed
        if (!MONEY_REGEX.matches(normalised)) return null
        val value = BigDecimal(normalised)
        return if (value.signum() > 0 || (allowZero && value.signum() == 0)) value else null
    }

    fun price(text: String): String? = when {
        text.isBlank() -> "Enter a price"
        parsePrice(text) == null -> "Enter a price above 0 with at most 2 decimals"
        else -> null
    }

    fun deliveryDays(text: String): String? {
        val days = text.trim().toIntOrNull()
        return when {
            text.isBlank() -> "Enter the delivery time in days"
            days == null || days < 1 || days > 365 -> "Delivery time must be between 1 and 365 days"
            else -> null
        }
    }

    fun stockQuantity(text: String): String? {
        val stock = text.trim().toIntOrNull()
        return when {
            text.isBlank() -> "Enter the stock quantity"
            stock == null || stock < 0 || stock > 1_000_000 -> "Stock must be a whole number from 0 to 1 000 000"
            else -> null
        }
    }

    /** Optional. Must be an http:// or https:// link when given. */
    fun imageUrl(value: String): String? {
        val text = value.trim()
        return when {
            text.isEmpty() -> null
            text.length > 500 -> "Image link must be at most 500 characters"
            !URL_REGEX.matches(text) -> "Image link must start with http:// or https://"
            else -> null
        }
    }

    fun orderNotes(value: String): String? =
        if (value.trim().length > 1000) "Notes must be at most 1000 characters" else null
}