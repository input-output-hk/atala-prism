package io.iohk.atala.prism.app.ui.main.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.iohk.atala.prism.app.data.local.preferences.models.CustomDateFormat
import io.iohk.atala.prism.app.neo.common.EventWrapper
import io.iohk.atala.prism.app.neo.common.model.CheckableData
import io.iohk.atala.prism.app.neo.data.PreferencesRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsDateFormatViewModel @Inject constructor(private val repository: PreferencesRepository) : ViewModel() {

    private val _checkableCustomDateFormats = MutableLiveData<List<CheckableData<CustomDateFormat>>>()

    val checkableCustomDateFormats: LiveData<List<CheckableData<CustomDateFormat>>> = _checkableCustomDateFormats

    private var currentDateFormat: CustomDateFormat? = null

    private val _defaultDateFormat = MutableLiveData<CustomDateFormat>()

    val defaultDateFormat: LiveData<CustomDateFormat> = _defaultDateFormat

    private val _preferencesSavedSuccessfully = MutableLiveData<EventWrapper<Boolean>>()

    val preferencesSavedSuccessfully: LiveData<EventWrapper<Boolean>> = _preferencesSavedSuccessfully

    fun loadPreferences() {
        viewModelScope.launch {
            currentDateFormat = repository.getCustomDateFormat()
            _checkableCustomDateFormats.postValue(
                repository.getAvailableCustomDateFormats().map {
                    CheckableData(it, it == currentDateFormat)
                }
            )
            _defaultDateFormat.postValue(repository.getDefaultDateFormat())
        }
    }

    fun savePreferences() {
        currentDateFormat?.let {
            viewModelScope.launch {
                repository.saveCustomDateFormat(it)
                _preferencesSavedSuccessfully.postValue(EventWrapper(true))
            }
        }
    }

    fun selectCustomDateFormat(customDateFormat: CustomDateFormat) {
        val currentList: List<CheckableData<CustomDateFormat>> = checkableCustomDateFormats.value?.toList()
            ?: listOf()
        // deselect
        (currentList.find { it.data == currentDateFormat })?.setChecked(false)
        // select
        (currentList.find { it.data == customDateFormat })?.setChecked(true)
        currentDateFormat = customDateFormat
        _checkableCustomDateFormats.value = currentList
    }
}
