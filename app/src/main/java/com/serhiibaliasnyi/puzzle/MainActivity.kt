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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.unit.Density
import androidx.compose.ui.input.pointer.PointerInputChange

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

// Data class PuzzlePiece
data class PuzzlePiece(
    val initialX: Float,
    val initialY: Float,
    val correctX: Float,
    val correctY: Float,
    val isInPlace: Boolean,
    val width: Float,
    val height: Float,
    val currentX: Float = initialX,
    val currentY: Float = initialY
)


@Composable
fun PuzzleGame() {
    val context = LocalContext.current
    val pieces = remember { mutableStateListOf<PuzzlePiece>() }
    var isStarted by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val density = LocalContext.current.resources.displayMetrics.density

    // Calculate the screen width and height in pixels
    val screenWidthPixels = screenWidthDp * density
    val screenHeightPixels = configuration.screenHeightDp * density

    val originalImageWidth = 1024f
    val originalImageHeight = 768f
    val scaleFactor = 0.4013672f // задано руками

    val puzzleWidth = originalImageWidth * scaleFactor
    val puzzleHeight = originalImageHeight * scaleFactor

    // Height of the bottom area (DP)
    val bottomAreaHeightDp = 350.dp
    // Height of the bottom area in pixels
    val bottomAreaHeight = bottomAreaHeightDp.value * density

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
                painter = painterResource(id = R.drawable.puzzle_complete),
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
                            onPositionChange = { newX: Float, newY: Float ->
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

        // Bottom area for puzzle pieces
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomAreaHeightDp)
                .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                .padding(8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            pieces.forEachIndexed { index, piece ->
                if (!piece.isInPlace) {
                    DraggablePuzzlePiece(
                        piece = piece,
                        imageId = R.drawable.puzzle_piece1 + index,
                        onPositionChange = { newX: Float, newY: Float ->
                            val isOverPuzzleArea = newY < puzzleHeight
                            if (isOverPuzzleArea) {
                                val isNearCorrectPosition =
                                    abs(newX - piece.correctX) < 20 &&
                                            abs(newY - piece.correctY) < 20

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
        // Function to get image dimensions
        fun getImageSize(resourceId: Int): Pair<Float, Float> {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resourceId, options)
            return Pair(options.outWidth.toFloat(), options.outHeight.toFloat())
        }

        Log.d("PuzzleGame", "Screen width: ${screenWidthPixels}px")
        Log.d("PuzzleGame", "Screen height: ${configuration.screenHeightDp * density}px")

        val (originalWidth, originalHeight) = getImageSize(R.drawable.puzzle_complete)
        val scaleFactor =  0.4013672f

        Log.d("PuzzleGame", "Original image size: ${originalWidth}x${originalHeight} (pixels)")
        Log.d("PuzzleGame", "Scale factor: $scaleFactor")

        // Get scaled sizes of puzzle pieces
        val pieceSizes = (0..11).map { i ->
            val (width, height) = getImageSize(R.drawable.puzzle_piece1 + i)
            Log.d("PuzzleGame", "Piece ${i + 1} original size: ${width}x${height} (pixels)")
            val scaledWidth = width * scaleFactor
            val scaledHeight = height * scaleFactor
            Log.d("PuzzleGame", "Piece ${i + 1} scaled size: ${scaledWidth}x${scaledHeight} (pixels)")
            Pair(scaledWidth, scaledHeight)
        }

        // Available width and height in pixels
        val availableWidth = screenWidthPixels
        val availableHeight = bottomAreaHeight

        Log.d("PuzzleGame", "Available width: ${availableWidth} (pixels)")
        Log.d("PuzzleGame", "Available height: ${availableHeight} (pixels)")

        // *** MANUAL GRID LAYOUT ***
        val columns = 4
        val rows = 3

        // Calculate horizontal spacing total
        var maxPieceWidth = 0f
        for (i in 0 until 12) {
            if (pieceSizes[i].first > maxPieceWidth) {
                maxPieceWidth = pieceSizes[i].first
            }
        }
        var maxPieceHeight = 0f
        for (i in 0 until 12) {
            if (pieceSizes[i].second > maxPieceHeight) {
                maxPieceHeight = pieceSizes[i].second
            }
        }

        Log.d("PuzzleGame", "Max Piece Width: ${maxPieceWidth} (pixels)")
        Log.d("PuzzleGame", "Max Piece Height: ${maxPieceHeight} (pixels)")


        // Calculate horizontal and vertical spacing
        val horizontalSpacing = 0f
        val verticalSpacing = 0f

        // Calculate cell sizes
        val cellWidth = availableWidth / columns
        val cellHeight = availableHeight / rows
        Log.d("PuzzleGame", "Calculated columns: $columns, rows: $rows")
        Log.d("PuzzleGame", "Cell width: $cellWidth (pixels)")
        Log.d("PuzzleGame", "Cell height: $cellHeight (pixels)")
        Log.d("PuzzleGame", "horizontalSpacing: $horizontalSpacing (pixels)")
        Log.d("PuzzleGame", "verticalSpacing: $verticalSpacing (pixels)")


        // Calculate Initial Positions
        val startX = 0f
        val startY = 0f

        for (i in 0 until 12) {
            val row = i / columns
            val col = i % columns
            val (pieceWidth, pieceHeight) = pieceSizes[i]


            // Calculate correct X and Y with manual grid
            //val correctX = startX + col * cellWidth + (cellWidth - pieceWidth) / 2  // Previous
            //val correctY = startY + row * cellHeight + (cellHeight - pieceHeight) / 2  // Previous

            // New code
            val correctX = startX + col * cellWidth
            val correctY = startY + row * cellHeight
            // Log
            Log.d("PuzzleGame", "Puzzle ${i + 1}: width=${pieceWidth}, height=${pieceHeight}")

            Log.d("PuzzleGame", "Puzzle ${i + 1}: correctX=$correctX, correctY=$correctY (pixels)")

            pieces.add(
                PuzzlePiece(
                    initialX = correctX,
                    initialY = correctY,
                    correctX = correctX,
                    correctY = correctY,
                    isInPlace = false,
                    width = pieceWidth,
                    height = pieceHeight
                )
            )
        }
    }
}

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
        painter = painterResource(id = imageId),
        contentDescription = "Puzzle Piece",
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(piece.width.dp)
            .height(piece.height.dp)
            .pointerInput(Unit) {
                detectDragGestures { change: PointerInputChange, dragAmount: Offset ->
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

        // Display scale information
        Column(
            modifier = Modifier
                .align(if (isHorizontal) Alignment.TopEnd else Alignment.BottomStart)
                .padding(4.dp)
                .background(MaterialTheme.colors.surface.copy(alpha = 0.8f))
                .padding(4.dp)
        ) {
            Text(
                text = "Step: ${step.toInt()} pixels",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text = "Scale: ${String.format("%.2f", scaleFactor)}x",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface
            )
        }
    }
}