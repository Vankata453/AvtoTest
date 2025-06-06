package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName

data class Answer(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String?,
    @SerializedName("pictureId") val pictureID: String?,
    @SerializedName("correct") val correct: Boolean?,
    @SerializedName("checked") val checked: Boolean?
)

data class AnswerAssessed(
    @SerializedName("id") val id: Int,
    @SerializedName("correct") val correct: Boolean
)