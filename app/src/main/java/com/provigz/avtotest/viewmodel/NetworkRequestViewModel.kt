package com.provigz.avtotest.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provigz.avtotest.network.newRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val BASE_URL = "https://avtoizpit.com/api/"

class NetworkRequestViewModel<T>(
    private val endpoint: String,
    private val method: String,
    private val request: Any,
    private val responseClass: Class<T>
) : ViewModel() {
    private var _responseState = MutableStateFlow<T?>(null)
    val responseState: StateFlow<T?> = _responseState

    private var _finishedState = MutableStateFlow(false)
    val finishedState: StateFlow<Boolean> = _finishedState

    private var _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    fun fetch() {
        viewModelScope.launch {
            try {
                val response = newRequest(
                    url = BASE_URL + endpoint,
                    method,
                    request,
                    responseClass
                )
                _responseState.value = response
                _finishedState.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _errorState.value = e.message
            }
        }
    }
}