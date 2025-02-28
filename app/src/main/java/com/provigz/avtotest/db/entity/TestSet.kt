package com.provigz.avtotest.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.provigz.avtotest.db.TestSetDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Entity(tableName = "testSet")
data class TestSet(
    @PrimaryKey val id: Int,
    val categoryID: Int,
    val categoryName: String,
    val durationMinutes: Int,
    var questionIDs: List<Int>,
) {
    /* STATE */
    var stateCurrentQuestionIndex: Int = 0
    var stateDurationSecondsLeft: Int = durationMinutes
    var stateTimeFinished: Int = 0

    constructor(model: com.provigz.avtotest.model.TestSet) : this(
        id = model.id,
        categoryID = model.subCategory.id,
        categoryName = model.subCategory.name,
        durationMinutes = model.subCategory.durationMinutes,
        questionIDs = emptyList()
    )

    suspend fun insertQuestions(
        testSetDao: TestSetDao,
        questions: Array<com.provigz.avtotest.model.Question>
    ) {
        questions.forEach { questionModel ->
            val question = Question(questionModel)
            question.insertAnswers(
                testSetDao,
                answers = questionModel.answers
            )
            question.createState(
                testSetDao,
                testSetID = id
            )

            testSetDao.insertQuestion(question)
            questionIDs += questionModel.id
        }
    }

    suspend fun query(testSetDao: TestSetDao): TestSetQueried {
        val queried = TestSetQueried(
            testSetDao,
            base = this
        )
        val questions = testSetDao.getQuestionsByIDs(questionIDs)
        questions.forEach { question ->
            queried.questions += question.query(
                testSetDao,
                testSetID = id
            )
        }
        return queried
    }
}

data class TestSetQueried(
    private val testSetDao: TestSetDao?,
    val base: TestSet,
    var questions: List<QuestionQueried> = emptyList()
) {
    private var _updateCount = MutableStateFlow(0)
    val updateCount: StateFlow<Int> = _updateCount

    suspend fun save() {
        ++_updateCount.value

        testSetDao!!.insertTestSet(base)
    }
}