package com.provigz.avtotest.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.provigz.avtotest.db.TestSetDao
import com.provigz.avtotest.model.QuestionAssessed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Entity(tableName = "question")
data class Question(
    @PrimaryKey val id: Int,
    val text: String,
    val thumbnailID: String?,
    val pictureID: String?,
    val videoID: Int?,
    val points: Int,
    val correctAnswers: Int
) {
    var answerIDs: List<Int> = emptyList()

    constructor(model: com.provigz.avtotest.model.Question) : this(
        id = model.id,
        text = model.text,
        thumbnailID = model.thumbnailID,
        pictureID = model.pictureID,
        videoID = model.videoID,
        points = model.points,
        correctAnswers = model.correctAnswers
    )

    suspend fun insertAnswers(
        testSetDao: TestSetDao,
        answers: Array<com.provigz.avtotest.model.Answer>
    ) {
        answers.forEach { answerModel ->
            testSetDao.insertAnswer(
                Answer(
                    questionID = id,
                    answerModel
                )
            )
            answerIDs += answerModel.id
        }
    }

    suspend fun createState(
        testSetDao: TestSetDao,
        testSetID: Int,
        answers: Array<com.provigz.avtotest.model.Answer>
    ) {
        val state = QuestionState(
            testSetID = testSetID,
            questionID = id,
            answerIDs = answerIDs
        )
        answers.forEachIndexed { answerIndex, answerAssessed ->
            if (answerAssessed.checked == true) {
                state.stateSelectedAnswerIndexes += answerIndex
            }
        }
        testSetDao.insertQuestionState(state)
    }

    suspend fun query(
        testSetDao: TestSetDao,
        testSetID: Int
    ): QuestionQueried {
        val state = testSetDao.getQuestionStateByIDs(
            testSetID,
            questionID = id
        )
        return QuestionQueried(
            testSetDao,
            base = this,
            state,
            answers = testSetDao.getAnswersByIDs(state.answerIDs)
        )
    }
}

@Entity(tableName = "questionState")
data class QuestionState(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val testSetID: Int,
    val questionID: Int,

    // Will preserve the question's answerIDs in their exact order within this testSet.
    val answerIDs: List<Int>,
) {
    /* STATE */
    var stateSelectedAnswerIndexes: List<Int> = emptyList()
    var stateVideoTimesWatched: Int = 0
    var stateVideoWatched: Boolean = false
}

data class QuestionQueried(
    private val testSetDao: TestSetDao?,
    val base: Question,
    val state: QuestionState,
    var answers: List<Answer>
) {
    private var _updateCount = MutableStateFlow(0)
    val updateCount: StateFlow<Int> = _updateCount

    suspend fun save() {
        ++_updateCount.value

        testSetDao!!.insertQuestionState(state)
    }

    suspend fun setAssessment(assessed: QuestionAssessed) {
        answers.forEach { answer ->
            run answerAssessedForeach@{
                assessed.answers.forEach { answerAssessed ->
                    if (answer.id == answerAssessed.id) {
                        // Mark answer "correct" property, save to DB
                        answer.correct = answerAssessed.correct
                        testSetDao!!.updateAnswerSetCorrect(
                            answer.id,
                            answerAssessed.correct
                        )
                        return@answerAssessedForeach
                    }
                }
            }
        }
    }

    fun isCorrect(): Boolean {
        val correctAnswerIndexes = emptyList<Int>().toMutableList()
        answers.forEachIndexed { answerIndex, answer ->
            if (answer.correct == true) {
                correctAnswerIndexes += answerIndex
            }
        }

        return correctAnswerIndexes == state.stateSelectedAnswerIndexes.sorted()
    }
}