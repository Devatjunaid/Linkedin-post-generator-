package com.devatjunaid.examclock.domain

/** How far a student has got with one chapter/topic of an exam. Stored as [code] in the database. */
enum class TopicStatus(val code: Int, val weight: Float) {
    NOT_STARTED(0, 0f),
    LEARNING(1, 0.35f),
    REVISING(2, 0.7f),
    DONE(3, 1f);

    /** Status shown after one tap on the status chip. */
    fun next(): TopicStatus = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromCode(code: Int): TopicStatus = entries.firstOrNull { it.code == code } ?: NOT_STARTED
    }
}

/** Kind of study block. Stored as [code] in the database. */
enum class SessionType(val code: Int) {
    STUDY(0),
    REVISION(1),
    PAST_PAPER(2),
    CUSTOM(3);

    companion object {
        fun fromCode(code: Int): SessionType = entries.firstOrNull { it.code == code } ?: CUSTOM
    }
}
