package com.comejia.horsegame

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TableRow
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.test.runner.screenshot.ScreenCapture
import androidx.test.runner.screenshot.Screenshot.capture
import com.comejia.horsegame.databinding.ActivityMainBinding
import java.sql.Timestamp
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private var cellSelectedX = 0
    private var cellSelectedY = 0

    private var moves = 8*8
    private var movesRequired = 4
    private var bonus = 0
    private var gameLevel: Int = 2
    private var lives = 1

    private var allMovementAvailable = false
    private lateinit var board: Array<IntArray>
    private lateinit var binding: ActivityMainBinding

    private var timeHandler: Handler? = null
    private var timeInSeconds = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initScreenGame()
        startGame(gameLevel)

        binding.ibShare.setOnClickListener {
            shareGame()
        }

        binding.tvAction.setOnClickListener {
            hideMessage()
            startGame(gameLevel)
        }
    }

    private fun shareGame() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)

        val screen: ScreenCapture = capture(this)
        val timestamp = Timestamp(System.currentTimeMillis())

        val bitmapPath = saveImage(screen.bitmap, "${timestamp}.jpg") ?: return

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_STREAM, Uri.parse(bitmapPath))
            putExtra(Intent.EXTRA_TEXT, "Te desafio a ganarme!")
            type = "image/png"
        }

        val finalShareIntent = Intent.createChooser(shareIntent, "Select the app you want to share the game to")
        finalShareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        this.startActivity(finalShareIntent)
    }

    private fun saveImage(bitmap: Bitmap?, filename: String): String? {
        if (bitmap == null) return null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Screenshots")
            }
            val uri = this.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            return uri?.let {
                this.contentResolver.openOutputStream(uri).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, output)
                    output?.flush()
                    output?.close()

                    MediaScannerConnection.scanFile(this, arrayOf(uri.toString()), null, null)
                }
                it.toString()
            }
        }
        return null
    }

    private fun startGame(level: Int) {
        resetBoard()
        clearBoard()
        setLevelParameters(level)
        setFirstPosition()
        resetTime()
        startTime()
    }

    private fun setLevelParameters(level: Int) {
        when (level) {
            1 -> {
                val column = (0..7).random()
                moves = 64
                movesRequired = 4
                for (i in 0..7) {
                    paintHorseCell(i, column, R.color.previous_cell)
                    setBoardCell(i, column, 1)
                }
            }
            2 -> {
                val row = (0..7).random()
                moves = 50
                movesRequired = 6
                for (i in 0..7) {
                    paintHorseCell(row, i, R.color.previous_cell)
                    setBoardCell(row, i, 1)
                }
            }
            3 -> {
                moves = 40
                movesRequired = 10
                for (i in 0..7) {
                    val column = (0..7).random()
                    val row = (0..7).random()
                    paintHorseCell(row, column, R.color.previous_cell)
                    setBoardCell(row, column, 1)
                }
            }
        }
        bonus = 0
        binding.tvLevelNumber.text = level.toString()
        binding.tvLiveData.text = lives.toString()
    }

    private fun startTime() {
        timeHandler = Handler(Looper.getMainLooper())
        chronometer.run()
    }

    private fun resetTime() {
        timeHandler?.removeCallbacks(chronometer)
        timeInSeconds = 0
        binding.tvTimeData.text = getFormattedTime(timeInSeconds)
    }

    private fun stopTime() {
        timeHandler?.removeCallbacks(chronometer)
    }

    private var chronometer: Runnable = object: Runnable {
        override fun run() {
            try {
                timeInSeconds++
                updateTimeView(timeInSeconds)
            } finally {
                timeHandler?.postDelayed(this, 1000L)
            }
        }
    }

    private fun updateTimeView(seconds: Long) {
        binding.tvTimeData.text = getFormattedTime(seconds)
    }

    private fun getFormattedTime(seconds: Long): String {
        val min = TimeUnit.SECONDS.toMinutes(seconds)
        val sec = seconds - TimeUnit.MINUTES.toSeconds(min)
        return String.format("%02d:%02d", min, sec)
    }

    private fun clearBoard() {
        for (i in board.indices) {
            for (j in board.indices) {
                val image: ImageView = findViewById(resources.getIdentifier("c$i$j", "id", packageName))
                image.apply {
                    setImageResource(0)
                    setBackgroundColor(ContextCompat.getColor(this@MainActivity, getColorCell(i, j)))
                }
            }
        }
    }

    fun onCellClicked(v: View) {
        val name = v.tag.toString()
        val x = name.substring(1, 2).toInt()
        val y = name.substring(2, 3).toInt()

        if (!isCellSelected(x, y) && isMovementValid(x, y)) {
            allMovementAvailable = false
            selectCell(x, y)
        }
    }

    private fun isMovementValid(x: Int, y: Int): Boolean {
        val diffX = abs(x - cellSelectedX)
        val diffY = abs(y - cellSelectedY)
        return (diffX == 1 && diffY == 0) || (diffX == 0 && diffY == 1 || allMovementAvailable)
    }

    private fun isCellSelected(x: Int, y: Int): Boolean = board[x][y] == 1

    private fun paintOptions(x: Int, y: Int) {
        val options = listValidOptions(x, y)
        options.forEach { option -> paintOption(option[0], option[1]) }

        showOptionsAmount(options.size)
    }

    private fun showOptionsAmount(amount: Int) {
        binding.tvOptionsData.text = amount.toString()
    }

    private fun showMovesAmount(amount: Int) {
        binding.tvMovesData.text = amount.toString()
    }

    private fun listValidOptions(x: Int, y: Int): List<IntArray> = arrayOf(
            intArrayOf(x, y+1),
            intArrayOf(x, y-1),
            intArrayOf(x+1, y),
            intArrayOf(x-1, y)
        ).filter { option -> isOptionValid(option[0], option[1]) && !isCellSelected(option[0], option[1]) }

    private fun paintOption(x: Int, y: Int) {
        val image: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        image.setBackgroundResource(if (getColorCell(x, y) == R.color.black_cell) R.drawable.option_black else R.drawable.option_white)
    }

    private fun isOptionValid(x: Int, y: Int): Boolean = x in 0..7 && y in 0..7

    private fun getColorCell(x: Int, y: Int): Int {
        val blackColumnX = arrayOf(0, 2, 4, 6)
        val blackColumnY = arrayOf(1, 3, 5, 7)
        if (blackColumnX.contains(x) && blackColumnX.contains(y)
            || blackColumnY.contains(x) && blackColumnY.contains(y))
                return R.color.black_cell

        return R.color.white_cell
    }

    private fun resetBoard() {
        board = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        )
    }

    private fun setFirstPosition() {
        do {
            cellSelectedX = (0..7).random()
            cellSelectedY = (0..7).random()
        } while (!isCellEmpty(cellSelectedX, cellSelectedY))

        selectCell(cellSelectedX, cellSelectedY)
    }

    private fun selectCell(x: Int, y: Int) {
        moves--
        showMovesAmount(moves)

        if (isBonusCell(x, y)) {
            bonus++
            binding.tvBonusData.text = " + $bonus"
        }
        clearAllOptions()

        paintHorseCell(cellSelectedX, cellSelectedY, R.color.previous_cell)
        clearOptions(cellSelectedX, cellSelectedY)
        cellSelectedX = x
        cellSelectedY = y
        paintHorseCell(x, y, R.color.selected_cell)
        paintOptions(x, y)

        setBoardCell(x, y, 1)

        if (hasMoreMoves()) {
            if (isBoardComplete()) {
                showMessage("You Win!!", "Next Level", false)
                stopTime()
                gameLevel++
            } else {
                checkGameOver(x, y)
                if (isBonusMove(moves)) {
                    addNewBonus()
                }
            }
        } else {
            showMessage("You Win!!", "Next Level", false)
            stopTime()
            gameLevel++
        }

    }

    private fun hasMoreMoves(): Boolean = moves > 0

    private fun checkGameOver(x: Int, y: Int) {
        if (listValidOptions(x, y).isEmpty()) {
            /*if (isBoardComplete()) {
                showMessage("You Win!!", "Next Level", false)
                stopTime()
                gameLevel++
            }*/
            if (bonus == 0) {
                showMessage("Game Over", "Start Again", true)
                stopTime()
                gameLevel = 1
            } else {
                allMovementAvailable = true
                bonus--
                binding.tvBonusData.text = if (bonus > 0) " + $bonus" else ""
                paintAllOptions()
            }
        }
    }

    private fun isBoardComplete(): Boolean = board.all { array -> array.all { cell -> cell == 1 } }

    private fun paintAllOptions() {
        board.forEachIndexed { index, array ->
            array.indices.forEach { j ->
                if (array[j] != 1) {
                    paintOption(index, j)
                }
            }
        }
    }

    private fun clearAllOptions() {
        board.forEachIndexed { index, array ->
            array.indices.forEach { j ->
                if (array[j] != 1) {
                    clearOption(index, j)
                }
            }
        }
    }

    private fun showMessage(title: String, action: String, isGameOver: Boolean) {
        binding.lyMessage.visibility = View.VISIBLE
        binding.tvTitleMessage.text = title

        val score: String = if (isGameOver) "Score: ${64 - moves}/64" else binding.tvTimeData.text.toString()
        binding.tvScoreMessage.text = score

        binding.tvAction.text = action
    }

    private fun isBonusCell(x: Int, y: Int): Boolean = board[x][y] == 2

    private fun addNewBonus() {
        var bonusCellX: Int
        var bonusCellY: Int
        do {
            bonusCellX = (0..7).random()
            bonusCellY = (0..7).random()
        } while (!isCellEmpty(bonusCellX, bonusCellY))

        setBoardCell(bonusCellX, bonusCellY, 2)
        paintBonusCell(bonusCellX, bonusCellY)
    }

    private fun paintBonusCell(x: Int, y: Int) {
        val image: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        image.setImageResource(R.drawable.icon_bonus)
    }

    private fun setBoardCell(x: Int, y: Int, value: Int) {
        board[x][y] = value
    }

    private fun isCellEmpty(x: Int, y: Int): Boolean = board[x][y] == 0

    private fun isBonusMove(move: Int): Boolean = move % movesRequired == 0

    private fun clearOptions(x: Int, y: Int) {
        val options = listValidOptions(x, y)
        options.forEach { option -> clearOption(option[0], option[1]) }
    }

    private fun clearOption(x: Int, y: Int) {
        val image: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        image.setBackgroundColor(ContextCompat.getColor(this@MainActivity, getColorCell(x, y)))
    }

    private fun paintHorseCell(x: Int, y: Int, color: Int) {
        val image: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName))
        image.apply {
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, color))
            setImageResource(R.drawable.icon_horse)
        }
    }

    private fun initScreenGame() {
        setSizeBoard()
        hideMessage()
    }

    private fun hideMessage() {
        binding.lyMessage.visibility = View.INVISIBLE
    }

    private fun setSizeBoard() {
        var image: ImageView

        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val widthDp = size.x / resources.displayMetrics.density

        val lateralMarginDp = 0
        val widthCell = (widthDp - lateralMarginDp) / 8
        val heightCell = widthCell


        for(i in 0..7) {
            for (j in 0..7) {
                image = findViewById(resources.getIdentifier("c$i$j", "id", packageName))
                val height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightCell, resources.displayMetrics).toInt()
                val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthCell, resources.displayMetrics).toInt()

                image.layoutParams = TableRow.LayoutParams(width, height)
            }
        }
    }
}