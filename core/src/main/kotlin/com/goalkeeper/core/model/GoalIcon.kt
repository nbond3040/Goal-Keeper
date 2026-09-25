package com.goalkeeper.core.model

/**
 * Icon choices for a goal. The app maps each key to a Material icon; the enum name is what
 * gets persisted, so never rename an entry — only append new ones.
 */
enum class GoalIcon(val label: String) {
    TARGET("Target"),
    RUN("Running"),
    FITNESS("Fitness"),
    BIKE("Cycling"),
    SWIM("Swimming"),
    HIKE("Hiking"),
    BOOK("Reading"),
    STUDY("Study"),
    LANGUAGE("Language"),
    WRITE("Writing"),
    CODE("Code"),
    WORK("Career"),
    MONEY("Money"),
    SAVINGS("Savings"),
    MEDITATE("Mindfulness"),
    SLEEP("Sleep"),
    WATER("Hydration"),
    FOOD("Nutrition"),
    NO_SMOKING("Quit habit"),
    MUSIC("Music"),
    ART("Art"),
    CAMERA("Photography"),
    TRAVEL("Travel"),
    HOME("Home"),
    PLANT("Garden"),
    FAMILY("Family"),
    HEART("Health"),
    PETS("Pets"),
    STAR("Other");

    companion object {
        fun fromKey(key: String?): GoalIcon = entries.firstOrNull { it.name == key } ?: TARGET
    }
}
