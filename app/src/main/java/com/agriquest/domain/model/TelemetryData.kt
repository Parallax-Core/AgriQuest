package com.agriquest.domain.model

/**
 * Domain model representing IoT Telemetry Data collected from plant sensors.
 *
 * @param soilMoisture Soil moisture percentage (0.0% to 100.0%)
 * @param airTemperature Air temperature in degrees Celsius (°C)
 * @param relativeHumidity Relative humidity percentage (0.0% to 100.0%)
 * @param lightIntensity Light intensity in Lux
 */
data class TelemetryData(
    val soilMoisture: Float = 68.0f,
    val airTemperature: Float = 24.5f,
    val relativeHumidity: Float = 62.0f,
    val lightIntensity: Float = 14200.0f
) {
    companion object {
        const val MIN_OPTIMAL_SOIL_MOISTURE = 60.0f
        const val MAX_OPTIMAL_SOIL_MOISTURE = 80.0f

        const val MIN_OPTIMAL_AIR_TEMP = 21.0f
        const val MAX_OPTIMAL_AIR_TEMP = 28.0f

        const val MIN_OPTIMAL_HUMIDITY = 50.0f
        const val MAX_OPTIMAL_HUMIDITY = 70.0f

        const val MIN_OPTIMAL_LIGHT_LUX = 10000.0f
        const val MAX_OPTIMAL_LIGHT_LUX = 50000.0f
    }

    /** Returns whether soil moisture is within optimal range (60-80%). */
    val isSoilMoistureOptimal: Boolean
        get() = soilMoisture in MIN_OPTIMAL_SOIL_MOISTURE..MAX_OPTIMAL_SOIL_MOISTURE

    /** Returns whether air temperature is within optimal range (21°C - 28°C). */
    val isAirTemperatureOptimal: Boolean
        get() = airTemperature in MIN_OPTIMAL_AIR_TEMP..MAX_OPTIMAL_AIR_TEMP

    /** Returns whether relative humidity is within optimal range (50% - 70%). */
    val isHumidityOptimal: Boolean
        get() = relativeHumidity in MIN_OPTIMAL_HUMIDITY..MAX_OPTIMAL_HUMIDITY

    /** Returns whether light intensity is within optimal range (>10000 Lux). */
    val isLightIntensityOptimal: Boolean
        get() = lightIntensity >= MIN_OPTIMAL_LIGHT_LUX

    /** Overall optimal status across all parameters. */
    val isOverallOptimal: Boolean
        get() = isSoilMoistureOptimal && isAirTemperatureOptimal && isHumidityOptimal && isLightIntensityOptimal

    /** Status summary text helper. */
    val statusSummary: String
        get() = when {
            isOverallOptimal -> "All sensor parameters are in optimal range."
            !isSoilMoistureOptimal && soilMoisture < MIN_OPTIMAL_SOIL_MOISTURE -> "Soil moisture low ($soilMoisture%)."
            !isSoilMoistureOptimal && soilMoisture > MAX_OPTIMAL_SOIL_MOISTURE -> "Soil is over-saturated ($soilMoisture%)."
            !isAirTemperatureOptimal -> "Temperature out of optimal bounds ($airTemperature°C)."
            !isHumidityOptimal -> "Humidity out of optimal bounds ($relativeHumidity%)."
            !isLightIntensityOptimal -> "Light intensity out of optimal bounds ($lightIntensity Lux)."
            else -> "Sub-optimal telemetry values detected."
        }
}
