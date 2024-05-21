package com.noumanahmad.tapandscore

import android.provider.BaseColumns

object ScoreContract {
    object ScoreEntry : BaseColumns
    {
        const val TABLE_NAME = "scores"
        const val COLUMN_NAME_PLAYER = "player"
        const val COLUMN_NAME_SCORE = "score"
    }
}