package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName

data class TestSetSubCategory(
    @SerializedName("id") val id: Int,
    @SerializedName("categoryId") val categoryID: Int,
    @SerializedName("name") val name: String,
    // learningPlans
    @SerializedName("examDurationMinutes") val durationMinutes: Int
)