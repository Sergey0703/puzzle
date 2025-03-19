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
import androidx.compose.material.Text  // Added this import
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
// Add this import
import androidx.compose.ui.Alignment

// Add these imports at the top
import androidx.compose.material.Button
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter

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
    val pieces = remember { mutableStateListOf<PuzzlePiece>() }
    var isStarted by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Reference image area (1/3 of screen)
        Box(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.33f)
            .background(MaterialTheme.colors.surface)
        ) {
            Image(
                painter = painterResource(R.drawable.puzzle_complete),
                contentDescription = "Complete Puzzle",
                modifier = Modifier
                    .width(768.dp)
                    .height(1024.dp)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit,
                colorFilter = if (isStarted) {
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                } else null,
                alpha = if (isStarted) 0.3f else 1f
            )
        }
        
        // Start button
        Button(
            onClick = { isStarted = !isStarted },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(if (isStarted) "Show Reference" else "Start Game")
        }
        
        // Divider with text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colors.primary)
        )
        
        Text(
            text = "Assemble from these pieces",
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        // Puzzle pieces area (remaining screen)
        Box(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colors.background)
        ) {
            pieces.forEachIndexed { index, piece ->
                DraggablePuzzlePiece(
                    piece = piece,
                    imageId = R.drawable.puzzle_piece1 + index,
                    onPositionChange = { newX, newY ->
                        pieces[index] = piece.copy(currentX = newX, currentY = newY)
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val puzzleSquareSize = 200f  // Keeping consistent with image size
        val piecesPerRow = 4
        val horizontalSpacing = 280f
        val verticalSpacing = 280f
        val startX = 20f
        val startY = 100f

        for (i in 0..11) {
            val row = i / piecesPerRow
            val col = i % piecesPerRow
            
            val randomOffsetX = (-20..20).random().toFloat()
            val randomOffsetY = (-20..20).random().toFloat()
            
            pieces.add(
                PuzzlePiece(
                    initialX = startX + (col * horizontalSpacing) + randomOffsetX,
                    initialY = startY + (row * verticalSpacing) + randomOffsetY,
                    correctX = col * puzzleSquareSize,
                    correctY = row * puzzleSquareSize
                )
            )
        }
    }
}

@Composable
fun DraggablePuzzlePiece(
    piece: PuzzlePiece,
    imageId: Int,
    onPositionChange: (Float, Float) -> Unit
) {
    var offsetX by remember { mutableStateOf(piece.initialX) }
    var offsetY by remember { mutableStateOf(piece.initialY) }

    // Remove fixed size and let the image maintain its proportions
    Image(
        painter = painterResource(imageId),
        contentDescription = "Puzzle Piece",
        modifier = Modifier
            .wrapContentSize()  // This will maintain original proportions
            .graphicsLayer(
                translationX = offsetX,
                translationY = offsetY
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    onPositionChange(offsetX, offsetY)
                }
            },
        contentScale = ContentScale.None  // Changed to None to prevent scaling
    )
}

data class PuzzlePiece(
    val initialX: Float,
    val initialY: Float,
    val correctX: Float,
    val correctY: Float,
    val currentX: Float = initialX,
    val currentY: Float = initialY
)