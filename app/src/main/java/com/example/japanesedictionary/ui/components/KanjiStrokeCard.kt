package com.example.japanesedictionary.ui.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import kotlin.math.sqrt

@Composable
fun KanjiStrokeCard(
    fileSvgName: String,
    targetSize: Dp = 350.dp
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State cho các đường path, độ dài, animatable dash phases, và scale matrix
    val paths = remember { mutableStateOf<List<Path>>(emptyList()) }
    val pathLengths = remember { mutableStateOf<List<Float>>(emptyList()) }
    val dashPhases =
        remember { mutableStateOf<List<Animatable<Float, AnimationVector1D>>>(emptyList()) }
    val scaleMatrix = remember { mutableStateOf(Matrix()) }

    // State cho màu đang chọn (mặc định là Red)
    val strokeColors = listOf(
        Color.Red, Color.Blue, Color.Green, Color.Black, Color.Magenta, Color.Gray
    )

    // Chuyển targetSize (dp) sang pixel
    val targetSizePx = with(LocalDensity.current) { targetSize.toPx() }

    // Tải SVG và chạy animation
    LaunchedEffect(fileSvgName, targetSizePx) {
        loadAndAnimatePaths(
            context = context,
            fileSvgName = fileSvgName,
            scope = scope,
            paths = paths,
            pathLengths = pathLengths,
            dashPhases = dashPhases,
            scaleMatrix = scaleMatrix,
            targetSize = targetSizePx
        )
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            Box {
                Canvas(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(targetSize)
                ) {
                    // Tính toán offset để căn giữa vùng có kích thước targetSizePx
                    val offsetX = (size.width - targetSizePx) / 2f
                    val offsetY = (size.height - targetSizePx) / 2f

                    withTransform({
                        translate(offsetX, offsetY)
                    }) {
                        paths.value.forEachIndexed { index, path ->

                            val selectedColor = strokeColors[index % strokeColors.size]

                            val textPaint = Paint().apply {
                                color = selectedColor.toArgb()
                                textSize = 16.dp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }


                            val pathLength = pathLengths.value.getOrNull(index) ?: 0f
                            val currentPhase =
                                dashPhases.value.getOrNull(index)?.value ?: pathLength

                            // Tạo bản sao của path và transform theo scaleMatrix
                            val transformedPath = Path(path)
                            transformedPath.transform(scaleMatrix.value)

                            // Vẽ nét với màu theo lựa chọn của người dùng
                            drawPath(
                                path = transformedPath.asComposePath(),
                                color = selectedColor,
                                style = Stroke(
                                    width = 5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(pathLength, pathLength),
                                        currentPhase
                                    ),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )


                            if (currentPhase <= 1f) {
                                val measure = PathMeasure(transformedPath, false)
                                val pos = FloatArray(2)
                                val tan = FloatArray(2)
                                if (measure.getPosTan(measure.length, pos, tan)) {
                                    // Tính vector pháp tuyến từ tan
                                    val normalX = -tan[1]
                                    val normalY = tan[0]

                                    // Chuẩn hóa vector pháp tuyến
                                    val normLength = sqrt(normalX * normalX + normalY * normalY)
                                    val (offsetX, offsetY) = if (normLength != 0f) {
                                        val normX = normalX / normLength
                                        val normY = normalY / normLength

                                        val offset = 16.dp.toPx()
                                        Pair(normX * offset, normY * offset)
                                    } else {
                                        Pair(0f, -16.dp.toPx())
                                    }

                                    drawContext.canvas.nativeCanvas.drawText(
                                        "${index + 1}",
                                        pos[0] + offsetX,
                                        pos[1] + offsetY,
                                        textPaint
                                    )
                                }
                            }
                        }
                    }
                }
                IconButton(
                    onClick = {
                        loadAndAnimatePaths(
                            context = context,
                            fileSvgName = fileSvgName,
                            scope = scope,
                            paths = paths,
                            pathLengths = pathLengths,
                            dashPhases = dashPhases,
                            scaleMatrix = scaleMatrix,
                            targetSize = targetSizePx
                        )
                    },
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        }
    }
}

fun loadAndAnimatePaths(
    context: Context,
    fileSvgName: String,
    scope: CoroutineScope,
    paths: MutableState<List<Path>>,
    pathLengths: MutableState<List<Float>>,
    dashPhases: MutableState<List<Animatable<Float, AnimationVector1D>>>,
    scaleMatrix: MutableState<Matrix>,
    targetSize: Float
) {
    scope.launch {
        val loadedPaths = loadSvgPaths(context, fileSvgName)
        paths.value = loadedPaths

        if (loadedPaths.isNotEmpty()) {
            // Tính unionBounds của tất cả các path
            val unionBounds = RectF().apply {
                loadedPaths.first().computeBounds(this, true)
            }
            loadedPaths.drop(1).forEach { path ->
                val tempRect = RectF()
                path.computeBounds(tempRect, true)
                unionBounds.union(tempRect)
            }

            // Thêm margin để tránh tràn nét
            val margin = targetSize * 0.1f
            val effectiveSize = targetSize - 2 * margin

            // Tính scale sao cho unionBounds phù hợp với effectiveSize
            val scale = effectiveSize / unionBounds.height()
            // Tính offset để căn giữa unionBounds và thêm margin offset
            val dx =
                (effectiveSize - unionBounds.width() * scale) / 2f - unionBounds.left * scale + margin
            val dy =
                (effectiveSize - unionBounds.height() * scale) / 2f - unionBounds.top * scale + margin

            scaleMatrix.value = Matrix().apply {
                setScale(scale, scale)
                postTranslate(dx, dy)
            }

            // Tính độ dài của từng path sau khi transform
            pathLengths.value = loadedPaths.map { path ->
                val transformedPath = Path(path)
                transformedPath.transform(scaleMatrix.value)
                PathMeasure(transformedPath, false).length
            }

            // Tạo Animatable cho hiệu ứng vẽ (dash phase)
            dashPhases.value = pathLengths.value.map { Animatable(it) }
            dashPhases.value.forEach { animatable ->
                animatable.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 750,
                        easing = LinearEasing
                    )
                )
                delay(400)
            }
        }
    }
}

fun loadSvgPaths(context: Context, fileName: String): List<Path> {
    val paths = mutableListOf<Path>()
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    val inputStream: InputStream = context.assets.open("kanji_svg/$fileName")
    parser.setInput(inputStream, null)

    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG && parser.name == "path") {
            val pathData = parser.getAttributeValue(null, "d")
            if (pathData != null) {
                val path = PathParser.createPathFromPathData(pathData)
                paths.add(path)
            }
        }
        eventType = parser.next()
    }
    inputStream.close()
    return paths
}
