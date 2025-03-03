package com.provigz.avtotest.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.provigz.avtotest.db.entity.Answer
import com.provigz.avtotest.db.entity.Property
import com.provigz.avtotest.db.entity.Question
import com.provigz.avtotest.db.entity.QuestionState
import com.provigz.avtotest.db.entity.TestSet

suspend fun <T> getOrderedObjectsByIDs(
    tableName: String,
    ids: List<Int>,
    queryFun: suspend (SupportSQLiteQuery) -> List<T>
): List<T> {
    if (ids.isEmpty()) {
        return emptyList()
    }

    val caseStatement = ids.mapIndexed { index, id ->
        "WHEN id = $id THEN $index"
    }.joinToString(separator = " ")

    return queryFun(
        SimpleSQLiteQuery(
            query =
            """
                SELECT * FROM $tableName 
                WHERE id IN (${ids.joinToString()}) 
                ORDER BY CASE $caseStatement ELSE 999 END
            """
        )
    )
}

@Dao
interface TestSetDao {
    @Query("SELECT * FROM testSet WHERE id = :id")
    suspend fun getTestSetByID(id: Int): TestSet?
    @Query("SELECT * FROM questionState WHERE testSetID = :testSetID AND questionID = :questionID")
    suspend fun getQuestionStateByIDs(testSetID: Int, questionID: Int): QuestionState

    @RawQuery
    suspend fun getQuestionsByRawQuery(query: SupportSQLiteQuery): List<Question>
    suspend fun getQuestionsByIDs(questionIDs: List<Int>): List<Question> {
        return getOrderedObjectsByIDs(
            tableName = "question",
            ids = questionIDs
        ) { query ->
            getQuestionsByRawQuery(query)
        }
    }
    @RawQuery
    suspend fun getAnswersByRawQuery(query: SupportSQLiteQuery): List<Answer>
    suspend fun getAnswersByIDs(answerIDs: List<Int>): List<Answer> {
        return getOrderedObjectsByIDs(
            tableName = "answer",
            ids = answerIDs
        ) { query ->
            getAnswersByRawQuery(query)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestSet(testSet: TestSet)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestion(question: Question)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionState(questionState: QuestionState)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnswer(answer: Answer)

    @Query("UPDATE testSet SET stateSecondsPassed = :secondsPassed WHERE id = :testSetID")
    suspend fun updateTestSetSecondsPassed(testSetID: Int, secondsPassed: Int)

    @Query("UPDATE answer SET correct = :correct WHERE id = :answerID")
    suspend fun updateAnswerSetCorrect(answerID: Int, correct: Boolean)

    /* PROPERTIES */
    @Query("SELECT value FROM property WHERE name = :name")
    suspend fun getProperty(name: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setProperty(property: Property)

    @Query("DELETE FROM property WHERE name = :name")
    suspend fun deleteProperty(name: String)
}