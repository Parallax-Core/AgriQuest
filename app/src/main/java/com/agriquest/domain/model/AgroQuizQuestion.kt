package com.agriquest.domain.model

/**
 * Domain model representing a daily agriculture quiz question.
 */
data class AgroQuizQuestion(
    val id: String,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String,
    val xpReward: Int = 30
) {
    companion object {
        val QUIZ_POOL: List<AgroQuizQuestion> = listOf(
            AgroQuizQuestion(
                id = "quiz_soil_moisture",
                questionText = "What is the ideal soil moisture range for growing tomatoes?",
                options = listOf("10-30%", "60-80%", "90-100%", "40-50%"),
                correctOptionIndex = 1,
                explanation = "Tomatoes thrive in soil moisture between 60-80%. Lower moisture causes wilting, while higher moisture causes root rot.",
                xpReward = 30
            ),
            AgroQuizQuestion(
                id = "quiz_light_spectrum",
                questionText = "Which light spectrum promotes strong vegetative tomato plant growth?",
                options = listOf("Blue Light", "Green Light", "Infrared Light", "Far-Red Light"),
                correctOptionIndex = 0,
                explanation = "Blue light (400-500 nm) promotes strong vegetative growth, leaf development, and robust stem structure.",
                xpReward = 30
            ),
            AgroQuizQuestion(
                id = "quiz_blossom_end_rot",
                questionText = "What primary nutrient deficiency causes Blossom End Rot in tomatoes?",
                options = listOf("Calcium Deficiency", "Nitrogen Deficiency", "Potassium Deficiency", "Iron Deficiency"),
                correctOptionIndex = 0,
                explanation = "Blossom End Rot is caused by localized calcium deficiency in rapidly expanding fruit tissue.",
                xpReward = 30
            ),
            AgroQuizQuestion(
                id = "quiz_watering_time",
                questionText = "When is the best time of day to water outdoor plants?",
                options = listOf("Early Morning", "Midday Heat", "Late Evening", "Midnight"),
                correctOptionIndex = 0,
                explanation = "Watering in the early morning minimizes evaporation and allows foliage to dry before evening, preventing disease.",
                xpReward = 30
            ),
            AgroQuizQuestion(
                id = "quiz_pruning_benefits",
                questionText = "Why is pruning yellowing bottom leaves beneficial for tomato plants?",
                options = listOf("Improves Airflow & Prevents Disease", "Makes Plant Shorter", "Increases Fruit Size Only", "No Benefit"),
                correctOptionIndex = 0,
                explanation = "Pruning lower yellow leaves improves air circulation around the base and prevents soil-borne fungal spores from spreading.",
                xpReward = 30
            ),
            AgroQuizQuestion(
                id = "quiz_capacitive_sensor",
                questionText = "Which sensor is used to measure soil moisture in automated farming?",
                options = listOf("Capacitive Soil Moisture Sensor", "Ultrasonic Sensor", "Gyroscope", "Barometer"),
                correctOptionIndex = 0,
                explanation = "Capacitive soil moisture sensors measure soil dielectric permittivity to accurately monitor soil water content.",
                xpReward = 30
            )
        )
    }
}
