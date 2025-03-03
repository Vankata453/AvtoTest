package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName

data class Question(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String,
    @SerializedName("thumbnailId") val thumbnailID: String?,
    @SerializedName("pictureId") val pictureID: String?,
    @SerializedName("videoFileId") val videoID: Int?,
    @SerializedName("points") val points: Int,
    @SerializedName("correctAnswersCount") val correctAnswers: Int,
    @SerializedName("answers") val answers: Array<Answer>
)

data class QuestionAssessed(
    @SerializedName("id") val id: Int,
    @SerializedName("answers") val answers: Array<AnswerAssessed>
)