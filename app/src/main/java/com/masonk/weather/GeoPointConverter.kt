package com.masonk.weather

import android.os.Build.VERSION_CODES.P
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

class GeoPointConverter {
    private val NX = 149 // x축 격자점 수
    private val NY = 253 // y축 격자점 수

    private val RE = 6371.00877 // 지도 반경
    private val GRID = 5.0 // 격자 간격 (km)
    private val SLAT1 = 30.0 // 표준 위도 1
    private val SLAT2 = 60.0 // 표준 위도 2
    private val OLON = 126.0 // 기준점 경도
    private val OLAT = 38.0 // 기준점 위도
    private val XO = 210 / GRID // 기준점 X좌표
    private val YO = 675 / GRID // 기준점 Y좌표

    private val DEGRAD = PI / 180.0
    private val RADDEG = 180.0 / PI

    private val re = RE / GRID
    private val slat1 = SLAT1 * DEGRAD
    private val slat2 = SLAT2 * DEGRAD
    private val olon = OLON * DEGRAD
    private val olat = OLAT * DEGRAD

    // x, y 좌표값을 담는 Point 클래스
    data class Point(
        val x: Int,
        val y: Int
    )

    // 위경도를 x,y 좌표로 변환하여 Point값을 반환하는 함수
    fun convert(lat: Double, lon: Double) : Point {
        var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
        sn = log2(cos(slat1) / cos(slat2)) / log2(sn)

        var sf = tan(PI * 0.25 + slat1 * 0.5)
        sf = sf.pow(sn) * cos(slat1) /sn
        var ro = tan(PI * 0.25 + olat * 0.5)
        ro = re * sf / ro.pow(sn)

        var ra = tan(PI * 0.25 + lat * DEGRAD * 0.5)
        ra = re * sf /ra.pow(sn)

        var theta = lon * DEGRAD - olon

        if (theta > PI) {
            theta -= 2 * PI
        }
        if (theta < -PI) {
            theta += 2 * PI
        }

        theta *= sn

        val x = ra * sin(theta) + XO + 1.5
        val y = ro - ra * cos(theta) + YO + 1.5

        return Point(x.toInt(), y.toInt())
    }
}