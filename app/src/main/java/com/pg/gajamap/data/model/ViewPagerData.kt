package com.pg.gajamap.data.model

data class ViewPagerData(
    val clientId: Long,
    val groupId: Long,
    val profileImg: String,
    var name: String,
    var address: String,
    var detail: String,
    var phoneNumber: String,
    var distance: Double?=null,
    var latitude: Double?= null,
    var longitude: Double? = null
)
