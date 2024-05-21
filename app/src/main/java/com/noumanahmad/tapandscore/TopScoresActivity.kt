package com.noumanahmad.tapandscore


import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TopScoresActivity : AppCompatActivity() {

    private lateinit var dbHelper: ScoreDbHelper
    private lateinit var db: SQLiteDatabase
    private lateinit var scoresTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a LinearLayout as the root view
        val rootLayout = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
        }
        setContentView(rootLayout)

        // Initialize the database helper and database
        dbHelper = ScoreDbHelper(this)
        db = dbHelper.readableDatabase


        scoresTextView = TextView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(scoresTextView)

        // Display the top scores
        displayTopScores()
    }

    private fun displayTopScores() {
        val cursor = db.query(
            ScoreContract.ScoreEntry.TABLE_NAME,
            arrayOf(ScoreContract.ScoreEntry.COLUMN_NAME_PLAYER, ScoreContract.ScoreEntry.COLUMN_NAME_SCORE),
            null, null, null, null,
            "${ScoreContract.ScoreEntry.COLUMN_NAME_SCORE} DESC"
        )

        val scores = StringBuilder()
        with(cursor) {
            while (moveToNext()) {
                val player = getString(getColumnIndexOrThrow(ScoreContract.ScoreEntry.COLUMN_NAME_PLAYER))
                val score = getInt(getColumnIndexOrThrow(ScoreContract.ScoreEntry.COLUMN_NAME_SCORE))
                scores.append("$player: $score\n")
            }
        }
        cursor.close()

        scoresTextView.text = scores.toString()
    }



}


