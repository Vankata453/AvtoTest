package com.provigz.avtotest.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.provigz.avtotest.db.entity.Answer
import com.provigz.avtotest.db.entity.TestSet
import com.provigz.avtotest.db.entity.Question
import com.provigz.avtotest.db.entity.QuestionState
import com.provigz.avtotest.db.entity.Property

class RoomConverters {
    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return value?.joinToString(separator = ",") ?: ""
    }
    @TypeConverter
    fun toIntList(value: String): List<Int> {
        return if (value.isEmpty()) emptyList() else value.split(",").map { it.toInt() }
    }
}

@Database(
    entities = [
        Answer::class,
        TestSet::class,
        Question::class,
        QuestionState::class,
        Property::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class TestSetDatabase : RoomDatabase() {
    abstract fun testSetDao(): TestSetDao

    companion object {
        @Volatile
        private var INSTANCE: TestSetDatabase? = null

        fun getDatabase(context: Context): TestSetDatabase {
            return INSTANCE ?: synchronized(lock = this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TestSetDatabase::class.java,
                    name = "test_set_database"
                ).fallbackToDestructiveMigration().build()

                INSTANCE = instance
                instance
            }
        }
    }
}