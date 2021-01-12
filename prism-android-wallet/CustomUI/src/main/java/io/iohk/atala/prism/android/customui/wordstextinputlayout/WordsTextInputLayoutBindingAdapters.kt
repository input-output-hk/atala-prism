package io.iohk.atala.prism.android.customui.wordstextinputlayout

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener

@BindingAdapter("words")
fun setWords(textInputChipsLayout: WordsTextInputLayout, words: List<String>?) {
    words?.let {
        textInputChipsLayout.setWords(it)
    }
}

@InverseBindingAdapter(attribute = "words")
fun getWords(textInputChipsLayout: WordsTextInputLayout): List<String> {
    return textInputChipsLayout.getWords()
}

@BindingAdapter("wordsAttrChanged")
fun setWordsListeners(textInputChipsLayout: WordsTextInputLayout, attrChange: InverseBindingListener) {
    textInputChipsLayout.setOnWordsChangeListener(object : WordsTextInputLayout.OnWordsChangeListener {
        override fun wordsChanged(words: List<String>) {
            attrChange.onChange()
        }

    })
}