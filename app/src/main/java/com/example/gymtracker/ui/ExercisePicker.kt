package com.example.gymtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gymtracker.data.CATEGORIES
import com.example.gymtracker.data.EXERCISE_LIBRARY
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.ui.theme.BodyFont
import com.example.gymtracker.ui.theme.Gym

@Composable
fun ExercisePicker(onSelect: (name: String, category: String, equipment: Equipment?) -> Unit, onClose: () -> Unit) {
    var search by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("All") }
    var custom by remember { mutableStateOf("") }
    var customCat by remember { mutableStateOf("Chest") }
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { searchFocus.requestFocus() }

    val filtered = EXERCISE_LIBRARY.filter {
        (cat == "All" || it.category == cat) && it.name.contains(search, ignoreCase = true)
    }

    fun pick(name: String, category: String, equipment: Equipment? = null) {
        onSelect(name, category, equipment)
        onClose()
    }

    fun addCustom() {
        if (custom.isNotBlank()) pick(custom.trim(), customCat)
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Gym.Foreground.copy(alpha = 0.45f))
                .clickable(interactionSource = null, indication = null, onClick = onClose)
                .systemBarsPadding()
                .imePadding()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp
            Column(
                Modifier
                    .widthIn(max = 448.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .card(RoundedCornerShape(12.dp))
                    // Swallow taps so they don't reach the scrim.
                    .clickable(interactionSource = null, indication = null) {},
            ) {
                // Search
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SearchIcon(Gym.Muted)
                    BareTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = "Search exercises…",
                        style = body(14.sp),
                        placeholderColor = Gym.Muted,
                        modifier = Modifier.weight(1f).focusRequester(searchFocus),
                    )
                    CloseIcon(14.dp, Gym.Muted, Modifier.clickable(onClick = onClose))
                }
                HorizontalDivider(color = Gym.Divider)

                // Category filter
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CATEGORIES.forEach { c ->
                        val active = cat == c
                        Text(
                            c,
                            style = mono(12.sp, if (active) Gym.OnPrimary else Gym.Muted),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) Gym.Primary else Gym.Chip)
                                .clickable { cat = c }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                // Results
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(filtered, key = { it.name }) { e ->
                        Column {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { pick(e.name, e.category, e.equipment) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CategoryChip(e.category, minWidth = true)
                                Text(e.name, style = body(14.sp))
                            }
                            HorizontalDivider(color = Gym.Divider)
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                "No results",
                                style = body(14.sp, Gym.Faint),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }

                // Custom exercise
                HorizontalDivider(color = Gym.Divider)
                Column(Modifier.padding(16.dp)) {
                    Text("CUSTOM EXERCISE", style = mono(12.sp, Gym.Dimmer), modifier = Modifier.padding(bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        BareTextField(
                            value = custom,
                            onValueChange = { custom = it },
                            placeholder = "Exercise name…",
                            style = body(14.sp),
                            placeholderColor = Gym.Muted,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addCustom() }),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Gym.InputBorder, SmallShape)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        CategorySelect(customCat, onChange = { customCat = it })
                        PrimaryButton("Add", onClick = ::addCustom, padding = 12.dp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySelect(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .border(1.dp, Gym.InputBorder, SmallShape)
                .clip(SmallShape)
                .clickable { open = true }
                .padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value, style = body(12.sp, Gym.Muted))
            ChevronDownIcon(Gym.Muted)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = Gym.Chip,
        ) {
            CATEGORIES.drop(1).forEach { c ->
                DropdownMenuItem(
                    text = {
                        Text(
                            c,
                            fontFamily = BodyFont,
                            fontSize = 14.sp,
                            fontWeight = if (c == value) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (c == value) Gym.Primary else Gym.Foreground,
                        )
                    },
                    onClick = {
                        onChange(c)
                        open = false
                    },
                )
            }
        }
    }
}
