package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName

data class Question(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String,
    @SerializedName("thumbnailId") val thumbnailID: String,
    @SerializedName("pictureId") val pictureID: String,
    @SerializedName("videoFileId") val videoID: String,
    @SerializedName("points") val points: Int,
    @SerializedName("correctAnswersCount") val correctAnswers: Int,
    @SerializedName("answers") val answers: Array<Answer>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Question

        if (id != other.id) return false
        if (text != other.text) return false
        if (thumbnailID != other.thumbnailID) return false
        if (pictureID != other.pictureID) return false
        if (videoID != other.videoID) return false
        if (points != other.points) return false
        if (correctAnswers != other.correctAnswers) return false
        if (!answers.contentEquals(other.answers)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + text.hashCode()
        result = 31 * result + thumbnailID.hashCode()
        result = 31 * result + pictureID.hashCode()
        result = 31 * result + videoID.hashCode()
        result = 31 * result + points
        result = 31 * result + correctAnswers
        result = 31 * result + answers.contentHashCode()
        return result
    }
}