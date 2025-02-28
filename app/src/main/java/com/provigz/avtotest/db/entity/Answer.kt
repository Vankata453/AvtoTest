package com.provigz.avtotest.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "answer")
data class Answer(
    @PrimaryKey val id: Int,
    val text: String?,
    val pictureID: String?
) {
    var correct: Boolean? = null

    constructor(model: com.provigz.avtotest.model.Answer) : this(
        id = model.id,
        text = model.text,
        pictureID = model.pictureID
    )
}