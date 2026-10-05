package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.BangladeshCity
import com.example.location.LocationItem
import com.example.location.LocationServiceImpl
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BangladeshDistrictPickerSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onLocationSelected: (BangladeshCity) -> Unit,
    onLocationItemSelected: ((LocationItem) -> Unit)? = null,
    onUseLiveLocation: (() -> Unit)? = null,
    isDetectingLocation: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!isOpen) return

    val context = LocalContext.current
    val locationService = remember { LocationServiceImpl(context) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(LocationServiceImpl.DEFAULT_SUGGESTIONS) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = LocationServiceImpl.DEFAULT_SUGGESTIONS
            isSearching = false
        } else {
            isSearching = true
            delay(280) // Debounce typing
            searchResults = locationService.searchLocations(searchQuery)
            isSearching = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "স্থান খুঁজুন (জেলা, উপজেলা, শহর, গ্রাম)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Clear, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("location_search_input"),
                placeholder = {
                    Text(text = "স্থান খুঁজুন (যেমন: নাগেশ্বরী শহর, কুড়িগ্রাম)")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // "বর্তমান অবস্থান ব্যবহার করুন" Button
            if (onUseLiveLocation != null) {
                Button(
                    onClick = {
                        onUseLiveLocation()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_use_current_location"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "বর্তমান অবস্থান ব্যবহার করুন (GPS)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick suggestion chips
            Text(
                text = "জনপ্রিয় স্থানসমূহ:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val quickChips = listOf(
                    "নাগেশ্বরী শহর" to (25.9667 to 89.6833),
                    "কুড়িগ্রাম সদর" to (25.8050 to 89.6360),
                    "ঢাকা" to (23.8103 to 90.4125),
                    "শ্রীমঙ্গল" to (24.3065 to 91.7296),
                    "কক্সবাজার" to (21.4272 to 92.0058),
                    "রংপুর" to (25.7439 to 89.2752)
                )

                items(quickChips) { (title, coords) ->
                    SuggestionChip(
                        onClick = {
                            val item = LocationItem(
                                id = "chip_${title}",
                                nameBn = title,
                                nameEn = title,
                                hierarchyBn = title,
                                typeLabel = "শহর/উপজেলা",
                                latitude = coords.first,
                                longitude = coords.second
                            )
                            if (onLocationItemSelected != null) {
                                onLocationItemSelected(item)
                            } else {
                                onLocationSelected(
                                    BangladeshCity(title, title, "", "", coords.first, coords.second)
                                )
                            }
                            onDismiss()
                        },
                        label = { Text(title, fontWeight = FontWeight.Medium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Search Results List
            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "কোনো স্থান খুঁজে পাওয়া যায়নি। অন্য নাম দিয়ে চেষ্টা করুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(searchResults, key = { "${it.id}_${it.latitude}_${it.longitude}" }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (onLocationItemSelected != null) {
                                        onLocationItemSelected(item)
                                    } else {
                                        onLocationSelected(
                                            BangladeshCity(
                                                item.nameEn,
                                                item.nameBn,
                                                item.hierarchyBn,
                                                item.hierarchyBn,
                                                item.latitude,
                                                item.longitude
                                            )
                                        )
                                    }
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                                .testTag("location_item_${item.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.typeLabel.contains("শহর") || item.typeLabel.contains("জেলা")) Icons.Default.LocationCity else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "📍 ${item.nameBn}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.typeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Text(
                                    text = item.hierarchyBn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
