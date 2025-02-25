package com.example.japanesedictionary.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, hasData: Boolean) {
    val splashDuration = 2000L
    val fadeInDuration = 1000

    // Tạo hiệu ứng alpha cho nội dung
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Hiệu ứng fade-in
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = fadeInDuration, easing = FastOutSlowInEasing)
        )
        // Giữ màn hình splash trong thời gian đã định
        delay(splashDuration)
        // Điều hướng đến màn hình phù hợp dựa trên hasData
        val targetRoute = if (hasData) "dictionary" else "loading"
        navController.navigate(targetRoute) {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Japanese Dictionary", // Thay đổi chữ hiển thị cho phù hợp
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alpha(alphaAnim.value)
        )
    }
}
