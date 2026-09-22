package com.olafsapp.gsearch14.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * Shape library built on `androidx.graphics.shapes`.
 *
 * These rounded polygons are what gives the app its own silhouette: the engine badge is a
 * soft seven-sided slab rather than a circle, and the search indicator morphs between two
 * of them while a page loads.
 */
object Polygons {

    /** The signature badge shape — a heptagon rounded almost, but not quite, to a circle. */
    val badge: RoundedPolygon by lazy {
        RoundedPolygon(
            numVertices = 7,
            radius = 1f,
            centerX = 0f,
            centerY = 0f,
            rounding = CornerRounding(0.42f),
        )
    }

    /** A softly rounded square used as the resting state of the loading indicator. */
    val slab: RoundedPolygon by lazy {
        RoundedPolygon(
            numVertices = 4,
            radius = 1f,
            centerX = 0f,
            centerY = 0f,
            rounding = CornerRounding(0.4f),
        )
    }

    /** The bloom the loading indicator morphs into. */
    val bloom: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 8,
            radius = 1f,
            innerRadius = 0.78f,
            rounding = CornerRounding(0.25f),
            centerX = 0f,
            centerY = 0f,
        )
    }
}

/**
 * Draws a [RoundedPolygon] as a Compose [Shape], scaled to fill the element.
 *
 * Each call returns a fresh [Path]. Callers such as `Modifier.background` and
 * `Modifier.border` hold on to the outline they are given, so handing them a shared
 * mutable path lets one of them corrupt the other's geometry.
 */
class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        path.addPath(polygon.toPath().asComposePath())
        path.transform(fitToSize(size))
        return Outline.Generic(path)
    }
}

/**
 * A [Shape] that interpolates between two polygons.
 *
 * [progress] is read on every outline pass, so animating it re-shapes the element without
 * recomposing anything around it.
 */
class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        path.addPath(morph.toPath(progress).asComposePath())
        path.transform(fitToSize(size))
        return Outline.Generic(path)
    }
}

/**
 * Polygons are authored in a -1..1 space centred on the origin; this maps that space onto
 * the element's actual bounds.
 */
private fun fitToSize(size: Size): Matrix = Matrix().apply {
    scale(size.width / 2f, size.height / 2f)
    translate(1f, 1f)
}

/** Convenience for the common "morph between two named polygons" case. */
fun morphOf(start: RoundedPolygon, end: RoundedPolygon): Morph = Morph(start, end)
