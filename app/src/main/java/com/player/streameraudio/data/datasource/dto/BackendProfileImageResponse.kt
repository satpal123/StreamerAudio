package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class BackendProfileImageResponse(
    @SerializedName("data") val data: List<BackendUserDTO>
)