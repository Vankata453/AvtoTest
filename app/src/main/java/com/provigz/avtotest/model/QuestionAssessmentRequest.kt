package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName
import com.provigz.avtotest.db.entity.QuestionState

data class QuestionAssessmentRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("answers") var answers: List<AnswerAssessmentRequest>
) {
    constructor(questionState: QuestionState) : this(
        id = questionState.questionID,
        answers = emptyList()
    ) {
        questionState.answerIDs.forEachIndexed { answerIndex, answerID ->
            answers += AnswerAssessmentRequest(
                id = answerID,
                checked = answerIndex in questionState.stateSelectedAnswerIndexes
            )
        }
    }
}