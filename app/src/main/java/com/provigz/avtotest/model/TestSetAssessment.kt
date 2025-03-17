package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName

data class TestSetAssessment(
    @SerializedName("result") val result: TestSetAssessmentResult,
    @SerializedName("testSet") val testSet: TestSetAssessed
)

data class TestSetAssessmentFull(
    @SerializedName("result") val result: TestSetAssessmentResult,
    @SerializedName("testSet") val testSet: TestSet
)