package com.noumanahmad.tapandscore


import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.provider.BaseColumns
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.EditText
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random
import android.util.Log

class GameSurfaceView(context: Context, attrs: AttributeSet?) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val thread: GameThread
    private val rectangles: CopyOnWriteArrayList<Rectangle> = CopyOnWriteArrayList()
    private val rectangleSpeed: Int = 8
    private val maxRectangles: Int = 56
    private val maxRetries: Int = 50
    private val minGapBetweenRectangles: Int = 50
    private val paint: Paint = Paint()
    private val gameTimer: GameTimer = GameTimer(60000)
    private var score: Int = 0
    private val scorePaint: Paint = Paint()
    private var gameEnded: Boolean = false
    private val dbHelper: ScoreDbHelper = ScoreDbHelper(context)



    init {
        holder.addCallback(this)
        thread = GameThread(holder, this)
        paint.color = Color.BLACK
        scorePaint.color = Color.WHITE
        scorePaint.textSize = 50f
        scorePaint.textAlign = Paint.Align.LEFT
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread.setRunning(true)
        thread.start()
        generateRectangles()
        gameTimer.start()  // Start the timer
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        thread.setRunning(false)
        while (retry) {
            try {
                thread.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    fun update() {
        if (!gameEnded) {
            for (rectangle in rectangles) {
                rectangle.y -= rectangleSpeed
                if (rectangle.y < -rectangle.height) {
                    resetRectangle(rectangle)
                }
            }
            gameTimer.update()  // Update the timer

            if (gameTimer.isTimeUp()) {
                endGame()
            }
        }
    }
    private fun saveScore() {
        val db = dbHelper.writableDatabase

        // Check if the score is high enough to be saved
        val cursor = db.query(
            ScoreContract.ScoreEntry.TABLE_NAME,
            arrayOf(BaseColumns._ID, ScoreContract.ScoreEntry.COLUMN_NAME_PLAYER, ScoreContract.ScoreEntry.COLUMN_NAME_SCORE),
            null, null, null, null,
            "${ScoreContract.ScoreEntry.COLUMN_NAME_SCORE} DESC"
        )

        if (cursor.count < 5 || (cursor.moveToLast() && score > cursor.getInt(cursor.getColumnIndexOrThrow(ScoreContract.ScoreEntry.COLUMN_NAME_SCORE)))) {
            cursor.close()
            // Show the dialog only if the score is in the top five
            showSaveScoreDialog()
        } else {
            cursor.close()
        }
    }

    private fun showSaveScoreDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("New High Score!")

        val input = EditText(context)
        input.hint = "Enter your name"
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val playerName = input.text.toString()
            if (playerName.isNotBlank()) {
                saveScoreToDatabase(playerName)
                // Start TopScoresActivity after saving the score
               // val intent = Intent(context, TopScoresActivity::class.java)
                //Log.d("topscore","Score show to database")
                //context.startActivity(intent)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        Log.d("entername","Score saved to database")
        builder.show()
    }


    private fun saveScoreToDatabase(playerName: String) {
        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(ScoreContract.ScoreEntry.COLUMN_NAME_PLAYER, playerName)
            put(ScoreContract.ScoreEntry.COLUMN_NAME_SCORE, score)
        }

        db.insert(ScoreContract.ScoreEntry.TABLE_NAME, null, values)
             Log.d("savescoretodb","Score saved to database")
        // Delete the lowest scores if there are more than 5 entries
        db.execSQL(
            "DELETE FROM ${ScoreContract.ScoreEntry.TABLE_NAME} WHERE ${BaseColumns._ID} NOT IN (SELECT ${BaseColumns._ID} FROM ${ScoreContract.ScoreEntry.TABLE_NAME} ORDER BY ${ScoreContract.ScoreEntry.COLUMN_NAME_SCORE} DESC LIMIT 5)"
        )
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(Color.BLACK)
        for (rectangle in rectangles) {
            paint.color = rectangle.color
            canvas.drawRect(rectangle.x.toFloat(), rectangle.y.toFloat(), (rectangle.x + rectangle.width).toFloat(), (rectangle.y + rectangle.height).toFloat(), paint)
        }
        gameTimer.draw(canvas, width.toFloat() - 20, 50f)  // Draw the timer at the top right
        canvas.drawText("Score: $score", 20f, 50f, scorePaint)  // Draw the score at the top left
        if (gameEnded) {
            paint.textSize = 100f
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.RED
            canvas.drawText("Game Over", (width / 2).toFloat(), (height / 2).toFloat(), paint)
        }
    }

    private fun generateRectangles() {
        postDelayed(object : Runnable {
            override fun run() {
                if (rectangles.size < maxRectangles) {
                    val newRectangle = createNonCollidingRectangleWithGap()
                    if (newRectangle != null) {
                        rectangles.add(newRectangle)
                    }
                }
                if (!gameEnded) {
                    postDelayed(this, Random.nextLong(1000, 3000))
                }
            }
        }, Random.nextLong(1000, 3000))
    }

    private fun createNonCollidingRectangleWithGap(): Rectangle? {
        var newRectangle: Rectangle
        var retries = 0
        do {
            newRectangle = createRectangleWithGap()
            retries++
        } while (isColliding(newRectangle) && retries < maxRetries)
        return if (retries >= maxRetries) null else newRectangle
    }

    private fun createRectangleWithGap(): Rectangle {
        val width = 100
        val height = 150
        val x = Random.nextInt(0, this.width - width)
        // Ensure the rectangle starts below the screen
        val y = this.height + Random.nextInt(minGapBetweenRectangles * (rectangles.size + 1), height + this.height)
        val color = getRandomColor()
        return Rectangle(x, y, width, height, color)
    }

    private fun resetRectangle(rectangle: Rectangle) {
        var retries = 0
        do {
            // Ensure the rectangle starts below the screen
            rectangle.y = this.height + Random.nextInt(300, 400)
            rectangle.x = Random.nextInt(0, this.width - rectangle.width)
            rectangle.color = getRandomColor()
            retries++
        } while (isColliding(rectangle) && retries < maxRetries)
    }

    private fun isColliding(newRectangle: Rectangle): Boolean {
        for (rectangle in rectangles) {
            if (newRectangle.isCollidingWith(rectangle)) {
                return true
            }
        }
        return false
    }

    private fun getRandomColor(): Int {
        return when (Random.nextInt(4)) {
            0 -> Color.RED
            1 -> Color.GREEN
            2 -> Color.BLUE
            else -> Color.YELLOW
        }
    }

    private fun endGame() {
        gameEnded = true
        thread.setRunning(false)

        // Ensure showSaveScoreDialog is called on the UI thread
        Handler(Looper.getMainLooper()).post {
            saveScore()
            showSaveScoreDialog()
        }
    }








    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && !gameEnded) {
            val touchX = event.x.toInt()
            val touchY = event.y.toInt()
            var touchedRectangle = false
            for (rectangle in rectangles) {
                if (touchX >= rectangle.x && touchX <= rectangle.x + rectangle.width && touchY >= rectangle.y && touchY <= rectangle.y + rectangle.height) {
                    score += 2  // Increase score by 2 for each tap on a rectangle
                    rectangles.remove(rectangle)
                    touchedRectangle = true
                    break
                }
            }
            if (!touchedRectangle) {
                score -= 1  // Decrease score by 1 if tapped outside rectangles
            }
            if (score < 0) {
                score = 0  // Ensure score does not go below zero
            }
        }
        return true
    }

    data class Rectangle(var x: Int, var y: Int, val width: Int, val height: Int, var color: Int) {
        fun isCollidingWith(other: Rectangle): Boolean {
            return !(x + width < other.x || x > other.x + other.width || y + height < other.y || y > other.y + other.height)
        }
    }
}
