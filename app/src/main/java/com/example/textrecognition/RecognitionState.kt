package com.example.textrecognition

sealed class RecognitionState{
    object idle : RecognitionState()
    object loading : RecognitionState()
    data class Success(val text : String) : RecognitionState()
    data class error (val message : String) : RecognitionState()
}