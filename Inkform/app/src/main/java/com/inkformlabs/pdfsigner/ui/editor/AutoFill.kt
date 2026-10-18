package com.inkformlabs.pdfsigner.ui.editor

import com.inkformlabs.pdfsigner.data.Profile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Matches form-field names/labels to the saved profile. Only fills fields that are still empty. */
object AutoFill {

    fun valueFor(fieldName: String, label: String, profile: Profile): String? {
        val key = (fieldName + " " + label).lowercase().replace(Regex("[^a-z0-9]"), "")
        fun has(vararg words: String) = words.any { key.contains(it) }

        return when {
            has("email", "mail") -> profile.email
            has("phone", "mobile", "telephone", "tel", "cell") -> profile.phone
            has("birth", "dob") -> profile.dateOfBirth
            has("firstname", "givenname", "fname", "forename") -> profile.firstName.ifBlank { profile.displayName.substringBefore(' ') }
            has("lastname", "surname", "familyname", "lname") -> profile.lastName.ifBlank { profile.displayName.substringAfterLast(' ', "") }
            has("zip", "postal", "postcode") -> profile.zip
            has("city", "town") -> profile.city
            has("state", "province", "region", "county") -> profile.state
            has("country") -> profile.country
            has("company", "employer", "organization", "organisation", "business") -> profile.company
            has("address", "street", "addr") -> profile.address
            has("fullname", "name", "applicant", "signername", "printname") -> profile.displayName
            has("date", "today") -> today()
            else -> null
        }?.takeIf { it.isNotBlank() }
    }

    fun today(): String = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}
