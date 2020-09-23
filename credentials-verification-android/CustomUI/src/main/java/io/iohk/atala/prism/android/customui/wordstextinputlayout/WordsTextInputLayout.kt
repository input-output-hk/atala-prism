package io.iohk.atala.prism.android.customui.wordstextinputlayout

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.google.android.flexbox.*
import com.google.android.material.chip.Chip
import com.google.android.material.resources.MaterialResources
import io.iohk.atala.prism.android.customui.R

class WordsTextInputLayout : FlexboxLayout {

    /*
    * Chip´s Styling
    * */
    private var chipsBackgroundColor: ColorStateList? = null
    private var chipsTextColor: Int? = null
    private var chipsCloseIconTint: ColorStateList? = null
    private var chipsCloseIcon: Drawable? = null

    private var maxWords: Int = -1
    private var maxLengthPerWord: Int = -1

    // 4dps between each child view
    private val itemsMargin: Int = (4 * Resources.getSystem().displayMetrics.scaledDensity).toInt()

    private var editText: EditText? = null

    // All child minus the editText
    private val totalOfChips: Int
        get() = childCount - 1

    private var onWordsChangeListener: OnWordsChangeListener? = null

    private val wordsList: MutableList<String> = mutableListOf()

    // Is full when a maxWords has been set and the total of words is greater than or equal to this variable
    private val isFull: Boolean
        get() = maxWords > 0 && wordsList.size >= maxWords

    constructor(context: Context) : super(context) {
        initialConfiguration(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialConfiguration(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialConfiguration(attrs)
    }


    private fun initialConfiguration(attrs: AttributeSet?) {
        alignContent = AlignContent.STRETCH
        alignItems = AlignItems.STRETCH
        flexWrap = FlexWrap.WRAP
        flexDirection = FlexDirection.ROW
        handleAttrs(attrs)
        buildEditText()
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        return editText?.requestFocus(direction, previouslyFocusedRect) ?: true
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if ((child is EditText && child == this.editText) || child is Chip) {
            super.addView(child, index, params)
        } else {
            throw IllegalArgumentException("Only can add Chip´s childs")
        }
    }

    @SuppressLint("ResourceType")
    private fun handleAttrs(attrs: AttributeSet?) {
        attrs?.let {
            context.theme.obtainStyledAttributes(it, R.styleable.WordsTextInputLayout, 0, 0).apply {
                try {
                    maxWords = getInteger(R.styleable.WordsTextInputLayout_maxWords, -1)
                    maxLengthPerWord = getInteger(R.styleable.WordsTextInputLayout_maxLengthPerWord, -1)
                    handleChipsAttributes(this)
                } finally {
                    recycle()
                }
            }
        }
    }


    @SuppressLint("RestrictedApi")
    private fun handleChipsAttributes(typedArray: TypedArray) {
        if (typedArray.hasValue(R.styleable.WordsTextInputLayout_chipsTextColor)) {
            chipsTextColor = typedArray.getColor(R.styleable.WordsTextInputLayout_chipsTextColor, 0)
        }
        chipsBackgroundColor = MaterialResources.getColorStateList(context, typedArray, R.styleable.WordsTextInputLayout_chipsBackgroundColor)
        chipsCloseIconTint = MaterialResources.getColorStateList(context, typedArray, R.styleable.WordsTextInputLayout_chipsCloseIconTint)
        chipsCloseIcon = MaterialResources.getDrawable(context, typedArray, R.styleable.WordsTextInputLayout_chipsCloseIcon)
    }

    /*
    * Build the editText and add it to this layout
    * */
    private fun buildEditText() {
        editText = EditText(context)
        editText?.setBackgroundColor(Color.TRANSPARENT)
        val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        layoutParams.marginStart = itemsMargin * 2
        layoutParams.marginEnd = itemsMargin * 2
        layoutParams.flexGrow = 1f
        addView(editText, 0, layoutParams)
        configureEditTextWatchers()
    }


    private fun configureEditTextWatchers() {
        editText?.addTextChangedListener(textWatcher)
        editText?.setOnKeyListener { _, keyCode, _ ->
            val editTextIsBlank = editText?.text?.isBlank() ?: true
            if (keyCode == KeyEvent.KEYCODE_DEL && editTextIsBlank && wordsList.size > 0) {
                editLastWord()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }

    private fun editLastWord() {
        val lastWord = wordsList.last()
        editText?.setText(lastWord)
        editText?.setSelection(lastWord.length)
        removeWordAt(wordsList.lastIndex)
    }

    private fun handleEditTextVisibility() {
        if (isFull) {
            editText?.visibility = View.GONE
            editText?.isEnabled = false
            editText?.setText("")
        } else {
            editText?.isEnabled = true
            editText?.visibility = View.VISIBLE
            editText?.requestFocus()
        }
    }

    fun addWords(words: List<String>) {
        words.forEach { word ->
            fixAndAddWordToWordsList(word)
        }
        updateChips()
        onWordsChangeListener?.wordsChanged(wordsList)
    }

    fun setWords(words: List<String>) {
        if (words == wordsList) {
            return
        }
        wordsList.clear()
        words.forEach {
            fixAndAddWordToWordsList(it)
        }
        onWordsChangeListener?.wordsChanged(wordsList)
        updateChips()
    }

    fun removeWordAt(index: Int) {
        wordsList.removeAt(index)
        updateChips()
        onWordsChangeListener?.wordsChanged(wordsList)
        handleEditTextVisibility()
    }

    fun getWords(): List<String> = wordsList.toList()

    fun setOnWordsChangeListener(onWordsChangeListener: OnWordsChangeListener?) {
        this.onWordsChangeListener = onWordsChangeListener
    }

    private fun fixAndAddWordToWordsList(text: String): String? {
        if (isFull) {
            return null
        }
        var word = text.trim()
        if (word.isEmpty()) {
            return null
        }
        if (maxLengthPerWord > 0 && maxLengthPerWord < word.length) {
            word = word.dropLast(word.length - maxLengthPerWord)
        }
        wordsList.add(word)
        handleEditTextVisibility()
        return word
    }

    @SuppressLint("SetTextI18n")
    private fun updateChips() {
        val totalWords = wordsList.size
        for (i in 0 until totalWords) {
            val word = wordsList[i]
            val chip = getOrCreateChipAt(i)
            chip.text = "${i + 1}. $word"
        }
        // remove leftover chip´s
        for (i in totalWords until totalOfChips) {
            removeViewAt(i)
        }
    }

    private fun getOrCreateChipAt(index: Int): Chip {
        val weHaveAChipCreated = index + 1 <= totalOfChips
        val chip = if (weHaveAChipCreated) getChildAt(index) as Chip else createChip()
        if (!weHaveAChipCreated) {
            addView(chip, index)
        }
        return chip
    }

    private fun createChip(): Chip {

        val chip = Chip(context, null, 0)

        chipsBackgroundColor?.let { chip.chipBackgroundColor = chipsBackgroundColor }
        chipsTextColor?.let { chip.setTextColor(it) }
        chipsCloseIconTint?.let { chip.closeIconTint = it }
        chipsCloseIcon?.let { chip.closeIcon = it }

        val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        chip.isCloseIconVisible = true
        val margin = itemsMargin
        params.setMargins(margin, 0, margin, 0)
        chip.layoutParams = params
        chip.setOnCloseIconClickListener {
            val index: Int = indexOfChild(chip as View)
            removeWordAt(index)
        }
        return chip
    }


    private val textWatcher: TextWatcher = object : TextWatcher {

        val whiteSpaceRegex = "\\s+".toRegex()

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            s?.let { editable ->
                if (editable.contains(whiteSpaceRegex)) {
                    val words = editable.replace(whiteSpaceRegex, " ").split(" ").filter { it != "" }
                    addWords(words)
                    editText?.setText("")
                }
            }
        }
    }

    interface OnWordsChangeListener {
        fun wordsChanged(words: List<String>)
    }
}