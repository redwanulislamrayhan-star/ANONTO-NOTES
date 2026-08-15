package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CanvasMode
import com.example.util.LedgerParseResult
import com.example.util.TagCategory

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotepadCanvas(
    noteText: String,
    onNoteChange: (String) -> Unit,
    parseResult: LedgerParseResult,
    canvasMode: CanvasMode,
    onVoiceInputClick: () -> Unit,
    onClearNote: () -> Unit,
    useBengaliNumerals: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val isIncomeMode = canvasMode == CanvasMode.INCOME

    val modeAccentColor = if (isIncomeMode) Color(0xFF059669) else Color(0xFFEA580C)
    val canvasBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFAFBFD)
    val canvasBorder = modeAccentColor.copy(alpha = 0.3f)

    val quickWords = if (isIncomeMode) {
        listOf("+", "000", "হাজার", "বেতন", "ফ্রিল্যান্সিং", "ভাড়া জমা", "ব্যবসা", "বোনাস", "উপহার", "প্রজেক্ট")
    } else {
        listOf("+", "000", "হাজার", "চাল", "মাছ", "ওষুধ", "বাজার", "ডাক্তার", "বিদ্যুৎ", "ভাড়া", "রিকশা", "তেল")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("notepad_canvas_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = canvasBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, canvasBorder, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            // Header Bar: Mode Label, Voice & Clear Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NoteAdd,
                        contentDescription = null,
                        tint = modeAccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isIncomeMode) {
                            if (useBengaliNumerals) "ইনকাম ক্যানভাস (Type Income)" else "Income Canvas"
                        } else {
                            if (useBengaliNumerals) "খরচ ক্যানভাস (Type Expense)" else "Expense Canvas"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = modeAccentColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice Mic Button
                    IconButton(
                        onClick = onVoiceInputClick,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(modeAccentColor.copy(alpha = 0.15f))
                            .testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = modeAccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (noteText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onClearNote,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("clear_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-line Text Editor with Plaintext Math Parser
            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("notepad_text_input"),
                placeholder = {
                    Text(
                        text = if (isIncomeMode) {
                            if (useBengaliNumerals) "যেমন লিখুন:\nবেতন ৫০০০০ + ফ্রিল্যান্সিং ২০০০০ + বাসা ভাড়া জমা ৮০০০" else "Type income:\nSalary 50000 + Freelance 20000 + Rent 8000"
                        } else {
                            if (useBengaliNumerals) "যেমন লিখুন:\nবাসা ভাড়া ১৫০০০ + চাল ২০০০ + আম্মু ডাক্তার ১৫০০ + ইভা ওষুধ ৬০০" else "Type expense:\nHouse Rent 15000 + Rice 2000 + Doctor 1500"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                },
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = modeAccentColor.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            // Detected Auto-Tags Live Display
            if (parseResult.allTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (useBengaliNumerals) "⚡ আনলিমিটেড অটো-ট্যাগ (Dynamic Auto-Tags):" else "⚡ Unlimited Dynamic Auto-Tags:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = modeAccentColor
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    parseResult.allTags.forEach { tag ->
                        TagBadgePill(tag = tag, useBengali = useBengaliNumerals)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Shortcut Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickWords) { word ->
                    Surface(
                        onClick = {
                            val space = if (noteText.endsWith(" ") || noteText.endsWith("\n") || noteText.isEmpty()) "" else " "
                            onNoteChange("$noteText$space$word")
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = modeAccentColor.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, modeAccentColor.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = word,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = modeAccentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagBadgePill(
    tag: TagCategory,
    useBengali: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = tag.color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, tag.color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = tag.iconEmoji, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (useBengali) tag.nameBn else tag.nameEn,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tag.color
            )
        }
    }
}
