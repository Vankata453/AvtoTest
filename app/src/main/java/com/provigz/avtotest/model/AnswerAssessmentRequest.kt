package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName

data class AnswerAssessmentRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("checked") val checked: Boolean
)