package com.example.gymtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.ui.theme.BodyFont
import com.example.gymtracker.ui.theme.Gym

/**
 * Compact equipment picker plus a Per / Total switch. The switch only appears for
 * one-handed equipment (dumbbell, cable); bars are always loaded as a total.
 */
@Composable
fun EquipmentSelector(
    equipment: Equipment?,
    perSide: Boolean,
    onChange: (equipment: Equipment?, perSide: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            Row(
                Modifier
                    .clip(SmallShape)
                    .border(1.dp, Gym.InputBorder, SmallShape)
                    .clickable { open = true }
                    .padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(equipment?.label ?: "Equipment", style = mono(12.sp, if (equipment == null) Gym.Faint else Gym.Muted))
                ChevronDownIcon(Gym.Muted)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Gym.Background) {
                (listOf<Equipment?>(null) + Equipment.entries).forEach { e ->
                    val selected = e == equipment
                    DropdownMenuItem(
                        text = {
                            Text(
                                e?.label ?: "None",
                                fontFamily = BodyFont,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) Gym.PrimaryText else Gym.Foreground,
                            )
                        },
                        onClick = {
                            open = false
                            if (e != equipment) onChange(e, e?.defaultPerSide ?: false)
                        },
                    )
                }
            }
        }

        if (equipment?.supportsPerSide == true) {
            Row(Modifier.clip(SmallShape).border(1.dp, Gym.InputBorder, SmallShape)) {
                Segment("Per", selected = perSide) { onChange(equipment, true) }
                Segment("Total", selected = !perSide) { onChange(equipment, false) }
            }
        }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = mono(12.sp, if (selected) Gym.OnPrimary else Gym.Muted),
        modifier = Modifier
            .background(if (selected) Gym.Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
