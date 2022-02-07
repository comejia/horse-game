package com.comejia.horsegame

import android.graphics.Point
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TableRow
import androidx.core.content.ContextCompat
import com.comejia.horsegame.databinding.ActivityMainBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private var cellSelectedX = 0
    private var cellSelectedY = 0
    private var moves = 8*8
    private var movesRequired = 4
    private var bonus = 0
    private lateinit var board: Array<IntArray>
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initScreenGame()
        resetBoard()
        setFirstPosition()
    }

    fun onCellClicked(v: View) {
        val name = v.tag.toString()
        val x = name.substring(1, 2).toInt()
        val y = name.substring(2, 3).toInt()

        if (!isCellSelected(x, y) && isMoveValid(x, y)) {
            selectCell(x, y)
        }
    }

    private fun isMoveValid(x: Int, y: Int): Boolean {
        val diffX = abs(x - cellSelectedX)
        val diffY = abs(y - cellSelectedY)
        return (diffX == 1 && diffY == 0) || (diffX == 0 && diffY == 1)
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
        cellSelectedX = (0..7).random()
        cellSelectedY = (0..7).random()

        selectCell(cellSelectedX, cellSelectedY)
    }

    private fun selectCell(x: Int, y: Int) {
        moves--
        showMovesAmount(moves)

        if (isBonusCell(x, y)) {
            bonus++
            binding.tvBonusData.text = " + $bonus"
        }

        paintHorseCell(cellSelectedX, cellSelectedY, R.color.previous_cell)
        clearOptions(cellSelectedX, cellSelectedY)
        cellSelectedX = x
        cellSelectedY = y
        paintHorseCell(x, y, R.color.selected_cell)
        paintOptions(x, y)

        setBoardCell(x, y, 1)

        if (moves > 0 && isBonusMove(moves)) {
            addNewBonus()
        }
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