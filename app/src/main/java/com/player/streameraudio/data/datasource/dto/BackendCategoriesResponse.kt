package com.player.streameraudio.data.datasource.dto

import com.google.gson.annotations.SerializedName

data class BackendCategoriesResponse(
    @SerializedName("categories") val categories: List<BackendCategoryDTO> = emptyList(),
    @SerializedName("pagination") val pagination: TwitchPaginationDTO? = null
)