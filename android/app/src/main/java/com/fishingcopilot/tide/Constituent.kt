package com.fishingcopilot.tide

/**
 * Tidal constituents fitted from one year of hourly sea level.
 * Speeds in degrees per mean solar hour, from NOAA CO-OPS harmonic constituent data
 * (https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/9414290/harcon.json).
 *
 * T2 and S1 are left out: they sit within 0.041 deg/h of S2 and K1, which one year of data
 * cannot separate (Rayleigh criterion 360 / 8766 h).
 */
enum class Constituent(val speedDegreesPerHour: Double) {
    // Semidiurnal
    M2(28.984104), S2(30.0), N2(28.43973), K2(30.082138),
    NU2(28.512583), MU2(27.968208), `2N2`(27.895355), L2(29.528479), LAM2(29.455626),

    // Diurnal
    K1(15.041069), O1(13.943035), P1(14.958931), Q1(13.398661),
    J1(15.5854435), OO1(16.139101), M1(14.496694), `2Q1`(12.854286), RHO(13.471515),

    // Shallow water
    M4(57.96821), MS4(58.984104), MN4(57.423832), M6(86.95232), MK3(44.025173), M3(43.47616),

    // Long period
    MM(0.5443747), MF(1.0980331), MSF(1.0158958), SSA(0.0821373), SA(0.0410686)
}
