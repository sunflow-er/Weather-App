package com.masonk.weather

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.masonk.weather.databinding.ActivityMainBinding
import com.masonk.weather.databinding.ItemChildForecastBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // 위치 권한을 요청하고 결과를 처리하는 콜백 등록
    private val locationPermissionRequest = registerForActivityResult( // 결과를 처리할 콜백을 등록하는 메서드
        ActivityResultContracts.RequestMultiplePermissions() // 여러 권한을 한 번에 요청할 수 있도록 도와주는 contract
    ) { permissions -> // 사용자가 권한 요청에 응답한 결과를 포함하는 맵
        // 권한이 허용되었는지 확인하고 그에 따른 동작 수행
        when {
            permissions.getOrDefault(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                false
            ) -> { // 대략적인 위치 권한만 허용
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            Thread {
                try {
                    val addressList =
                        Geocoder(this, Locale.KOREA).getFromLocation(it.latitude, it.longitude, 1)

                    runOnUiThread {
                        binding.locationTextView.text = addressList
                            ?.get(0)
                            ?.thoroughfare // 최소 단위 (도로명, 거리명)
                            .orEmpty()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

            // 위치 정보를 바탕으로 날씨 예보 정보 가져오기
            WeatherRepository.getVillageForecast(
                location = it, // 위치 정보
                successCallback = { list -> // 성공 시 실행할 콜백
                    // 현재 날씨 정보는 리스트의 첫 번째 값
                    val currentForecast = list.first()

                    // UI
                    binding.temperatureTextView.text =
                        getString(R.string.temperature_text, currentForecast.temperature)
                    binding.weatherTextView.text = currentForecast.weather // 하늘상태 & 강수형태
                    binding.precipitationPercentageTextView.text =
                        getString(
                            R.string.precipitation_text,
                            currentForecast.precipitationPercentage
                        )
                    binding.childForecastLayout.apply {
                        // 레이아웃에 뷰를 추가하는 방식
                        list.forEachIndexed { index, forecast ->
                            // 현재 날씨의 경우 무시
                            if (index == 0) {
                                return@forEachIndexed
                            }

                            // 그 외 다른 날씨 정보, 뷰 생성 및 데이터 할당
                            val childForecastItemView =
                                ItemChildForecastBinding.inflate(layoutInflater)
                            childForecastItemView.apply {
                                timeTextView.text = forecast.forecastTime
                                weatherTextView.text = forecast.weather
                                temperatureTextView.text =
                                    getString(R.string.temperature_text, forecast.temperature)
                            }

                            // 레이아웃에 뷰 추가
                            addView(childForecastItemView.root)
                        }
                    }
                },
                failureCallback = { // 실패 시 실행할 콜백
                    it.printStackTrace()
                }
            )
        }
    }


}