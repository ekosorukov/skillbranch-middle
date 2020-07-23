package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    var lastIndex = 0;

    this.asReversed().forEachIndexed { index, item ->
        run {
            if (predicate.invoke(item)) {
                lastIndex = this.size - index - 1
                return this.subList(0, lastIndex)
            }
        }
    }

    return emptyList()
}