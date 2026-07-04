package com.raymi.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.raymi.app.core.lang.LocalRaymiStrings

data class Country(
    val name: String,
    val code: String,
    val flag: String,
    val phoneLength: Int
)

val AllAmiricasCountries = listOf(
    Country("Antigua y Barbuda", "+1", "🇦🇬", 7),
    Country("Argentina", "+54", "🇦🇷", 10),
    Country("Bahamas", "+1", "🇧🇸", 7),
    Country("Barbados", "+1", "🇧🇧", 7),
    Country("Belice", "+501", "🇧🇿", 7),
    Country("Bolivia", "+591", "🇧🇴", 8),
    Country("Brasil", "+55", "🇧🇷", 11),
    Country("Canadá", "+1", "🇨🇦", 10),
    Country("Chile", "+56", "🇨🇱", 9),
    Country("Colombia", "+57", "🇨🇴", 10),
    Country("Costa Rica", "+506", "🇨🇷", 8),
    Country("Cuba", "+53", "🇨🇺", 8),
    Country("Dominica", "+1", "🇩🇲", 7),
    Country("Ecuador", "+593", "🇪🇨", 9),
    Country("El Salvador", "+503", "🇸🇻", 8),
    Country("Estados Unidos", "+1", "🇺🇸", 10),
    Country("Granada", "+1", "🇬🇩", 7),
    Country("Guatemala", "+502", "🇬🇹", 8),
    Country("Guyana", "+592", "🇬🇾", 7),
    Country("Haití", "+509", "🇭🇹", 8),
    Country("Honduras", "+504", "🇭🇳", 8),
    Country("Jamaica", "+1", "🇯🇲", 10),
    Country("México", "+52", "🇲🇽", 10),
    Country("Nicaragua", "+505", "🇳🇮", 8),
    Country("Panamá", "+507", "🇵🇦", 8),
    Country("Paraguay", "+595", "🇵🇾", 9),
    Country("Perú", "+51", "🇵🇪", 9),
    Country("Rep. Dominicana", "+1", "🇩🇴", 10),
    Country("San Cristóbal y Nieves", "+1", "🇰🇳", 7),
    Country("Santa Lucía", "+1", "🇱🇨", 7),
    Country("San Vicente y las Granadinas", "+1", "🇻🇨", 7),
    Country("Surinam", "+597", "🇸🇷", 7),
    Country("Trinidad y Tobago", "+1", "🇹🇹", 7),
    Country("Uruguay", "+598", "🇺🇾", 9),
    Country("Venezuela", "+58", "🇻🇪", 10)
)

@Composable
fun RaymiPhoneField(
    phone: String,
    onPhoneChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Teléfono",
    isError: Boolean = false,
    supportingText: String? = null,
    testTag: String? = null
) {
    var showCountryDialog by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(AllAmiricasCountries.find { it.code == "+51" } ?: AllAmiricasCountries[0]) }
    val strings = LocalRaymiStrings.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = { showCountryDialog = true },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(selectedCountry.flag, fontSize = 20.sp)
                Spacer(Modifier.width(4.dp))
                Text(selectedCountry.code, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        OutlinedTextField(
            value = phone,
            onValueChange = {
                if (it.length <= selectedCountry.phoneLength && it.all { c -> c.isDigit() }) {
                    onPhoneChange(it)
                }
            },
            label = { Text(label) },
            modifier = Modifier.weight(1f).then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            shape = MaterialTheme.shapes.large,
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } } ?: {
                Text("${if (strings is com.raymi.app.core.lang.SpanishStrings) "Máx." else "Max."} ${selectedCountry.phoneLength} ${if (strings is com.raymi.app.core.lang.SpanishStrings) "dígitos" else "digits"}", style = MaterialTheme.typography.labelSmall)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp)) },
            singleLine = true
        )
    }

    if (showCountryDialog) {
        CountrySelectorDialog(
            onDismiss = { showCountryDialog = false },
            onSelect = {
                selectedCountry = it
                showCountryDialog = false
                if (phone.length > it.phoneLength) {
                    onPhoneChange(phone.take(it.phoneLength))
                }
            },
            strings = strings
        )
    }
}

@Composable
fun CountrySelectorDialog(
    onDismiss: () -> Unit,
    onSelect: (Country) -> Unit,
    strings: com.raymi.app.core.lang.RaymiStrings
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(if (strings is com.raymi.app.core.lang.SpanishStrings) "Selecciona tu País" else "Select your Country", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(AllAmiricasCountries) { country ->
                        Surface(
                            onClick = { onSelect(country) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(country.flag, fontSize = 24.sp)
                                Spacer(Modifier.width(16.dp))
                                Text(country.name, modifier = Modifier.weight(1f))
                                Text(country.code, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(strings.close)
                }
            }
        }
    }
}
