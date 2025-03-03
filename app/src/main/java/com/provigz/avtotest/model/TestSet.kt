package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName

data class TestSet(
    @SerializedName("id") val id: Int,
    @SerializedName("subCategory") val subCategory: TestSetSubCategory,
    // languageId, learningPlanId
    @SerializedName("questions") val questions: Array<Question>
)

data class TestSetAssessed(
    @SerializedName("id") val id: Int,
    @SerializedName("questions") val questions: Array<QuestionAssessed>
)