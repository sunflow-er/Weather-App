package com.masonk.weather

import gen._base._base_java__assetres.srcjar.R.id.time
import java.time.LocalDateTime
import java.time.LocalTime

data class BaseDateTime (
    val baseDate: String,
    val baseTime: String,
) {
    companion object {
        // 현재 시간을 바탕으로 BaseDateTime을 반환하는 함수
        fun getBaseDateTime(): BaseDateTime {
            // 현재 날짜 및 시간 가져오기
            var dateTime = LocalDateTime.now()

            // baseTime 보정
            val baseTime = when(dateTime.toLocalTime()) { // dateTime에서 날짜 정보는 제외하고 시간 정보만 추출
                in LocalTime.of(0, 0) .. LocalTime.of(2, 30) -> { // 0000 - 0230
                    // 현재 날짜에서 하루 빼기
                    dateTime = dateTime.minusDays(1)

                    // 전날의 2300시를 기준 시간으로 설정
                    "2300"
                }
                in LocalTime.of(2, 30) .. LocalTime.of(5, 30) -> { // 0230 - 0530
                    // 0200시를 기준 시간으로 설정
                    "0200"
                }
                in LocalTime.of(5, 30) .. LocalTime.of(8, 30) -> { // 0530 - 0830
                    // 0500시를 기준 시간으로 설정
                    "0500"
                }
                in LocalTime.of(8, 30) .. LocalTime.of(11, 30) -> { // 0830 - 1130
                    // 0800시를 기준 시간으로 설정
                    "0800"
                }
                in LocalTime.of(11, 30) .. LocalTime.of(14, 30) -> { // 1130 - 1430
                    // 1100시를 기준 시간으로 설정
                    "1100"
                }
                in LocalTime.of(14, 30) .. LocalTime.of(17, 30) -> { // 1430 - 1730
                    // 1400시를 기준 시간으로 설정
                    "1400"
                }
                in LocalTime.of(17, 30) .. LocalTime.of(20, 30) -> { // 1730 - 2030
                    // 1700시를 기준 시간으로 설정
                    "1700"
                }
                in LocalTime.of(20, 30) .. LocalTime.of(23, 30) -> { // 2030 - 2330
                    // 2000시를 기준 시간으로 설정
                    "2000"
                }
                else -> { // 2300 - 0000
                    // 2300시를 기준 시간으로 설정
                    "2300"
                }
            }

            // baseDate 보정
            val baseDate = String.format("%04d%02d%02d", dateTime.year, dateTime.month, dateTime.dayOfMonth)

            // baseDateTime 반환
            return BaseDateTime(baseDate, baseTime)
        }
    }
}