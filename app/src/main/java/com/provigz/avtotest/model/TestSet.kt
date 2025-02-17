package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName

data class TestSet(
    @SerializedName("id") val id: Int,
    @SerializedName("subCategory") val subCategory: TestSetSubCategory,
    // languageId, learningPlanId
    @SerializedName("questions") val questions: Array<Question>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestSet

        if (id != other.id) return false
        if (!questions.contentEquals(other.questions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + questions.contentHashCode()
        return result
    }
}