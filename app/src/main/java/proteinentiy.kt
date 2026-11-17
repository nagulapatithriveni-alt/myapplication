package com.example.proteinexplorer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proteins")
data class ProteinEntity(

    @PrimaryKey
    val accession: String,

    val name: String,

    val organism: String,

    val sequence: String
)