package com.masonk.weather

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val retrofit =
            Retrofit.Builder()
                .baseUrl("http://apis.data.go.kr/") // http -> manifest에서 권한 허용 필요
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        val weatherService = retrofit.create(WeatherService::class.java)

        weatherService.getVillageForecast(
            serviceKey = "KKL5lWfpwwpCEOTbj8WP303rixDkhtDFIO0IX1WPH8bPTofZ9spW795/KdJIiSOlkkkFIargPbqV5CQngoEZFg==",
            baseDate = "20250103",
            baseTime = "2300",
            nx = 55,
            ny = 127
        ).enqueue(object : Callback<Weather> {
            override fun onResponse(p0: Call<Weather>, p1: Response<Weather>) {
                // 예보 날짜 및 시간과 예보 정보를 짝지어 주는 맵
                val forecastDateTimeMap = mutableMapOf<String, Forecast>()

                // 예보 리스트
                val forecastList = p1.body()?.response?.body?.items?.item.orEmpty()

                for (forecast in forecastList) {

                    // 맵에 해당 예보 날짜/시간에 대한 예보 정보가 없다면
                    if (forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] == null) {
                        // 맵에 예보 정보 넣기
                        forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"] =
                            Forecast(
                                forecastDate = forecast.forecastDate,
                                forecastTime = forecast.forecastTime
                            )
                    }

                    forecastDateTimeMap["${forecast.forecastDate}/${forecast.forecastTime}"]?.apply {
                        when (forecast.category) {
                            Category.POP -> precipitationPercentage = forecast.forecastValue.toInt()
                            Category.PTY -> precipitationType = transformRainType(forecast)
                            Category.SKY -> sky = transformSkyType(forecast)
                            Category.TMP -> temperature = forecast.forecastValue.toDouble()
                            else -> {}
                        }
                    }


                }
            }

            override fun onFailure(p0: Call<Weather>, p1: Throwable) {
                p1.printStackTrace()
            }

        })
    }

    private fun transformRainType(forecast: WeatherForecast): String {
        return when (forecast.forecastValue.toInt()) {
            0 -> "없음"
            1 -> "비"
            2 -> "비/눈"
            3 -> "눈"
            4 -> "소나기"
            else -> ""
        }
    }

    private fun transformSkyType(forecast: WeatherForecast): String {
        return when (forecast.forecastValue.toInt()) {
            1 -> "맑음"
            3 -> "구름많음"
            4 -> "흐림"
            else -> ""
        }
    }
}