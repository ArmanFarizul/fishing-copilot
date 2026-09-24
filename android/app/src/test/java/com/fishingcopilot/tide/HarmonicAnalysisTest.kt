package com.fishingcopilot.tide

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class HarmonicAnalysisTest {
    private val epoch = 1_767_225_600_000L // 2026-01-01T00:00Z
    private val hourMs = 3_600_000L

    private val truth = TideModel(
        epochMillis = epoch,
        meanLevel = 1.2,
        constants = listOf(
            HarmonicConstant(Constituent.M2, amplitude = 1.0, phaseDegrees = 40.0),
            HarmonicConstant(Constituent.S2, amplitude = 0.3, phaseDegrees = 200.0),
            HarmonicConstant(Constituent.K1, amplitude = 0.5, phaseDegrees = 120.0),
            HarmonicConstant(Constituent.O1, amplitude = 0.35, phaseDegrees = 300.0)
        )
    )

    private fun hourlyYear(noise: Double = 0.0, keep: Double = 1.0, seed: Int = 7): List<SeaLevelSample> {
        val random = Random(seed)
        return (0 until 365 * 24)
            .filter { random.nextDouble() < keep }
            .map { hour ->
                val t = epoch + hour * hourMs
                SeaLevelSample(t, truth.heightAt(t) + noise * random.nextGaussian())
            }
    }

    private fun Random.nextGaussian(): Double {
        // Box-Muller; good enough for synthetic test noise.
        val u1 = nextDouble().coerceAtLeast(1e-12)
        val u2 = nextDouble()
        return kotlin.math.sqrt(-2 * kotlin.math.ln(u1)) * kotlin.math.cos(2 * Math.PI * u2)
    }

    private fun assertRecovers(model: TideModel, amplitudeTolerance: Double, phaseTolerance: Double) {
        assertEquals(truth.meanLevel, model.meanLevel, amplitudeTolerance)
        truth.constants.forEach { expected ->
            val actual = model.constants.first { it.constituent == expected.constituent }
            assertEquals("${expected.constituent} amplitude", expected.amplitude, actual.amplitude, amplitudeTolerance)
            val phaseError = ((actual.phaseDegrees - expected.phaseDegrees + 540) % 360) - 180
            assertEquals("${expected.constituent} phase", 0.0, phaseError, phaseTolerance)
        }
    }

    @Test
    fun `recovers exact constants from a clean hourly year`() {
        val model = HarmonicAnalysis.fit(hourlyYear(), epoch)
        assertRecovers(model, amplitudeTolerance = 1e-6, phaseTolerance = 1e-4)
        model.constants
            .filter { c -> truth.constants.none { it.constituent == c.constituent } }
            .forEach { assertEquals("${it.constituent} should be absent", 0.0, it.amplitude, 1e-6) }
    }

    @Test
    fun `tolerates noise and missing hours`() {
        val model = HarmonicAnalysis.fit(hourlyYear(noise = 0.1, keep = 0.6), epoch)
        assertRecovers(model, amplitudeTolerance = 0.01, phaseTolerance = 1.0)
    }

    @Test
    fun `predicts the fitted year back within a millimetre`() {
        val samples = hourlyYear()
        val model = HarmonicAnalysis.fit(samples, epoch)
        samples.forEach { assertEquals(it.height, model.heightAt(it.epochMillis), 1e-3) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `refuses to fit with fewer samples than unknowns`() {
        HarmonicAnalysis.fit(hourlyYear().take(10), epoch)
    }
}
