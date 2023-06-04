package com.example.tetrisgame

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.gridlayout.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import androidx.core.view.children
import kotlinx.coroutines.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    //private lateinit var gridLayout: GridLayout
    private lateinit var formedWord: TextView
    private lateinit var okButton: Button
    private lateinit var clearButton: Button
    private var points = 0
    private lateinit var pointsTextView: TextView

    private lateinit var gridLayouts: List<GridLayout>
    private val selectedLetters = mutableListOf<TextView>()
    private var isGameOver = false
    private val englishWords = mutableSetOf<String>()

    private val consoRed = Color.rgb(232, 104, 80)
    private val vowelGreen = Color.rgb(88, 112, 88)

    private val columns = Array(8) { mutableListOf<TextView>() }

    private fun dpToPx(dp: Float, context: Context): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridLayouts = listOf(
            findViewById(R.id.grid_layout1),
            findViewById(R.id.grid_layout2),
            findViewById(R.id.grid_layout3),
            findViewById(R.id.grid_layout4),
            findViewById(R.id.grid_layout5),
            findViewById(R.id.grid_layout6),
            findViewById(R.id.grid_layout7),
            findViewById(R.id.grid_layout8)
        )
        formedWord = findViewById(R.id.formed_word)
        okButton = findViewById(R.id.ok_button)
        clearButton = findViewById(R.id.clear_button)
        pointsTextView = findViewById(R.id.pointsTextView)

        clearButton.setOnClickListener {
            clearSelectedLetters()
        }

        // loadWords
        loadEnglishWords()



        // start timer
        startFallingLetters()

        // setupOK
        okButton.setOnClickListener {
            validateAndClearWord()
        }

        for (gridLayout in gridLayouts) {
            initGameBoard(gridLayout)
        }
    }



    private val letterClickListener = View.OnClickListener {
        val textView = it as TextView
        if (!selectedLetters.contains(textView)) {
            selectedLetters.add(textView)
            formedWord.text = selectedLetters.joinToString("") { it.text.toString() }
        }
    }

    private fun getLetterPoints(letter: Char): Int {
        return when (letter.toUpperCase()) {
            'A' -> 1
            'B' -> 3
            'C' -> 4
            'Ç' -> 4
            'D' -> 3
            'E' -> 1
            'F' -> 7
            'G' -> 5
            'Ğ' -> 8
            'H' -> 5
            'I' -> 2
            'İ' -> 1
            'J' -> 10
            'K' -> 1
            'L' -> 1
            'M' -> 2
            'N' -> 1
            'O' -> 2
            'Ö' -> 7
            'P' -> 5
            'R' -> 1
            'S' -> 2
            'Ş' -> 4
            'T' -> 1
            'U' -> 2
            'Ü' -> 3
            'V' -> 7
            'Y' -> 3
            'Z' -> 4
            else -> 0
        }
    }

    private fun isVowel(letter: Char): Boolean {
        val turkishVowels = "aeıioöuü"
        return letter.toLowerCase() in turkishVowels
    }


    private fun loadEnglishWords() {
        //val reader = BufferedReader(InputStreamReader(assets.open("words_alpha.txt"))) //ingilizce
        val reader = BufferedReader(InputStreamReader(assets.open("words_turkish.txt")))
        reader.useLines { lines ->
            lines.forEach { englishWords.add(it) }
        }
    }

    private fun clearSelectedLetters() {
        selectedLetters.clear()
        formedWord.text = ""
    }

    private fun initGameBoard(gridLayout: GridLayout) {
        // bos bir gameboard ac
        for (i in 0 until 10) {
            val emptyCell = createEmptyCell()
            gridLayout.addView(emptyCell)
        }
        // 3 row doldur
        for (i in 7 until 10) {
            val letterView = createLetterView()
            gridLayout.removeViewAt(i)
            gridLayout.addView(letterView, i)
        }
    }

    private fun startFallingLetters() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (isGameOver) {
                    handler.removeCallbacks(this)
                    Toast.makeText(this@MainActivity, "Game Over", Toast.LENGTH_SHORT).show()
                    restartGame()
                } else {
                    val columnIndex = (0 until 8).random()
                    if (!isColumnFull(columnIndex)) {
                        dropLetter(columnIndex)
                    }
                    val delay = when {
                        points >= 400 -> 1000L
                        points >= 300 -> 2000L
                        points >= 200 -> 3000L
                        points >= 100 -> 4000L
                        else -> 5000L
                    }
                    handler.postDelayed(this, delay)
                }
            }
        }
        handler.post(runnable)
    }

    private fun isColumnFull(columnIndex: Int): Boolean {
        val gridLayout = gridLayouts[columnIndex]
        val topCell = gridLayout.getChildAt(0) as TextView
        return topCell.text.isNotEmpty()
    }

    private var isAnimating = false

    private fun dropLetter(columnIndex: Int) {
        if (isAnimating) return

        isAnimating = true
        val letterView = createLetterView()
        val gridLayout = gridLayouts[columnIndex]
        gridLayout.removeViewAt(0)
        gridLayout.addView(letterView, 0)
        animateLetterFall(letterView, columnIndex, 0)
    }


    private fun animateLetterFall(letterView: TextView, columnIndex: Int, rowIndex: Int) {
        val gridLayout = gridLayouts[columnIndex]
        val nextRowIndex = rowIndex + 1
        if (nextRowIndex < 10) {
            val nextCell = gridLayout.getChildAt(nextRowIndex) as TextView
            if (nextCell.text.isEmpty()) {
                gridLayout.removeViewAt(rowIndex)
                gridLayout.addView(createEmptyCell(), rowIndex)

                gridLayout.removeViewAt(nextRowIndex)
                gridLayout.addView(letterView, nextRowIndex)

                Handler(Looper.getMainLooper()).postDelayed({
                    animateLetterFall(letterView, columnIndex, nextRowIndex)
                }, 200)
            } else {
                isAnimating = false
                checkGameOver()
            }
        } else {
            isAnimating = false
            checkGameOver()
        }
    }



    private fun checkGameOver() {
        for (i in 0..7) {
            val topCell = gridLayouts[i].getChildAt(0) as TextView
            if (topCell.text.isNotEmpty()) {
                isGameOver = true
                break
            }
        }
    }
    private fun restartGame() {
        isGameOver = false
        gridLayouts.forEach { gridLayout ->
            gridLayout.removeAllViews()
            initGameBoard(gridLayout)
        }
        startFallingLetters()
    }



    private fun generateRandomLetter(): Char {
        val turkishVowels = "aeıioöuü"
        val turkishConsonants = "bcçdfgğhjklmnprsştvyz"
        val randomValue = Random.nextDouble()

        return if (randomValue <= 0.4) {
            turkishVowels[Random.nextInt(turkishVowels.length)]
        } else {
            turkishConsonants[Random.nextInt(turkishConsonants.length)]
        }
    }

    private fun createLetterView(): TextView {
        val letter = generateRandomLetter()
        val backgroundColor = if (isVowel(letter)) vowelGreen else consoRed
        val borderWidth = dpToPx(1f, this)
        val roundedCornersRadius = dpToPx(8f, this)

        val letterDrawable = GradientDrawable().apply {
            setColor(backgroundColor)
            setStroke(borderWidth, Color.BLACK)
            cornerRadius = roundedCornersRadius.toFloat()
        }

        return TextView(this).apply {
            text = letter.toString()
            textSize = 24f
            gravity = Gravity.CENTER
            background = letterDrawable
            setOnClickListener(letterClickListener)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
    }

    private fun createEmptyCell(): TextView {
        return TextView(this).apply {
            setBackgroundResource(R.drawable.rounded_border)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
        }
    }

    private fun validateAndClearWord() {
        val word = formedWord.text.toString()
        if (englishWords.contains(word.toLowerCase())) {
            removeSelectedLetters()
            clearSelectedLetters()
        }
    }

    private fun removeSelectedLetters() {
        // Calculate points
        selectedLetters.forEach { textView ->
            val letter = textView.text.toString().first()
            val letterPoints = getLetterPoints(letter)
            points += letterPoints
        }
        pointsTextView.text = "Points: $points"

        val removedIndices = selectedLetters.mapNotNull { textView ->
            val columnIndex = gridLayouts.indexOfFirst { it.indexOfChild(textView) != -1 }
            if (columnIndex != -1) {
                val rowIndex = gridLayouts[columnIndex].indexOfChild(textView)
                Pair(columnIndex, rowIndex)
            } else {
                null
            }
        }

        // Clear the selected letters and move the remaining letters down
        removedIndices.forEach { (columnIndex, rowIndex) ->
            val gridLayout = gridLayouts[columnIndex]

            for (i in rowIndex downTo 1) {
                val cell = gridLayout.getChildAt(i) as TextView
                val cellAbove = gridLayout.getChildAt(i - 1) as TextView

                cell.text = cellAbove.text
                cell.background = cellAbove.background
                cell.setOnClickListener(cellAbove.getOnClickListenerForView())

                cellAbove.text = ""
                cellAbove.setBackgroundColor(Color.TRANSPARENT)
                cellAbove.setOnClickListener(null)
            }
        }

        // Clear the selectedLetters list
        selectedLetters.clear()

        checkGameOver()
    }



    private fun View.getOnClickListenerForView(): View.OnClickListener? {
        try {
            val listenerInfoField = View::class.java.getDeclaredField("mListenerInfo")
            listenerInfoField.isAccessible = true
            val listenerInfo = listenerInfoField.get(this)

            val clickListenerField = listenerInfo?.javaClass?.getDeclaredField("mOnClickListener")
            clickListenerField?.isAccessible = true
            return clickListenerField?.get(listenerInfo) as? View.OnClickListener
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


}