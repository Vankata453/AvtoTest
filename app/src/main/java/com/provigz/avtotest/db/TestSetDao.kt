package com.provigz.avtotest.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.provigz.avtotest.db.entity.Answer
import com.provigz.avtotest.db.entity.Property
import com.provigz.avtotest.db.entity.Question
import com.provigz.avtotest.db.entity.QuestionState
import com.provigz.avtotest.db.entity.QuestionWithOccurrenceCount
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
    @Query("SELECT * FROM question WHERE id = :id")
    suspend fun getQuestionByID(id: Int): Question?
    @Query("SELECT * FROM questionState WHERE testSetID = :testSetID AND questionID = :questionID")
    suspend fun getQuestionStateByIDs(testSetID: Int, questionID: Int): QuestionState

    @RawQuery
    suspend fun getTestSetsByRawQuery(query: SupportSQLiteQuery): List<TestSet>
    suspend fun getTestSetsSortFilter(limit: Int, offset: Int, sortQuery: String, filterQuery: String): List<TestSet> {
        val orderByQueryStr = if (sortQuery.isNotBlank()) "ORDER BY $sortQuery" else ""
        val whereQueryStr = if (filterQuery.isNotBlank()) "WHERE $filterQuery" else ""
        val query = SimpleSQLiteQuery(
            query = "SELECT * FROM testSet $whereQueryStr $orderByQueryStr LIMIT $limit OFFSET $offset"
        )
        return getTestSetsByRawQuery(query)
    }
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
    suspend fun getQuestionsAndOccurenceCountByRawQuery(query: SupportSQLiteQuery): List<QuestionWithOccurrenceCount>
    suspend fun getQuestionsAndOccurenceCountSortFilter(limit: Int, offset: Int, sortQuery: String, filterQuery: String): List<QuestionWithOccurrenceCount> {
        val orderByQueryStr = if (sortQuery.isNotBlank()) "ORDER BY $sortQuery" else ""
        val whereQueryStr = if (filterQuery.isNotBlank()) "WHERE $filterQuery" else ""
        val query = SimpleSQLiteQuery(
            query = """
                SELECT q.*, COUNT(s.questionId) AS occurrenceCount
                FROM question q
                LEFT JOIN questionState s ON q.id = s.questionId
                $whereQueryStr
                GROUP BY q.id
                $orderByQueryStr
                LIMIT $limit OFFSET $offset
            """
        )
        return getQuestionsAndOccurenceCountByRawQuery(query)
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

    @Query("SELECT COUNT(*) FROM testSet WHERE voucherCode = :voucherCode")
    suspend fun countTestSetsByVoucherCode(voucherCode: String): Int
    @Query("SELECT COUNT(*) FROM questionState WHERE questionID = :questionID")
    suspend fun countQuestionOccurrencesByID(questionID: Int): Int

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
    @Query("UPDATE question SET favorite = :favorite WHERE id = :questionID")
    suspend fun updateQuestionFavorite(questionID: Int, favorite: Boolean)
    @Query("UPDATE answer SET correct = :correct WHERE id = :answerID")
    suspend fun updateAnswerSetCorrect(answerID: Int, correct: Boolean)

    @Query("DELETE FROM testSet WHERE id = :testSetID")
    suspend fun deleteTestSetEntryByID(testSetID: Int)
    @Query("DELETE FROM questionState WHERE testSetID = :testSetID")
    suspend fun deleteQuestionStatesByTestSetID(testSetID: Int)
    @Transaction
    suspend fun deleteTestSetByID(testSetID: Int) {
        deleteQuestionStatesByTestSetID(testSetID)
        deleteTestSetEntryByID(testSetID)
        if (getProperty(name = "startedTestSet") == testSetID.toString()) {
            deleteProperty(name = "startedTestSet")
        }

        deleteUndeterminedQuestions()
    }

    @Query("DELETE FROM question WHERE id IN (SELECT questionID FROM answer WHERE correct IS NULL)")
    suspend fun deleteUndeterminedQuestionEntries()
    @Query("DELETE FROM answer WHERE correct IS NULL")
    suspend fun deleteUndeterminedAnswers()
    @Transaction
    suspend fun deleteUndeterminedQuestions() {
        deleteUndeterminedQuestionEntries()
        deleteUndeterminedAnswers()
    }

    /* PROPERTIES */
    @Query("SELECT value FROM property WHERE name = :name")
    suspend fun getProperty(name: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setProperty(property: Property)

    @Query("DELETE FROM property WHERE name = :name")
    suspend fun deleteProperty(name: String)
}