package com.provigz.avtotest.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.provigz.avtotest.db.entity.Answer
import com.provigz.avtotest.db.entity.Property
import com.provigz.avtotest.db.entity.TestSet
import com.provigz.avtotest.db.entity.Question
import com.provigz.avtotest.db.entity.QuestionState

@Dao
interface TestSetDao {
    @Query("SELECT * FROM testSet WHERE id = :id")
    suspend fun getTestSetByID(id: Int): TestSet?
    @Query("SELECT * FROM question WHERE id IN (:questionIDs)")
    suspend fun getQuestionsByIDs(questionIDs: List<Int>): List<Question>
    @Query("SELECT * FROM questionState WHERE testSetID = :testSetID AND questionID = :questionID")
    suspend fun getQuestionStateByIDs(testSetID: Int, questionID: Int): QuestionState
    @Query("SELECT * FROM answer WHERE id IN (:answerIDs)")
    suspend fun getAnswersByIDs(answerIDs: List<Int>): List<Answer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestSet(testSet: TestSet)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: Question)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionState(questionState: QuestionState)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: Answer)

    /* PROPERTIES */
    @Query("SELECT value FROM property WHERE name = :name")
    suspend fun getProperty(name: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setProperty(property: Property)
}