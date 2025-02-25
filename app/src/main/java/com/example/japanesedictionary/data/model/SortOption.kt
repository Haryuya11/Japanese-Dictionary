package com.example.japanesedictionary.data.model

enum class SortOption {
    NAME_ASC {
        override fun toString() = "Name (A-Z)"
    },
    NAME_DESC {
        override fun toString() = "Name (Z-A)"
    },
    DATE_ASC {
        override fun toString() = "Date (Oldest)"
    },
    DATE_DESC {
        override fun toString() = "Date (Newest)"
    }
}
