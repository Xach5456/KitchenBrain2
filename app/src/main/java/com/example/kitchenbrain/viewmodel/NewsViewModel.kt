package com.example.kitchenbrain.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenbrain.models.Article
import com.example.kitchenbrain.repository.NewsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * NewsViewModel - Logic for News System
 * 
 * ✅ FEATURES:
 * - State management for UI (Loading, Success, Error)
 * - Automatic refresh on start
 * - Manual refresh support
 */
class NewsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NewsRepositoryImpl(application)
    
    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private val _news = MutableStateFlow<List<Article>>(emptyList())
    val news: StateFlow<List<Article>> = _news.asStateFlow()

    init {
        observeNews()
        refresh()
    }

    private fun observeNews() {
        viewModelScope.launch {
            repository.getNewsFlow().collect {
                _news.value = it
                if (it.isNotEmpty() && _uiState.value is NewsUiState.Loading) {
                    _uiState.value = NewsUiState.Success
                } else if (it.isEmpty() && _uiState.value is NewsUiState.Loading) {
                    // Stay in loading until first API call or if DB is truly empty
                }
            }
        }
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch {
            if (_news.value.isEmpty()) _uiState.value = NewsUiState.Loading
            
            val result = repository.refreshNews(force)
            if (result.isSuccess) {
                _uiState.value = NewsUiState.Success
            } else {
                if (_news.value.isEmpty()) {
                    _uiState.value = NewsUiState.Error(result.exceptionOrNull()?.message ?: "Unknown Error")
                }
                // If we have cached news, we just show a toast/silent error in the UI layer
            }
        }
    }

    sealed class NewsUiState {
        object Loading : NewsUiState()
        object Success : NewsUiState()
        object Empty : NewsUiState()
        data class Error(val message: String) : NewsUiState()
    }
}
