package com.provigz.avtotest.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "property")
data class Property(
    @PrimaryKey val name: String,
    val value: String
)