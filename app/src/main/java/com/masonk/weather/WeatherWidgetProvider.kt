package com.masonk.weather

import android.app.ForegroundServiceStartNotAllowedException
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        appWidgetIds.forEach { appWidgetId ->
            // PendingIntent 객체
            // 나중에 실행될 Intent를 캡슐화
            val pendingServiceIntent: PendingIntent =
                Intent(context, UpdateWeatherService::class.java).let { intent ->
                    // PendingIntent 생성
                    PendingIntent.getForegroundService( // 나중에 특정 Foreground Service를 실행할 수 있는 PendingIntent를 반환
                        context,
                        1, // PendingIntent를 구분하는 데 사용
                        intent, // 실행할 특정 Service를 지정하는 Intent 객체
                        PendingIntent.FLAG_IMMUTABLE // PendingIntent가 변경되지 않도록 설정
                    )
                }

            val views: RemoteViews = RemoteViews(
                context.packageName, // 현재 패키지 이름을 사용하여 RemoteViews 객체 생성
                R.layout.widget_weather // 위젯 레이아웃 리소스 지정
            ).apply {
                // 특정 뷰에 PendingIntent 설정
                // 클릭했을 때 pendingIntent에 정의된 MainActivity가 실행
                setOnClickPendingIntent(R.id.temperature_text_view, pendingServiceIntent)
            }

            // AppWidgetManager를 사용하여 위젯 업데이트
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // 포그라운드 서비스 시작
        val serviceIntent = Intent(context, UpdateWeatherService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: ForegroundServiceStartNotAllowedException) {
                e.printStackTrace()
            }
        } else {
            ContextCompat.startForegroundService(context, serviceIntent)
        }

    }
}