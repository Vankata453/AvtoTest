package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName

data class TestSetAssessmentResult(
    @SerializedName("id") val id: Int,
    @SerializedName("receivedPoints") val receivedPoints: Int,
    @SerializedName("totalPoints") val totalPoints: Int,
    @SerializedName("correctQuestionsCount") val correctQuestionsCount: Int,
    @SerializedName("incorrectQuestionsCount") val incorrectQuestionsCount: Int,
    @SerializedName("passed") val passed: Boolean
)