package com.sounddeck.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.sounddeck.core.model.GridPosition

class GridPositionModifier(
    val position: GridPosition
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = position
}

fun Modifier.gridPosition(position: GridPosition) = this.then(GridPositionModifier(position))
fun Modifier.gridPosition(row: Int, col: Int, rowSpan: Int = 1, colSpan: Int = 1) =
    this.then(GridPositionModifier(GridPosition(row, col, rowSpan, colSpan)))

/**
 * StaticSpanGrid: A high-performance, strictly zero-margin 2D layout engine for SoundDeck pads.
 * Measured and placed with fixed constraints per cell based on (row, col) and (rowSpan, colSpan).
 */
@Composable
fun StaticSpanGrid(
    rows: Int,
    cols: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)

    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val cellWidth = constraints.maxWidth / safeCols
        val cellHeight = constraints.maxHeight / safeRows

        val placeables = measurables.map { measurable ->
            val position = measurable.parentData as? GridPosition
                ?: GridPosition(row = 0, col = 0, rowSpan = 1, colSpan = 1)

            val width = (cellWidth * position.colSpan).coerceAtMost(constraints.maxWidth)
            val height = (cellHeight * position.rowSpan).coerceAtMost(constraints.maxHeight)

            val placeable = measurable.measure(
                Constraints.fixed(
                    width = width.coerceAtLeast(0),
                    height = height.coerceAtLeast(0)
                )
            )
            Triple(placeable, position.col * cellWidth, position.row * cellHeight)
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEach { (placeable, x, y) ->
                placeable.placeRelative(x = x, y = y)
            }
        }
    }
}
