package com.jeppeman.locallydynamic.integration_tests

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

fun <T> mutableLiveDataOf(initialData: T? = null) = MutableLiveData<T>().apply { value = initialData }

inline fun <T> LiveData<T>.observe(owner: LifecycleOwner, crossinline onChange: (T) -> Unit) {
    this.observe(owner, Observer<T> { data -> onChange(data) })
}

inline fun LiveData<Unit>.observe(owner: LifecycleOwner, crossinline onChange: () -> Unit) {
    this.observe(owner, Observer<Unit> { onChange() })
}

inline fun <T, R> LiveData<T>.map(crossinline transform: (T) -> R): LiveData<R> =
        Transformations.map(this) { data -> transform(data) }

inline fun <T, R> LiveData<T>.switchMap(crossinline transform: (T) -> LiveData<R>): LiveData<R> =
        Transformations.switchMap(this) { data -> transform(data) }