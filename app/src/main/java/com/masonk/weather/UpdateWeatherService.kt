package com.masonk.weather

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.view.LayoutInflater
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.masonk.weather.databinding.WidgetWeatherBinding

// 위젯 업데이트: AppWidgetManager를 사용하여 날씨 위젯을 업데이트
// 위치 정보 확인: 위치 권한을 확인하고, 마지막으로 알려진 위치 정보를 가져옴
// 날씨 정보 가져오기: WeatherRepository를 통해 날씨 예보 정보를 가져옴
// UI 업데이트: 가져온 날씨 정보를 RemoteViews를 통해 위젯의 UI에 반영
// 클릭 이벤트 설정: 위젯의 특정 뷰에 클릭 이벤트를 설정하여, 사용자가 클릭했을 때 UpdateWeatherService가 다시 실행되도록 함
// 서비스 종료: 작업이 완료되면 stopSelf()를 호출하여 서비스를 종료
class UpdateWeatherService : Service() {
    // 클라이언트가 서비스에 바인딩할 때 호출
    // 바인딩은 클라이언트가 서비스와 통신할 수 있도록 연결하는 것을 의미
    // 바인딩된 서비스는 클라이언트가 서비스의 메서드를 호출하여 직접 상호작용
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    // 서비스가 startService 메서드를 통해 시작될 때 호출
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // 채널 생성 및 시스템에 등록
        createChannel()

        // 포그라운드 서비스 시작
        // 서비스가 포그라운드에서 실행되도록 설정
        startForeground(
            1, // 알림의 ID
            createNotification() // Notification 객체, 포그라운드 서비스가 실행 중임을 사용자에게 알림
        )

        // 위젯 매니저, 위젯을 관리하고 업데이트하는 데 사용
        val widgetManager: AppWidgetManager = AppWidgetManager.getInstance(this)

        // 위치 권한 허용 상태 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) { // 권한 허용 안되어있으면
            // 위젯을 권한없음 상태로 표시하고, 클릭했을 때 권한 팝업을 얻을 수 있도록 조정

            val pendingIntent = Intent(this, SettingActivity::class.java).let {
                PendingIntent.getActivity(this, 2, it, PendingIntent.FLAG_IMMUTABLE)
            }

            stopSelf()

            RemoteViews(packageName, R.layout.widget_weather).apply {
                setTextViewText(R.id.temperature_text_view, "권한 없음")
                setTextViewText(R.id.weather_text_view, "")
                setOnClickPendingIntent(R.id.temperature_text_view, pendingIntent)
            }.also { remoteViews ->
                // 특정 컴포넌트(WeatherWidgetProvider)를 식별하는 데 사용
                val widgetName = ComponentName(this, WeatherWidgetProvider::class.java)

                // 지정된 위젯을 remoteViews로 업데이트
                widgetManager.updateAppWidget(widgetName, remoteViews)
            }

            // 기본 동작을 수행하고 종료
            return super.onStartCommand(intent, flags, startId)
        }

        // // 마지막으로 알려진 위치 정보 가져오기
        LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {
            // 위치 정보를 성공적으로 가져왔을 때
            WeatherRepository.getVillageForecast(
                location = it,
                successCallback = { list ->
                    // 날씨 예보 정보를 성공적으로 가져왔을 때

                    val currentForecast = list.first()

                    // UpdateWeatherService를 호출하는 PendingIntent 생성
                    val pendingServiceIntent: PendingIntent =
                        Intent(this, UpdateWeatherService::class.java).let { intent ->
                            PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
                        }

                    RemoteViews(packageName, R.layout.widget_weather).apply {
                        // 온도 텍스트 뷰에 현재 온도 설정
                        setTextViewText(
                            R.id.temperature_text_view,
                            getString(R.string.temperature_text, currentForecast.temperature)
                        )

                        // 날씨 텍스트 뷰에 현재 날씨 설정
                        setTextViewText(
                            R.id.weather_text_view,
                            currentForecast.weather
                        )

                        // 온도 텍스트 뷰에 클릭 이벤트 설정
                        // 뷰를 클릭했을 때 PendingIntent에 정의된 작업 실행
                        setOnClickPendingIntent(R.id.temperature_text_view, pendingServiceIntent)
                    }.also { remoteViews ->
                        // 특정 컴포넌트(WeatherWidgetProvider)를 식별하는 데 사용
                        val widgetName = ComponentName(this, WeatherWidgetProvider::class.java)

                        // 지정된 위젯을 remoteViews로 업데이트
                        widgetManager.updateAppWidget(widgetName, remoteViews)
                    }

                    // 서비스 중지
                    stopSelf()
                }, failureCallback = {
                    // 위젯을 에러 상태로 표시

                    val pendingServiceIntent: PendingIntent =
                        Intent(this, UpdateWeatherService::class.java).let { intent ->
                            PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_IMMUTABLE)
                        }

                    RemoteViews(packageName, R.layout.widget_weather).apply {
                        setTextViewText(
                            R.id.temperature_text_view,
                            "에러"
                        )

                        setTextViewText(
                            R.id.weather_text_view,
                            ""
                        )

                        // 온도 텍스트 뷰에 클릭 이벤트 설정
                        // 뷰를 클릭했을 때 PendingIntent에 정의된 작업 실행
                        setOnClickPendingIntent(R.id.temperature_text_view, pendingServiceIntent)
                    }.also { remoteViews ->
                        // 특정 컴포넌트(WeatherWidgetProvider)를 식별하는 데 사용
                        val widgetName = ComponentName(this, WeatherWidgetProvider::class.java)

                        // 지정된 위젯을 remoteViews로 업데이트
                        widgetManager.updateAppWidget(widgetName, remoteViews)
                    }

                    // 서비스 중지
                    stopSelf()
                }
            )
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        // 포그라운드 서비스 종료
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    // 채널 생성
    private fun createChannel() {
        // 채널 객체 생성
        val channel = NotificationChannel(
            getString(R.string.notification_channel_id), // id
            getString(R.string.notification_channel_name), // name
            NotificationManager.IMPORTANCE_LOW // importance
        )
        // description
        channel.description = getString(R.string.notification_channel_description)

        // 채널매니저
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 매니저를 통해 채널 생성
        manager.createNotificationChannel(channel)
    }

    // 알림 생성
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.baseline_circle_notifications_24)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .build()
    }
}