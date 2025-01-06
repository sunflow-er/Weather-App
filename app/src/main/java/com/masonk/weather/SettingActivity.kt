package com.masonk.weather

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.masonk.weather.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding

    // 위치 권한을 요청하고 결과를 처리하는 콜백 등록
    private val locationPermissionRequest = registerForActivityResult( // 결과를 처리할 콜백을 등록하는 메서드
        ActivityResultContracts.RequestMultiplePermissions() // 여러 권한을 한 번에 요청할 수 있도록 도와주는 contract
    ) { permissions -> // 사용자가 권한 요청에 응답한 결과를 포함하는 맵
        // 권한이 허용되었는지 확인하고 그에 따른 동작 수행
        when {
            permissions.getOrDefault(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                false
            ) -> {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, UpdateWeatherService::class.java)
                )
            }

            else -> { // 위치 권한 허용하지 않음
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            )

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            )
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.settingButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
    }


}