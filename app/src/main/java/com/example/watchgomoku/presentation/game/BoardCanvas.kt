package com.example.watchgomoku.presentation.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watchgomoku.R
import com.example.watchgomoku.domain.model.CellState
import com.example.watchgomoku.domain.model.GameState
import com.example.watchgomoku.domain.model.Player
import com.example.watchgomoku.domain.model.Position
import com.example.watchgomoku.ui.theme.BoardPalette
import com.example.watchgomoku.ui.theme.scaledDp
import com.example.watchgomoku.util.BoardGeometry
import kotlinx.coroutines.delay

/**
 * Canvas 棋盘：按背景 → 网格 → 星位 → 坐标 → 棋子 → 序号 →
 * 最后一手标记 → 获胜连线 → 候选点 → 预览落点的顺序绘制。
 * 单击落点吸附最近交叉点；双指以手指中点为缩放中心；单指缩放后拖动。
 */
@Composable
fun GomokuBoardCanvas(
    gameState: GameState,
    viewState: BoardViewState,
    palette: BoardPalette,
    showLastMove: Boolean,
    showNumbers: Boolean,
    showCoordinates: Boolean,
    stoneFactor: Float,
    hints: List<Position>,
    enabled: Boolean,
    onTap: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSizePx by remember { mutableIntStateOf(0) }
    var previewPoint by remember { mutableStateOf<Position?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val pixelPerSp = density.density * density.fontScale

    LaunchedEffect(gameState.moves.size) {
        previewPoint = null
    }

    val turnSide = if (gameState.currentPlayer == Player.BLACK) {
        stringResource(R.string.side_black)
    } else {
        stringResource(R.string.side_white)
    }
    val boardDescription = stringResource(
        R.string.a11y_board_fmt, gameState.moves.size, turnSide
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .onSizeChanged { canvasSizePx = it.width }
            .semantics { contentDescription = boardDescription }
            .pointerInput(enabled, gameState.moves.size) {
                detectTapGestures(
                    onPress = { offset ->
                        if (enabled) {
                            previewPoint = hitTest(
                                offset.x, offset.y,
                                canvasSizePx.toFloat(), viewState, gameState.boardSize
                            )
                        }
                    },
                    onTap = { offset ->
                        if (enabled) {
                            hitTest(
                                offset.x, offset.y,
                                canvasSizePx.toFloat(), viewState, gameState.boardSize
                            )?.let { point -> onTap(point.row, point.column) }
                        }
                    }
                )
            }
            .pointerInput(viewState) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (zoom != 1f || pan != Offset.Zero) {
                        viewState.applyTransform(
                            centroid.x, centroid.y,
                            pan.x, pan.y, zoom,
                            size.width.toFloat()
                        )
                    }
                }
            }
    ) {
        if (canvasSizePx > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawGomokuBoard(
                    gameState = gameState,
                    viewState = viewState,
                    palette = palette,
                    showLastMove = showLastMove,
                    showNumbers = showNumbers,
                    showCoordinates = showCoordinates,
                    stoneFactor = stoneFactor,
                    hints = hints,
                    previewPoint = previewPoint,
                    textMeasurer = textMeasurer,
                    pixelPerSp = pixelPerSp
                )
            }
        }
    }
}

private fun hitTest(
    screenX: Float,
    screenY: Float,
    viewportPx: Float,
    viewState: BoardViewState,
    boardSize: Int
): Position? {
    val (localX, localY) = BoardGeometry.screenToBoardLocal(
        screenX, screenY, viewState.scale, viewState.offsetX, viewState.offsetY
    )
    val layout = BoardGeometry.standardLayout(boardSize, viewportPx)
    return BoardGeometry.cellAt(localX, localY, layout)
}

private fun DrawScope.drawGomokuBoard(
    gameState: GameState,
    viewState: BoardViewState,
    palette: BoardPalette,
    showLastMove: Boolean,
    showNumbers: Boolean,
    showCoordinates: Boolean,
    stoneFactor: Float,
    hints: List<Position>,
    previewPoint: Position?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    pixelPerSp: Float
) {
    val px = size.minDimension
    val n = gameState.boardSize
    val layout = BoardGeometry.standardLayout(n, px)
    val cell = layout.cellSize
    val radius = CornerRadius(cell * 0.35f)

    withTransform({
        translate(viewState.offsetX, viewState.offsetY)
        scale(viewState.scale, viewState.scale, pivot = Offset.Zero)
    }) {
        // 1. 背景
        drawRoundRect(
            color = palette.boardBackground,
            topLeft = Offset.Zero,
            size = Size(px, px),
            cornerRadius = radius
        )
        // 2. 网格
        val gridStroke = (cell * 0.035f).coerceAtLeast(0.8f)
        val lineStart = layout.originX
        val lineEnd = layout.originX + (n - 1) * cell
        for (i in 0 until n) {
            val p = layout.originY + i * cell
            drawLine(palette.gridLine, Offset(lineStart, p), Offset(lineEnd, p), gridStroke)
            drawLine(palette.gridLine, Offset(p, lineStart), Offset(p, lineEnd), gridStroke)
        }
        // 边框
        drawRoundRect(
            color = palette.boardBorder,
            topLeft = Offset.Zero,
            size = Size(px, px),
            cornerRadius = radius,
            style = Stroke(width = gridStroke * 2f)
        )
        // 3. 星位
        val starRadius = cell * 0.09f
        BoardGeometry.starPoints(n).forEach { star ->
            val (cx, cy) = BoardGeometry.cellCenter(star.row, star.column, layout)
            drawCircle(palette.starPoint, starRadius, Offset(cx, cy))
        }
        // 坐标
        if (showCoordinates) {
            for (i in 0 until n) {
                val p = layout.originX + i * cell
                drawCenteredText(
                    textMeasurer, (i + 1).toString(),
                    p, layout.originY * 0.45f, cell * 0.6f,
                    palette.coordinateText, FontWeight.Normal, pixelPerSp
                )
                drawCenteredText(
                    textMeasurer, (i + 1).toString(),
                    layout.originX * 0.45f, p, cell * 0.6f,
                    palette.coordinateText, FontWeight.Normal, pixelPerSp
                )
            }
        }
        // 4~7. 棋子 / 序号 / 最后一手
        val stoneRadius = cell * stoneFactor / 2f
        val shadowOffset = cell * 0.05f
        val lastMoveNumber = gameState.moves.lastOrNull()?.moveNumber
        gameState.moves.forEach { move ->
            val (cx, cy) = BoardGeometry.cellCenter(move.position.row, move.position.column, layout)
            val isBlack = move.player == Player.BLACK
            drawCircle(
                palette.stoneShadow, stoneRadius,
                Offset(cx + shadowOffset, cy + shadowOffset)
            )
            drawCircle(
                if (isBlack) palette.blackStone else palette.whiteStone,
                stoneRadius, Offset(cx, cy)
            )
            val outline = if (isBlack) palette.blackStoneOutline else palette.whiteStoneOutline
            if (outline != Color.Transparent) {
                drawCircle(outline, stoneRadius, Offset(cx, cy), style = Stroke(cell * 0.045f))
            }
            if (showNumbers) {
                drawCenteredText(
                    textMeasurer, move.moveNumber.toString(), cx, cy, cell,
                    if (isBlack) palette.whiteStone else palette.blackStone,
                    FontWeight.Bold, pixelPerSp
                )
            }
            if (showLastMove && move.moveNumber == lastMoveNumber) {
                // 外圈标记：不只依赖颜色
                drawCircle(
                    palette.lastMoveMarker, stoneRadius * 1.28f, Offset(cx, cy),
                    style = Stroke(cell * 0.06f)
                )
            }
        }
        // 8. 获胜连线
        if (gameState.winningLine.size >= 2) {
            val first = gameState.winningLine.first()
            val last = gameState.winningLine.last()
            val (x1, y1) = BoardGeometry.cellCenter(first.row, first.column, layout)
            val (x2, y2) = BoardGeometry.cellCenter(last.row, last.column, layout)
            drawLine(
                palette.winLine, Offset(x1, y1), Offset(x2, y2),
                strokeWidth = cell * 0.12f, cap = StrokeCap.Round
            )
        }
        // 候选落点（练习 / 建议开启时）
        hints.forEach { hint ->
            val (cx, cy) = BoardGeometry.cellCenter(hint.row, hint.column, layout)
            drawCircle(
                palette.hintText, cell * 0.18f, Offset(cx, cy),
                style = Stroke(cell * 0.05f)
            )
        }
        // 预览落点
        val preview = previewPoint
        if (preview != null &&
            preview.row in 0 until n &&
            preview.column in 0 until n &&
            gameState.board[preview.row][preview.column] == CellState.EMPTY
        ) {
            val (cx, cy) = BoardGeometry.cellCenter(preview.row, preview.column, layout)
            drawCircle(
                palette.lastMoveMarker.copy(alpha = 0.55f), stoneRadius * 0.85f,
                Offset(cx, cy), style = Stroke(cell * 0.06f)
            )
        }
    }
}

private fun DrawScope.drawCenteredText(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    cx: Float,
    cy: Float,
    cell: Float,
    color: Color,
    fontWeight: FontWeight,
    pixelPerSp: Float
) {
    // 棋盘内文字按像素恒定：除以 pixelPerSp 抵消全局字体缩放，避免溢出格子
    val style = TextStyle(
        fontSize = TextUnit(cell * 0.42f / pixelPerSp, TextUnitType.Sp),
        color = color,
        fontWeight = fontWeight
    )
    val measured = textMeasurer.measure(text, style)
    drawText(
        measured,
        topLeft = Offset(cx - measured.size.width / 2f, cy - measured.size.height / 2f)
    )
}

/**
 * 棋盘右缘垂直居中的收纳式缩放控制：
 * 平时只显示一个半透明切换钮，不挡棋盘视野；
 * 点击展开 ＋/−/适/中 面板，5 秒无操作自动收起；
 * 点“适应屏幕”后立即收起（视野已完整）。全部按钮 ≥40dp、避开四角圆角区。
 */
@Composable
fun BoardZoomControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFit: () -> Unit,
    onCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var lastActionAt by remember { mutableLongStateOf(0L) }
    val expandDesc = stringResource(R.string.zoom_toggle_expand)
    val collapseDesc = stringResource(R.string.zoom_toggle_collapse)

    fun tap(action: () -> Unit, collapseAfter: Boolean = false) {
        lastActionAt = System.currentTimeMillis()
        action()
        if (collapseAfter) expanded = false
    }

    // 展开后 5 秒无操作自动收起；每次点击面板按钮都重置倒计时
    LaunchedEffect(expanded, lastActionAt) {
        if (expanded) {
            delay(5000)
            expanded = false
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.End,
        modifier = modifier.padding(4.dp)
    ) {
        if (expanded) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 2.dp
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ZoomButton("＋", stringResource(R.string.zoom_in)) { tap(onZoomIn) }
                        ZoomButton("－", stringResource(R.string.zoom_out)) { tap(onZoomOut) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ZoomButton("适", stringResource(R.string.zoom_fit)) { tap(onFit, collapseAfter = true) }
                        ZoomButton("中", stringResource(R.string.zoom_center)) { tap(onCenter) }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
        FilledIconButton(
            onClick = {
                lastActionAt = System.currentTimeMillis()
                expanded = !expanded
            },
            modifier = Modifier
                .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                .semantics {
                    contentDescription = if (expanded) {
                        collapseDesc
                    } else {
                        expandDesc
                    }
                },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(text = "⤢", fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun ZoomButton(glyph: String, description: String, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier
            .sizeIn(minWidth = scaledDp(40.dp), minHeight = scaledDp(40.dp))
            .semantics { contentDescription = description },
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = glyph, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
