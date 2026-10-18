package com.inkformlabs.pdfsigner.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inkformlabs.pdfsigner.data.Profile
import com.inkformlabs.pdfsigner.ui.LocalAppContainer
import com.inkformlabs.pdfsigner.ui.components.InkButton
import com.inkformlabs.pdfsigner.ui.components.InkCard
import com.inkformlabs.pdfsigner.ui.components.InkTextField
import com.inkformlabs.pdfsigner.ui.components.SectionLabel
import com.inkformlabs.pdfsigner.ui.theme.InkIcons
import com.inkformlabs.pdfsigner.ui.theme.LocalInk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Personal details used to auto-fill forms. Stored only on the device. */
@Composable
fun ProfileTab(onSaved: (String) -> Unit) {
    val p = LocalInk.current
    val store = LocalAppContainer.current.profile
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf<Profile?>(null) }
    LaunchedEffect(Unit) { if (draft == null) draft = store.profile.first() }
    val profile = draft ?: return

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("My details", style = MaterialTheme.typography.headlineSmall, color = p.ink)
        InkCard {
            Text(
                "Inkform uses these to auto-fill matching fields (name, email, address…) with one tap. " +
                    "They are saved only on this phone and never shared.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.muted,
            )
        }
        SectionLabel("Name", Modifier.padding(top = 6.dp))
        InkTextField(profile.fullName, { draft = profile.copy(fullName = it) }, "Full name")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InkTextField(profile.firstName, { draft = profile.copy(firstName = it) }, "First name", Modifier.weight(1f))
            InkTextField(profile.lastName, { draft = profile.copy(lastName = it) }, "Last name", Modifier.weight(1f))
        }
        InkTextField(profile.dateOfBirth, { draft = profile.copy(dateOfBirth = it) }, "Date of birth", placeholder = "DD/MM/YYYY")
        SectionLabel("Contact", Modifier.padding(top = 6.dp))
        InkTextField(profile.email, { draft = profile.copy(email = it) }, "Email")
        InkTextField(profile.phone, { draft = profile.copy(phone = it) }, "Phone")
        InkTextField(profile.company, { draft = profile.copy(company = it) }, "Company / employer")
        SectionLabel("Address", Modifier.padding(top = 6.dp))
        InkTextField(profile.address, { draft = profile.copy(address = it) }, "Street address")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InkTextField(profile.city, { draft = profile.copy(city = it) }, "City", Modifier.weight(1f))
            InkTextField(profile.state, { draft = profile.copy(state = it) }, "State / region", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InkTextField(profile.zip, { draft = profile.copy(zip = it) }, "ZIP / postal code", Modifier.weight(1f))
            InkTextField(profile.country, { draft = profile.copy(country = it) }, "Country", Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        InkButton(
            "Save details",
            icon = InkIcons.Check,
            onClick = {
                scope.launch {
                    store.save(profile)
                    onSaved("Details saved on this phone")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
    }
}
