package com.provigz.avtotest.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.provigz.avtotest.db.TestSetDao
import com.provigz.avtotest.model.TestSetAssessment
import com.provigz.avtotest.model.TestSetAssessmentResult
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
    var stateCurrentQuestionIndex: Int = 0 // Negative indicates results page
    var stateSecondsPassed: Int = 0

    /* STATE FINISHED */
    var stateTimedOut: Boolean = false // Whether the testSet was forcefully submitted because no remaining time was left
    var stateTimeFinished: Long? = null // null indicates this testSet hasn't yet been submitted
    var stateAssessed: Boolean = false
    var stateReceivedPoints: Int = 0
    var stateTotalPoints: Int = 0
    var stateCorrectQuestionsCount: Int = 0
    var stateIncorrectQuestionsCount: Int = 0
    var statePassed: Boolean = false

    constructor(model: com.provigz.avtotest.model.TestSet) : this(
        id = model.id,
        categoryID = model.subCategory.id.toInt(),
        categoryName = model.subCategory.name,
        durationMinutes = model.subCategory.durationMinutes,
        questionIDs = emptyList()
    )

    fun setAssessmentResult(assessmentResult: TestSetAssessmentResult) {
        stateAssessed = true
        stateReceivedPoints = assessmentResult.receivedPoints
        stateTotalPoints = assessmentResult.totalPoints
        stateCorrectQuestionsCount = assessmentResult.correctQuestionsCount
        stateIncorrectQuestionsCount = assessmentResult.incorrectQuestionsCount
        statePassed = assessmentResult.passed
    }

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
    /* TRACKED STATES */
    private var _updateCount = MutableStateFlow(0)
    val updateCount: StateFlow<Int> = _updateCount

    private var _secondsPassed = MutableStateFlow(base.stateSecondsPassed)
    val secondsPassed: StateFlow<Int> = _secondsPassed

    suspend fun save() {
        ++_updateCount.value

        testSetDao!!.insertTestSet(base)
    }

    suspend fun requestSubmit(timeout: Boolean = false) {
        base.stateTimedOut = timeout
        base.stateTimeFinished = System.currentTimeMillis()
        save()
    }
    suspend fun retractSubmit() {
        base.stateTimedOut = false
        base.stateTimeFinished = null
        save()
    }

    suspend fun setAssessment(assessment: TestSetAssessment) {
        base.setAssessmentResult(assessment.result)
        questions.forEach { question ->
            run questionAssessedForeach@{
                assessment.testSet.questions.forEach { questionAssessed ->
                    if (question.base.id == questionAssessed.id) {
                        question.setAssessment(questionAssessed)
                        return@questionAssessedForeach
                    }
                }
            }
        }
        base.stateCurrentQuestionIndex = -1 // Direct user to results page
        save()
    }

    suspend fun incrementSecondsPassed() {
        _secondsPassed.value = ++base.stateSecondsPassed
        testSetDao!!.updateTestSetSecondsPassed(
            testSetID = base.id,
            secondsPassed = base.stateSecondsPassed
        )
    }
}