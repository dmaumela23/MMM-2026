package com.maumela.magnummanagement

import com.maumela.magnummanagement.utils.Validators
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    // Verifies full-name length limits (after trimming spaces).
    @Test
    fun fullName_enforcesLength() {
        assertNotNull(Validators.fullName(""))
        assertNotNull(Validators.fullName("   "))
        assertNotNull(Validators.fullName("A"))
        assertNull(Validators.fullName("  Al  "))
        assertNotNull(Validators.fullName("x".repeat(101)))
        assertNull(Validators.fullName("x".repeat(100)))
    }

    // Verifies well-formed emails are accepted (including plus-addressing and subdomains).
    @Test
    fun email_acceptsValidAddresses() {
        for (email in listOf("a@b.co", "test.user@example.com", "first+tag@mail.example.co.za", " padded@example.com ")) {
            assertNull(email, Validators.email(email))
        }
    }

    // Verifies malformed or empty emails are rejected with a message.
    @Test
    fun email_rejectsInvalidAddresses() {
        assertEquals("Enter your email address", Validators.email(""))
        for (email in listOf("plain", "a@", "@b.com", "a@@b.com", "a b@c.com", "a@b", "a@b.c", "a@.com")) {
            assertEquals(email, "Enter a valid email address", Validators.email(email))
        }
    }

    // Verifies the phone is optional by default and may be required.
    @Test
    fun phone_isOptionalUnlessRequired() {
        assertNull(Validators.phone(""))
        assertNull(Validators.phone("   "))
        assertEquals("Enter your phone number", Validators.phone("", required = true))
    }

    // Verifies phone formats: separators ignored, optional +, 9 to 15 digits.
    @Test
    fun phone_acceptsAndRejectsFormats() {
        assertNull(Validators.phone("082 123 4567"))
        assertNull(Validators.phone("+27 (82) 123-4567"))
        assertNull(Validators.phone("0" + "1".repeat(14)))
        assertNotNull(Validators.phone("abc"))
        assertNotNull(Validators.phone("12345"))
        assertNotNull(Validators.phone("1".repeat(16)))
        assertNotNull(Validators.phone("0821234567x"))
    }

    // Verifies each password rule produces its own helpful message.
    @Test
    fun password_ruleMessages() {
        assertEquals("Enter a password", Validators.password(""))
        assertTrue(Validators.password("short1")!!.contains("at least 8"))
        assertTrue(Validators.password("onlyletters")!!.contains("digit"))
        assertTrue(Validators.password("12345678")!!.contains("letter"))
        assertTrue(Validators.password("a1".repeat(33))!!.contains("at most 64"))
    }

    // Verifies the length boundaries of the password rule (8 and 64 characters).
    @Test
    fun password_boundaries() {
        assertNull(Validators.password("abcdefg1"))
        assertNotNull(Validators.password("abcdef1"))
        assertNull(Validators.password("a".repeat(63) + "1"))
        assertNotNull(Validators.password("a".repeat(64) + "1"))
        assertNull(Validators.password("Passw0rd123"))
    }

    // Verifies password confirmation.
    @Test
    fun confirmPassword_mustMatch() {
        assertEquals("Confirm your password", Validators.confirmPassword("Passw0rd1", ""))
        assertEquals("Passwords do not match", Validators.confirmPassword("Passw0rd1", "Passw0rd2"))
        assertNull(Validators.confirmPassword("Passw0rd1", "Passw0rd1"))
    }

    // Verifies login blocks empty fields, but does not apply new-password strength rules.
    @Test
    fun login_checksOnlyThatFieldsAreFilled() {
        assertTrue(Validators.login("", "").hasErrors)
        assertEquals("Enter your password", Validators.login("a@b.com", "").password)
        assertTrue(Validators.login("not-an-email", "x").email != null)
        assertFalse(Validators.login("a@b.com", "weak").hasErrors)
    }

    // Verifies a fully valid registration form has no errors.
    @Test
    fun register_validFormHasNoErrors() {
        val errors = Validators.register("Thandi Nkosi", "thandi@example.com", "0821234567", "Passw0rd123", "Passw0rd123")
        assertFalse(errors.hasErrors)
    }

    // Verifies an empty registration form flags every required field (phone stays optional).
    @Test
    fun register_emptyFormFlagsRequiredFields() {
        val errors = Validators.register("", "", "", "", "")
        assertNotNull(errors.fullName)
        assertNotNull(errors.email)
        assertNull(errors.phone)
        assertNotNull(errors.password)
        assertNotNull(errors.confirmPassword)
    }

    // Verifies a mismatched confirmation is reported only on the confirmation field.
    @Test
    fun register_mismatchOnlyFlagsConfirmation() {
        val errors = Validators.register("Thandi", "thandi@example.com", "", "Passw0rd123", "Passw0rd999")
        assertEquals(Validators.RegisterErrors(confirmPassword = "Passwords do not match"), errors)
    }

    // Verifies the profile form (settings screen) validates name, email and phone.
    @Test
    fun profile_validatesEachField() {
        assertFalse(Validators.profile("Cathy", "cathy@example.com", "").hasErrors)
        val bad = Validators.profile("C", "nope", "abc")
        assertNotNull(bad.fullName)
        assertNotNull(bad.email)
        assertNotNull(bad.phone)
    }

    // Verifies the change-password form: current required, new strong and different, confirmation matches.
    @Test
    fun passwordChange_rules() {
        assertFalse(Validators.passwordChange("Old12345", "New12345", "New12345").hasErrors)
        assertEquals("Enter your current password", Validators.passwordChange("", "New12345", "New12345").current)
        assertNotNull(Validators.passwordChange("Old12345", "weak", "weak").new)
        assertEquals(
            "New password must be different from the current password",
            Validators.passwordChange("Old12345", "Old12345", "Old12345").new,
        )
        assertEquals("Passwords do not match", Validators.passwordChange("Old12345", "New12345", "New54321").confirm)
    }

    // Verifies typed prices parse to exact decimals, including a comma as the decimal separator.
    @Test
    fun parsePrice_acceptsValidPrices() {
        assertEquals(BigDecimal("1499.00"), Validators.parsePrice("1499.00"))
        assertEquals(BigDecimal("10.5"), Validators.parsePrice("10,5"))
        assertEquals(BigDecimal("250"), Validators.parsePrice(" 250 "))
        assertEquals(BigDecimal("0.01"), Validators.parsePrice("0.01"))
    }

    // Verifies invalid prices are rejected: zero, negative, too many decimals, text, empty.
    @Test
    fun parsePrice_rejectsInvalidPrices() {
        for (text in listOf("", "0", "0.00", "-5", "10.999", "abc", ".5", "1,499.00", "1e3", "12345678901")) {
            assertNull(text, Validators.parsePrice(text))
        }
        assertEquals(BigDecimal("0"), Validators.parsePrice("0", allowZero = true))
        assertEquals("Enter a price", Validators.price(" "))
        assertNotNull(Validators.price("10.999"))
        assertNull(Validators.price("10.50"))
    }

    // Verifies listing text fields follow the backend length rules.
    @Test
    fun listingNameAndCategory_lengths() {
        assertNotNull(Validators.listingName("A"))
        assertNull(Validators.listingName("Solar Panel"))
        assertNotNull(Validators.listingName("x".repeat(121)))
        assertNotNull(Validators.listingCategory(""))
        assertNull(Validators.listingCategory("Solar"))
        assertNotNull(Validators.listingCategory("x".repeat(51)))
    }

    // Verifies delivery days (1 to 365) and stock (0 to 1 000 000) must be whole numbers in range.
    @Test
    fun deliveryDaysAndStock_ranges() {
        assertNull(Validators.deliveryDays("1"))
        assertNull(Validators.deliveryDays("365"))
        assertNotNull(Validators.deliveryDays("0"))
        assertNotNull(Validators.deliveryDays("366"))
        assertNotNull(Validators.deliveryDays("2.5"))
        assertNotNull(Validators.deliveryDays(""))
        assertNull(Validators.stockQuantity("0"))
        assertNotNull(Validators.stockQuantity("-1"))
        assertNotNull(Validators.stockQuantity("abc"))
    }

    // Verifies the image link is optional, but must be http(s) when present.
    @Test
    fun imageUrl_optionalButMustBeHttp() {
        assertNull(Validators.imageUrl(""))
        assertNull(Validators.imageUrl("https://example.com/p.png"))
        assertNull(Validators.imageUrl("HTTP://example.com/p.png"))
        assertNotNull(Validators.imageUrl("ftp://example.com/p.png"))
        assertNotNull(Validators.imageUrl("example.com/p.png"))
    }

    // Verifies order notes are limited to 1000 characters.
    @Test
    fun orderNotes_limit() {
        assertNull(Validators.orderNotes(""))
        assertNull(Validators.orderNotes("x".repeat(1000)))
        assertNotNull(Validators.orderNotes("x".repeat(1001)))
    }
}