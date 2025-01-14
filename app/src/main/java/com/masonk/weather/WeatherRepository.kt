package com.masonk.weather

import android.location.Location
import com.masonk.weather.databinding.ItemChildForecastBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WeatherRepository {
    // retrofit 객체
    private val retrofit =
        Retrofit.Builder()
            .baseUrl("http://apis.data.go.kr/") // http -> manifest에서 권한 허용 필요
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // 네트워크 서비스 구현체
    private val weatherService = retrofit.create(WeatherService::class.java)

    fun getVillageForecast(
        location: Location,
        successCallback: (List<Forecast>) -> Unit,
        failureCallback: (Throwable) -> Unit,
    ) {
        // 현재 시간을 바탕으로 BaseDateTime 얻기
        val baseDateTime = BaseDateTime.getBaseDateTime()

        // 위경도 값을 x, y좌표로 변환하는 컨버터 객체
        val converter = GeoPointConverter()

        // 위경도를 바탕으로 변환된 x,y 좌표값을 담고 있는 Point 객체
        val point = converter.convert(lat = location.latitude, lon = location.longitude)

        // 현재 위치의 현재 시간을 기준으로 날씨 예보 정보 가져오기
        weatherService.getVillageForecast(
            serviceKey = "-",
            baseDate = baseDateTime.baseDate,
            baseTime = baseDateTime.baseTime,
            nx = point.x,
            ny = point.y
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

                    // 카테고리에 따른 해당 카테고리 정보 얻기
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

                // 맵의 값 리스트
                val list = forecastDateTimeMap.values.toMutableList()

                // 예보 날짜 및 시간을 기준으로 정렬
                list.sortWith { f1, f2 ->
                    val f1DateTime = "${f1.forecastDate}${f1.forecastTime}"
                    val f2DateTime = "${f2.forecastDate}${f2.forecastTime}"

                    return@sortWith f1DateTime.compareTo(f2DateTime)
                }

                if (list.isEmpty()) {
                    failureCallback(NullPointerException())
                } else {
                    // 콜백 메서드 호출
                    successCallback(list)
                }
            }

            override fun onFailure(p0: Call<Weather>, p1: Throwable) {
                // 콜백 메서드 호출
                failureCallback(p1)
            }

        })
    }

    // 정수형 forecastValue로 부터 강수형태 문자열 정보 얻기
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

    // 정수형 forecastValue로 부터 하늘상태 문자열 정보 얻기
    private fun transformSkyType(forecast: WeatherForecast): String {
        return when (forecast.forecastValue.toInt()) {
            1 -> "맑음"
            3 -> "구름많음"
            4 -> "흐림"
            else -> ""
        }
    }
}