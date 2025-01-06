package com.masonk.weather

data class Forecast(
    val forecastDate: String, // 예보날짜
    val forecastTime: String, // 예보시간
    var precipitationPercentage: Int = 0, // 강수확률
    var precipitationType: String = "", // 강수형태
    var sky: String = "", // 하늘상태
    var temperature: Double = 0.0, // // 1시간 기온
) {
    val weather: String
        get() { // getter
            // 강수형태가 없거나 '없음'이면
            return if (precipitationType == "" || precipitationType == "없음") {
                // 하늘상태 반환
                sky
            } else {
                // 강수형태 반환
                precipitationType
            }
        }
}
