package com.provigz.avtotest.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.provigz.avtotest.db.TestSetDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Entity(tableName = "question")
data class Question(
    @PrimaryKey val id: Int,
    val text: String,
    val thumbnailID: String,
    val pictureID: String,
    val videoID: String,
    val points: Int,
    val correctAnswers: Int,
    var answerIDs: List<Int>,

    /* STATE */
    var stateSelectedAnswerIndexes: List<Int> = emptyList(),
    var stateVideoTimesWatched: Int = 0,
    var stateVideoWatched: Boolean = false
) {
    constructor(model: com.provigz.avtotest.model.Question) : this(
        id = model.id,
        text = model.text,
        thumbnailID = if (model.thumbnailID.isNullOrBlank()) "" else model.thumbnailID,
        pictureID = if (model.pictureID.isNullOrBlank()) "" else model.pictureID,
        videoID = if (model.videoID.isNullOrBlank()) "" else model.videoID,
        points = model.points,
        correctAnswers = model.correctAnswers,
        answerIDs = emptyList()
    )

    suspend fun insertAnswers(
        testSetDao: TestSetDao,
        answers: Array<com.provigz.avtotest.model.Answer>
    ) {
        answers.forEach { answerModel ->
            testSetDao.insertAnswer(Answer(answerModel))
            answerIDs += answerModel.id
        }
    }

    suspend fun createState(
        testSetDao: TestSetDao,
        testSetID: Int
    ) {
        testSetDao.insertQuestionState(
            QuestionState(
                testSetID = testSetID,
                questionID = id,
                answerIDs = answerIDs
            )
        )
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

    /* STATE */
    var stateSelectedAnswerIndexes: List<Int> = emptyList(),
    var stateVideoTimesWatched: Int = 0,
    var stateVideoWatched: Boolean = false
)

data class QuestionQueried(
    private val testSetDao: TestSetDao,
    val base: Question,
    val state: QuestionState,
    var answers: List<Answer>
) {
    private var _updateCount = MutableStateFlow(0)
    val updateCount: StateFlow<Int> = _updateCount

    suspend fun save() {
        ++_updateCount.value

        testSetDao.insertQuestionState(state)
    }
}