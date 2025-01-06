package com.masonk.weather

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    // 위치 권한을 요청하고 결과를 처리하는 콜백 등록
    val locationPermissionRequest = registerForActivityResult( // 결과를 처리할 콜백을 등록하는 메서드
        ActivityResultContracts.RequestMultiplePermissions() // 여러 권한을 한 번에 요청할 수 있도록 도와주는 contract
    ) { permissions -> // 사용자가 권한 요청에 응답한 결과를 포함하는 맵
        // 권한이 허용되었는지 확인하고 그에 따른 동작 수행
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> { // 대략적인 위치 권한만 허용
                // 마지막 위치 정보 업데이트
                updateLocation()
            }

            else -> { // 위치 권한 허용하지 않음
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()

                // 앱 설정 화면으로 이동
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    // 해당 애플리케이션의 설정 화면으로 이동
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)

                // 앱 종료
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 위치 권한 요청
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )


    }

    private fun updateLocation() {
        // 위치 정보를 요청하고 받을 수 있는 FusedLocationProviderClient 객체
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 위치 권한 허용 상태 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) { // 권한 허용 안되어있으면
            // 위치 권한 요청
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

            return
        }

        // 마지막으로 알려진 위치 정보 가져오기
        fusedLocationClient.lastLocation.addOnSuccessListener { // 성공하면
            // 위치 정보를 바탕으로 날씨 예보 정보 가져오기
            fetchForecast(it)
        }
    }

    private fun fetchForecast(location: Location) {
        // retrofit 객체
        val retrofit =
            Retrofit.Builder()
                .baseUrl("http://apis.data.go.kr/") // http -> manifest에서 권한 허용 필요
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        // 네트워크 서비스 구현체
        val weatherService = retrofit.create(WeatherService::class.java)

        // 현재 시간을 바탕으로 BaseDateTime 얻기
        val baseDateTime = BaseDateTime.getBaseDateTime()

        // 위경도 값을 x, y좌표로 변환하는 컨버터 객체
        val converter = GeoPointConverter()

        // 위경도를 바탕으로 변환된 x,y 좌표값을 담고 있는 Point 객체
        val point = converter.convert(lat = location.latitude, lon = location.longitude)

        // 현재 위치의 현재 시간을 기준으로 날씨 예보 정보 가져오기
        weatherService.getVillageForecast(
            serviceKey = "KKL5lWfpwwpCEOTbj8WP303rixDkhtDFIO0IX1WPH8bPTofZ9spW795/KdJIiSOlkkkFIargPbqV5CQngoEZFg==",
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
            }

            override fun onFailure(p0: Call<Weather>, p1: Throwable) {
                p1.printStackTrace()
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