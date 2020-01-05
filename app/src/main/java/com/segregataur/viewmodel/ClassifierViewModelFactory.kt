package com.segregataur.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.segregataur.core.JunkImageClassifier
import com.segregataur.db.ClassificationDAO

@Suppress("UNCHECKED_CAST")
class ClassifierViewModelFactory(private val junkImageClassifier: JunkImageClassifier,
                                 private val classificationDAO: ClassificationDAO)
    : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ClassifierViewModel(junkImageClassifier, classificationDAO) as T
    }
}