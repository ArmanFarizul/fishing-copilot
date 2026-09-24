package com.fishingcopilot.tide

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Least-squares harmonic analysis: sea level = mean + sum(a cos(wt) + b sin(wt)). */
object HarmonicAnalysis {

    fun fit(
        samples: List<SeaLevelSample>,
        epochMillis: Long,
        constituents: List<Constituent> = Constituent.entries
    ): TideModel {
        val unknowns = 1 + 2 * constituents.size
        require(samples.size > unknowns) { "Need more than $unknowns samples, got ${samples.size}" }

        // Accumulate the normal equations (A^T A) x = A^T y one row at a time,
        // so a year of hourly data never needs the full design matrix in memory.
        val normal = Array(unknowns) { DoubleArray(unknowns) }
        val rhs = DoubleArray(unknowns)
        val row = DoubleArray(unknowns)
        for (sample in samples) {
            val hours = (sample.epochMillis - epochMillis) / 3_600_000.0
            row[0] = 1.0
            constituents.forEachIndexed { i, c ->
                val angle = Math.toRadians(c.speedDegreesPerHour * hours)
                row[1 + 2 * i] = cos(angle)
                row[2 + 2 * i] = sin(angle)
            }
            for (r in 0 until unknowns) {
                rhs[r] += row[r] * sample.height
                for (k in r until unknowns) normal[r][k] += row[r] * row[k]
            }
        }
        for (r in 0 until unknowns) for (k in 0 until r) normal[r][k] = normal[k][r]

        val x = solve(normal, rhs)
        return TideModel(
            epochMillis = epochMillis,
            meanLevel = x[0],
            constants = constituents.mapIndexed { i, c ->
                val a = x[1 + 2 * i]
                val b = x[2 + 2 * i]
                // a cos(wt) + b sin(wt) = A cos(wt - phase)
                HarmonicConstant(c, amplitude = hypot(a, b), phaseDegrees = (Math.toDegrees(atan2(b, a)) + 360) % 360)
            }
        )
    }

    /** Gaussian elimination with partial pivoting. Mutates its arguments. */
    private fun solve(matrix: Array<DoubleArray>, vector: DoubleArray): DoubleArray {
        val n = vector.size
        for (col in 0 until n) {
            val pivot = (col until n).maxBy { abs(matrix[it][col]) }
            matrix[col] = matrix[pivot].also { matrix[pivot] = matrix[col] }
            vector[col] = vector[pivot].also { vector[pivot] = vector[col] }
            for (r in col + 1 until n) {
                val factor = matrix[r][col] / matrix[col][col]
                for (k in col until n) matrix[r][k] -= factor * matrix[col][k]
                vector[r] -= factor * vector[col]
            }
        }
        val x = DoubleArray(n)
        for (r in n - 1 downTo 0) {
            var sum = vector[r]
            for (k in r + 1 until n) sum -= matrix[r][k] * x[k]
            x[r] = sum / matrix[r][r]
        }
        return x
    }
}
