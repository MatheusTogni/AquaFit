package com.aquafit.app.ui

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView

/**
 * Reordena por arraste os itens de uma lista dentro de um ScrollView.
 * Cada item tem uma alça; enquanto arrasta, os vizinhos abrem espaço e a tela rola
 * sozinha perto das bordas. Ao soltar, [onMoved] recebe as posições de origem e destino.
 */
class DragReorder(
    private val scroll: ScrollView,
    private val onMoved: (from: Int, to: Int) -> Unit
) {
    private val items = mutableListOf<View>()
    private var dragging: View? = null
    private var from = 0
    private var to = 0
    private var startRawY = 0f
    private var lastRawY = 0f
    private var startScrollY = 0

    private val autoScroll = object : Runnable {
        override fun run() {
            if (dragging == null) return
            val loc = IntArray(2)
            scroll.getLocationOnScreen(loc)
            val edge = scroll.context.dpf(72f)
            val maxSpeed = scroll.context.dpf(14f)
            val top = loc[1] + edge
            val bottom = loc[1] + scroll.height - edge
            val speed = when {
                lastRawY < top -> -maxSpeed * ((top - lastRawY) / edge).coerceAtMost(1f)
                lastRawY > bottom -> maxSpeed * ((lastRawY - bottom) / edge).coerceAtMost(1f)
                else -> 0f
            }
            if (speed != 0f) {
                scroll.scrollBy(0, speed.toInt())
                update()
            }
            scroll.postOnAnimation(this)
        }
    }

    /** Alça padrão (≡) para colocar no item. */
    fun handle(): TextView = scroll.context.run {
        text("≡", 26f, Palette.MUTED, bold = true).apply {
            gravity = Gravity.CENTER
            contentDescription = "Arrastar para reordenar"
            background = ripple(rounded(0x00000000, dpf(18f)), 0x3300B4D8)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun add(item: View, handle: View) {
        val index = items.size
        items += item
        handle.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    start(index, ev.rawY)
                }
                MotionEvent.ACTION_MOVE -> {
                    lastRawY = ev.rawY
                    update()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> finish()
            }
            true
        }
    }

    private fun start(index: Int, rawY: Float) {
        val v = items[index]
        dragging = v
        from = index
        to = index
        startRawY = rawY
        lastRawY = rawY
        startScrollY = scroll.scrollY
        v.animate().scaleX(1.03f).scaleY(1.03f).translationZ(v.context.dpf(10f)).setDuration(120).start()
        scroll.postOnAnimation(autoScroll)
    }

    private fun update() {
        val v = dragging ?: return
        val dy = lastRawY - startRawY + (scroll.scrollY - startScrollY)
        v.translationY = dy
        val center = v.top + v.height / 2f + dy
        val target = items.indices.count { i -> i != from && items[i].let { it.top + it.height / 2f } < center }
        if (target == to) return
        to = target
        val lp = v.layoutParams as ViewGroup.MarginLayoutParams
        val step = (v.height + lp.topMargin + lp.bottomMargin).toFloat()
        items.forEachIndexed { i, item ->
            if (item === v) return@forEachIndexed
            val shift = when {
                i in (from + 1)..to -> -step
                i in to until from -> step
                else -> 0f
            }
            item.animate().translationY(shift).setDuration(150).start()
        }
    }

    private fun finish() {
        val v = dragging ?: return
        dragging = null
        scroll.removeCallbacks(autoScroll)
        if (from == to) {
            v.animate().translationY(0f).scaleX(1f).scaleY(1f).translationZ(0f).setDuration(150).start()
            return
        }
        items.forEach {
            it.animate().cancel()
            it.translationY = 0f
        }
        v.scaleX = 1f
        v.scaleY = 1f
        v.translationZ = 0f
        onMoved(from, to)
    }
}
