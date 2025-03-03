package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName
import com.provigz.avtotest.db.entity.TestSetQueried

data class TestSetAssessmentRequest(
    @SerializedName("questions") var questions: List<QuestionAssessmentRequest>
) {
    constructor(testSet: TestSetQueried) : this(
        questions = emptyList()
    ) {
        testSet.questions.forEach { question ->
            questions += QuestionAssessmentRequest(question.state)
        }
    }
}