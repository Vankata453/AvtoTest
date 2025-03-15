package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName

data class TestSetRequest(
    @SerializedName("subCategoryId") val subCategoryID: TestSetCategory,
    @SerializedName("learningPlanId") val learningPlanID: Int,
    @SerializedName("languageId") val languageID: Int
)