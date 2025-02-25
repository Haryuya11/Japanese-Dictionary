package com.example.japanesedictionary.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.japanesedictionary.viewmodel.DictionaryViewModel

@Composable
fun SearchModeSwitch(viewModel: DictionaryViewModel) {
    val currentMode = viewModel.searchMode.value  // "ja" hoặc "en"

    BoxWithConstraints(
        modifier = Modifier
            .width(80.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.LightGray)
    ) {
        // Chiều rộng của mỗi phần
        val segmentWidth = maxWidth / 2

        // Tính vị trí indicator dựa theo chế độ hiện tại
        val targetOffset = if (currentMode == "ja") 0.dp else segmentWidth
        val animatedOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = tween(durationMillis = 300),
            label = "animatedOffset"
        )

        // Indicator nền cho phần đang được chọn
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.saveSearchMode("ja") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "JA",
                    color = if (currentMode == "ja") Color.White else Color.Black
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.saveSearchMode("en") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EN",
                    color = if (currentMode == "en") Color.White else Color.Black
                )
            }
        }
    }
}