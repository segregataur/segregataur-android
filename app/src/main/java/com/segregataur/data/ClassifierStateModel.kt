package com.segregataur.data

data class ClassifierStateModel(
        var classificationStarted: Boolean = false,
        var classificationComplete: Boolean = false,
        var itemsProcessed: Int = 0,
        var totalItemsToProcess: Int = 0,
        var junkSize: Long = 0,
        var displayMessage: String? = null
)
