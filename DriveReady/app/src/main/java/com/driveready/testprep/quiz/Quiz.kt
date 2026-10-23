package com.driveready.testprep.quiz

import com.driveready.testprep.data.model.Question
import com.driveready.testprep.data.model.QuestionCategory

data class QuizConfig(
    val title: String,
    val questions: List<Question>,
    val passMark: Int,
    val timeLimitSec: Int?,
    /** Practice shows the answer straight away; a mock reveals everything at the end. */
    val showFeedback: Boolean,
    val isMock: Boolean
)

/** A question with its options shuffled once per session; [correctIndex] follows the shuffle. */
data class ShuffledQuestion(val question: Question, val options: List<String>, val correctIndex: Int)

fun shuffleOptions(q: Question, seed: Long): ShuffledQuestion {
    val order = q.options.indices.shuffled(kotlin.random.Random(seed))
    return ShuffledQuestion(q, order.map { q.options[it] }, order.indexOf(q.correctIndex))
}

data class CategoryScore(val category: QuestionCategory, val correct: Int, val total: Int)

data class QuizResult(
    val config: QuizConfig,
    val items: List<ShuffledQuestion>,
    val answers: List<Int?>,
    val elapsedSec: Int
) {
    val total: Int get() = items.size
    val correct: Int get() = items.indices.count { answers.getOrNull(it) == items[it].correctIndex }
    val passed: Boolean get() = correct >= config.passMark

    val byCategory: List<CategoryScore>
        get() = items.indices.groupBy { items[it].question.category }.map { (cat, idx) ->
            CategoryScore(cat, idx.count { answers.getOrNull(it) == items[it].correctIndex }, idx.size)
        }.sortedBy { it.correct.toFloat() / it.total }

    val weakestCategory: QuestionCategory?
        get() = byCategory.firstOrNull { it.correct < it.total }?.category
}
