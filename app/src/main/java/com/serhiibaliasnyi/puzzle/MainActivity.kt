package com.serhiibaliasnyi.puzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import android.util.Log
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.Button
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import android.graphics.BitmapFactory
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PuzzleGame", "Starting MainActivity")
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    PuzzleGame()
                }
            }
        }
    }
}

@Composable
fun PuzzleGame() {
    val context = LocalContext.current
    val pieces = remember { mutableStateListOf<PuzzlePiece>() }
    var isStarted by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val originalImageWidth = 1024f
    val originalImageHeight = 768f
    val scaleFactor = screenWidth.value / originalImageWidth
    val puzzleWidth = originalImageWidth * scaleFactor
    val puzzleHeight = originalImageHeight * scaleFactor

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Scaled size: ${puzzleWidth.toInt()}x${puzzleHeight.toInt()} (scale: ${String.format("%.2f", scaleFactor)})",
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .width(puzzleWidth.dp)
                .height(puzzleHeight.dp)
                .background(MaterialTheme.colors.surface),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.puzzle_complete),
                contentDescription = "Complete Puzzle",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = if (isStarted) {
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                } else null,
                alpha = if (isStarted) 0.3f else 1f
            )

            // Horizontal ruler
            RulerScale(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.TopStart),
                isHorizontal = true,
                length = puzzleWidth,
                scaleFactor = scaleFactor
            )

            // Vertical ruler
            RulerScale(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .align(Alignment.TopStart),
                isHorizontal = false,
                length = puzzleHeight,
                scaleFactor = scaleFactor
            )

            if (isStarted) {
                pieces.forEachIndexed { index, piece ->
                    if (piece.isInPlace) {
                        DraggablePuzzlePiece(
                            piece = piece,
                            imageId = R.drawable.puzzle_piece1 + index,
                            onPositionChange = { newX, newY ->
                                pieces[index] = piece.copy(currentX = newX, currentY = newY)
                            },
                            scaleFactor = scaleFactor
                        )
                    }
                }
            }
        }

        // Start button
        Button(
            onClick = { isStarted = !isStarted },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp)
        ) {
            Text(if (isStarted) "Show Reference" else "Start Game")
        }

        // Divider with text
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(2.dp)
                .background(MaterialTheme.colors.primary)
        )

        Text(
            text = "Assemble from these pieces",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(16.dp)
        )

        // Область с пазлами для выбора
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                .padding(8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            pieces.forEachIndexed { index, piece ->
                if (!piece.isInPlace) {
                    DraggablePuzzlePiece(
                        piece = piece,
                        imageId = R.drawable.puzzle_piece1 + index,
                        onPositionChange = { newX, newY ->
                            val isOverPuzzleArea = newY < puzzleHeight
                            if (isOverPuzzleArea) {
                                val isNearCorrectPosition =
                                    Math.abs(newX - piece.correctX) < 20 &&
                                            Math.abs(newY - piece.correctY) < 20

                                if (isNearCorrectPosition) {
                                    pieces[index] = piece.copy(
                                        currentX = piece.correctX,
                                        currentY = piece.correctY,
                                        isInPlace = true
                                    )
                                } else {
                                    pieces[index] = piece.copy(currentX = newX, currentY = newY)
                                }
                            } else {
                                pieces[index] = piece.copy(currentX = newX, currentY = newY)
                            }
                        },
                        scaleFactor = scaleFactor
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val piecesPerColumn = 3

        // Функция для получения размеров изображения из ресурса
        fun getImageSize(resourceId: Int): Pair<Float, Float> {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resourceId, options)
            return Pair(options.outWidth.toFloat(), options.outHeight.toFloat())
        }

        // Получаем размеры оригинальной картинки и вычисляем масштаб
        val (originalWidth, originalHeight) = getImageSize(R.drawable.puzzle_complete)
        val scaleFactor = screenWidth.value / originalWidth
        Log.d("PuzzleGame", "Original image size: ${originalWidth}x${originalHeight}")
        Log.d("PuzzleGame", "Scale factor: $scaleFactor")

        // Получаем реальные размеры каждого пазла и масштабируем их
        val pieceSizes = (0..11).map { i ->
            val (width, height) = getImageSize(R.drawable.puzzle_piece1 + i)
            Log.d("PuzzleGame", "Piece ${i + 1} original size: ${width}x${height}")
            Pair(width * scaleFactor, height * scaleFactor)
        }

        // Вычисляем начальные позиции для области с частями пазла
        val maxPieceWidth = pieceSizes.maxOf { it.first }
        val maxPieceHeight = pieceSizes.maxOf { it.second }

        // Начинаем с самого края (0,0)
        val startX = 0f
        val startY = 0f

        // Создаем паззлы с правильными размерами
        for (i in 0..11) {
            val row = i / 4  // 3 ряда
            val col = i % 4  // 4 паззла в ряду
            val (pieceWidth, pieceHeight) = pieceSizes[i]

            pieces.add(
                PuzzlePiece(
                    initialX = startX + (col * pieceWidth),  // Используем реальную ширину каждого паззла
                    initialY = startY + (row * pieceHeight), // Используем реальную высоту каждого паззла
                    correctX = (screenWidth.value - puzzleWidth) / 2 + (i % 4) * (originalWidth / 4) * scaleFactor,
                    correctY = (i / 4) * (originalHeight / 3) * scaleFactor,
                    isInPlace = false,
                    width = pieceWidth,
                    height = pieceHeight
                )
            )
        }
    }
}

data class PuzzlePiece(
    val initialX: Float,
    val initialY: Float,
    val correctX: Float,
    val correctY: Float,
    val isInPlace: Boolean,
    val width: Float,     // Добавляем размеры в класс
    val height: Float,
    val currentX: Float = initialX,
    val currentY: Float = initialY
)

@Composable
fun DraggablePuzzlePiece(
    piece: PuzzlePiece,
    imageId: Int,
    onPositionChange: (Float, Float) -> Unit,
    scaleFactor: Float
) {
    var offsetX by remember { mutableStateOf(piece.currentX) }
    var offsetY by remember { mutableStateOf(piece.currentY) }

    Image(
        painter = painterResource(imageId),
        contentDescription = "Puzzle Piece",
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(piece.width.dp)
            .height(piece.height.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (!piece.isInPlace) {
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        onPositionChange(offsetX, offsetY)
                    }
                }
            },
        contentScale = ContentScale.Fit
    )
}

@Composable
fun RulerScale(
    modifier: Modifier,
    isHorizontal: Boolean,
    length: Float,
    scaleFactor: Float
) {
    val context = LocalContext.current
    val firstPieceSize = remember {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, R.drawable.puzzle_piece1, options)
        Pair(options.outWidth.toFloat(), options.outHeight.toFloat())
    }

    val step = if (isHorizontal) firstPieceSize.first * scaleFactor * 0.36f else firstPieceSize.second * scaleFactor * 0.36f
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                strokeWidth = 4f
                textSize = 32f
                isFakeBoldText = true
                setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
            }

            val scaledStep = step

            if (isHorizontal) {
                val width = size.width
                for (i in 0..width.toInt() step scaledStep.toInt()) {
                    val realValue = (i / scaleFactor).toInt()
                    drawLine(
                        color = androidx.compose.ui.graphics.Color.White,
                        start = Offset(i.toFloat(), 0f),
                        end = Offset(i.toFloat(), if (realValue % 100 == 0) 35f else 20f),
                        strokeWidth = 3f
                    )
                    if (realValue % 100 == 0) {
                        drawRect(
                            color = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
                            topLeft = Offset(i.toFloat() - 30f, 5f),
                            size = androidx.compose.ui.geometry.Size(60f, 35f)
                        )
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                "$realValue",
                                i.toFloat() - 25f,
                                35f,
                                paint
                            )
                        }
                    }
                }
            } else {
                val height = size.height
                for (i in 0..height.toInt() step scaledStep.toInt()) {
                    val realValue = (i / scaleFactor).toInt()
                    drawLine(
                        color = androidx.compose.ui.graphics.Color.White,
                        start = Offset(0f, i.toFloat()),
                        end = Offset(if (realValue % 100 == 0) 35f else 20f, i.toFloat()),
                        strokeWidth = 3f
                    )
                    if (realValue % 100 == 0) {
                        drawRect(
                            color = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
                            topLeft = Offset(5f, i.toFloat() - 20f),
                            size = androidx.compose.ui.geometry.Size(60f, 35f)
                        )
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                "$realValue",
                                10f,
                                i.toFloat() + 10f,
                                paint
                            )
                        }
                    }
                }
            }
        }

        // Информация о масштабе и шаге
        Column(
            modifier = Modifier
                .align(if (isHorizontal) Alignment.TopEnd else Alignment.BottomStart)
                .padding(4.dp)
                .background(MaterialTheme.colors.surface.copy(alpha = 0.8f))
                .padding(4.dp)
        ) {
            Text(
                text = "Шаг: ${step.toInt()} пикс",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text = "Масштаб: ${String.format("%.2f", scaleFactor)}x",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}