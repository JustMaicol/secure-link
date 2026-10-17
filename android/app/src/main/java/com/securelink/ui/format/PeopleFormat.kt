package com.securelink.ui.format

// spec 100, sección 11 · Personas: singular, plural y el tope de la 001 (255 = "255 o más").
sealed interface PeopleLabel {
    data object One : PeopleLabel

    data class Many(val count: Int) : PeopleLabel

    /** El paquete no distingue más allá del tope: `255 o más personas`. */
    data class AtLeast(val count: Int) : PeopleLabel
}

/** Tope de `people` en el paquete (spec 001, sección 3.1). */
const val PEOPLE_CAP = 255

fun classifyPeople(people: Int): PeopleLabel = when {
    people >= PEOPLE_CAP -> PeopleLabel.AtLeast(PEOPLE_CAP)
    people == 1 -> PeopleLabel.One
    else -> PeopleLabel.Many(people)
}
