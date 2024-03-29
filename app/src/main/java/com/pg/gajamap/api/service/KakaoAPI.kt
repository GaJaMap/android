package com.pg.gajamap.api.service

import com.pg.gajamap.data.response.ResultSearchCoord2addressData
import com.pg.gajamap.data.response.ResultSearchKeywordData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoAPI {
    @GET("v2/local/search/keyword.json")      // Keyword.json의 정보를 받아옴
    fun getSearchKeyword(
        @Header("Authorization") key: String, // 카카오 API 인증키 [필수]
        @Query("query") query: String,        // 검색을 원하는 질의어 [필수]
        @Query("page") page: Int              // 결과 페이지 번호
    ): Call<ResultSearchKeywordData>          // 받아온 정보가 ResultSearchKeywordData 클래스의 구조로 담김

    @GET("v2/local/geo/coord2address.json")
    fun getCoord2address(
        @Header("Authorization") key: String,// 카카오 API 인증키 [필수]
        @Query("x") x: String, // 경도
        @Query("y") y: String, // 위도
    ): Call<ResultSearchCoord2addressData>
}