package com.example.domain.model

enum class PageFormat(
    val title: String,
    val subtitle: String
) {
    BOOK(
        title = "Книга",
        subtitle = "Тёплая книжная бумага, переплёт и книжная верстка"
    ),
    RULED(
        title = "В линейку",
        subtitle = "Тетрадь в строчку с красным полем слева"
    ),
    GRID(
        title = "В клеточку",
        subtitle = "Классическая тетрадь в клетку с полями"
    ),
    BLANK(
        title = "Чистый лист",
        subtitle = "Гладкая страница без разлиновки"
    )
}
