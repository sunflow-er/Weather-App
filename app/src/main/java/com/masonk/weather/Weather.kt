package com.masonk.weather

import com.google.gson.annotations.SerializedName

data class Weather(
    @SerializedName("response")
    val response: WeatherResponse
)

data class WeatherResponse(
    @SerializedName("header")
    val header: WeatherHeader,
    @SerializedName("body")
    val body: WeatherBody
)

data class WeatherHeader(
    @SerializedName("resultCode")
    val resultCode: String,
    @SerializedName("resultMsg")
    val resultMessage: String,
)

data class WeatherBody(
    @SerializedName("items")
    val items: WeatherItems
)

data class WeatherItems(
    @SerializedName("item")
    val item : List<WeatherForecast>
)

data class WeatherForecast(
    @SerializedName("baseDate")
    val baseDate: String,
    @SerializedName("baseTime")
    val baseTime: String,
    @SerializedName("category")
    val category: Category?, // JSON 데이터의 category 필드 값이 Category 열거형 값으로 자동 변환됨
    @SerializedName("fcstDate")
    val forecastDate: String,
    @SerializedName("fcstTime")
    val forecastTime: String,
    @SerializedName("fcstValue")
    val forecastValue: String,
    @SerializedName("nx")
    val nx: Int,
    @SerializedName("ny")
    val ny: Int,
)

