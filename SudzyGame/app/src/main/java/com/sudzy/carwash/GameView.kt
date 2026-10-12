package com.sudzy.carwash

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.sudzy.carwash.engine.Game
import com.sudzy.carwash.engine.Touch

/** Hardware-accelerated view that drives the game loop from vsync. */
@SuppressLint("ViewConstructor")
class GameView(context: Context, private val game: Game) : View(context) {
    private var last = 0L
    private var running = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        game.resize(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 1f / 60f else (now - last) / 1_000_000_000f
        last = now
        game.update(dt)
        game.draw(canvas)
        if (running) postInvalidateOnAnimation()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        // single-finger game: only the first pointer counts
        if (e.actionIndex != 0 && e.actionMasked != MotionEvent.ACTION_MOVE) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> game.touch(Touch.DOWN, e.x, e.y)
            MotionEvent.ACTION_MOVE -> game.touch(Touch.MOVE, e.getX(0), e.getY(0))
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> game.touch(Touch.UP, e.x, e.y)
        }
        return true
    }

    fun resume() { running = true; last = 0L; postInvalidateOnAnimation() }
    fun pause() { running = false }
}
