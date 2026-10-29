package com.aquafit.app.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import kotlin.math.abs

/** Ação revelada ao deslizar um card para o lado. [onTrigger] recebe `reset`, que devolve o card ao lugar. */
class SwipeAction(val icon: String, val color: Int, val onTrigger: (reset: () -> Unit) -> Unit)

/**
 * Gestos dos cards de uma lista dentro de um ScrollView:
 * tocar abre, deslizar para o lado revela uma ação e segurar e arrastar reordena
 * (os vizinhos abrem espaço e a tela rola sozinha perto das bordas).
 * Ao soltar um card reordenado, [onMoved] recebe as posições de origem e destino.
 */
class CardList(
    private val scroll: ScrollView,
    private val onMoved: (from: Int, to: Int) -> Unit
) {
    private enum class Mode { NONE, IGNORE, SWIPE, DRAG }

    private val ctx = scroll.context
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private val rows = mutableListOf<View>()
    private var mode = Mode.NONE
    private var downX = 0f
    private var downY = 0f
    private var lastRawY = 0f
    private var pendingLongPress: Runnable? = null

    private var dragging: View? = null
    private var from = 0
    private var to = 0
    private var startRawY = 0f
    private var startScrollY = 0

    private val autoScroll = object : Runnable {
        override fun run() {
            if (dragging == null) return
            val loc = IntArray(2)
            scroll.getLocationOnScreen(loc)
            val edge = ctx.dpf(72f)
            val maxSpeed = ctx.dpf(14f)
            val top = loc[1] + edge
            val bottom = loc[1] + scroll.height - edge
            val speed = when {
                lastRawY < top -> -maxSpeed * ((top - lastRawY) / edge).coerceAtMost(1f)
                lastRawY > bottom -> maxSpeed * ((lastRawY - bottom) / edge).coerceAtMost(1f)
                else -> 0f
            }
            if (speed != 0f) {
                scroll.scrollBy(0, speed.toInt())
                updateDrag()
            }
            scroll.postOnAnimation(this)
        }
    }

    /**
     * Envolve [card] com os fundos das ações e liga os gestos.
     * Devolve a linha que deve ser adicionada à lista no lugar do card.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun add(
        card: View,
        onTap: () -> Unit,
        swipeLeft: SwipeAction? = null,
        swipeRight: SwipeAction? = null
    ): View {
        val index = rows.size
        val row = FrameLayout(ctx).apply {
            layoutParams = (card.layoutParams as? LinearLayout.LayoutParams)?.let { LinearLayout.LayoutParams(it) } ?: lp()
        }
        rows += row

        // Deslizar para a direita revela o fundo da esquerda, e vice-versa.
        val behindLeft = swipeRight?.let { behind(it, Gravity.START) }
        val behindRight = swipeLeft?.let { behind(it, Gravity.END) }
        listOfNotNull(behindLeft, behindRight).forEach { row.addView(it, FrameLayout.LayoutParams(-1, -1)) }
        row.addView(card, FrameLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))

        card.foreground = RippleDrawable(ColorStateList.valueOf(0x2200B4D8), null, rounded(Palette.WHITE, ctx.dpf(20f)))
        card.setOnClickListener { onTap() }
        card.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    mode = Mode.NONE
                    downX = ev.rawX
                    downY = ev.rawY
                    lastRawY = ev.rawY
                    v.drawableHotspotChanged(ev.x, ev.y)
                    v.isPressed = true
                    val longPress = Runnable {
                        pendingLongPress = null
                        v.isPressed = false
                        mode = Mode.DRAG
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        startDrag(index)
                    }
                    pendingLongPress = longPress
                    v.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
                    lastRawY = ev.rawY
                    when (mode) {
                        Mode.NONE -> if (abs(dx) > slop || abs(dy) > slop) {
                            cancelLongPress(v)
                            v.isPressed = false
                            val action = if (dx < 0) swipeLeft else swipeRight
                            mode = if (abs(dx) > abs(dy) && action != null) Mode.SWIPE else Mode.IGNORE
                            if (mode == Mode.SWIPE) v.parent.requestDisallowInterceptTouchEvent(true)
                        }
                        Mode.SWIPE -> {
                            var tx = dx
                            if (swipeLeft == null) tx = tx.coerceAtLeast(0f)
                            if (swipeRight == null) tx = tx.coerceAtMost(0f)
                            v.translationX = tx
                            behindLeft?.visibility = if (tx > 0) View.VISIBLE else View.INVISIBLE
                            behindRight?.visibility = if (tx < 0) View.VISIBLE else View.INVISIBLE
                        }
                        Mode.DRAG -> updateDrag()
                        Mode.IGNORE -> Unit
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelLongPress(v)
                    v.isPressed = false
                    when (mode) {
                        Mode.NONE -> if (ev.actionMasked == MotionEvent.ACTION_UP) v.performClick()
                        Mode.SWIPE -> settle(v, swipeLeft, swipeRight) {
                            behindLeft?.visibility = View.INVISIBLE
                            behindRight?.visibility = View.INVISIBLE
                        }
                        Mode.DRAG -> finishDrag()
                        Mode.IGNORE -> Unit
                    }
                    mode = Mode.NONE
                }
            }
            true
        }
        return row
    }

    private fun behind(action: SwipeAction, side: Int) = FrameLayout(ctx).apply {
        background = rounded(action.color, ctx.dpf(20f))
        visibility = View.INVISIBLE
        addView(
            ctx.text(action.icon, 24f, Palette.WHITE, bold = true),
            FrameLayout.LayoutParams(-2, -2, side or Gravity.CENTER_VERTICAL).apply {
                marginStart = ctx.dp(24)
                marginEnd = ctx.dp(24)
            }
        )
    }

    private fun cancelLongPress(v: View) {
        pendingLongPress?.let { v.removeCallbacks(it) }
        pendingLongPress = null
    }

    /** Solta o card deslizado: passou de 35% da largura dispara a ação, senão volta. */
    private fun settle(card: View, left: SwipeAction?, right: SwipeAction?, hide: () -> Unit) {
        val tx = card.translationX
        val action = if (tx < 0) left else right
        val reset = { card.animate().translationX(0f).setDuration(180).withEndAction { hide() }.start() }
        if (action == null || abs(tx) < card.width * 0.35f) {
            reset()
            return
        }
        val out = if (tx < 0) -card.width.toFloat() else card.width.toFloat()
        card.animate().translationX(out).setDuration(150).withEndAction { action.onTrigger(reset) }.start()
    }

    private fun startDrag(index: Int) {
        val v = rows[index]
        dragging = v
        from = index
        to = index
        startRawY = lastRawY
        startScrollY = scroll.scrollY
        v.animate().scaleX(1.03f).scaleY(1.03f).translationZ(ctx.dpf(10f)).setDuration(120).start()
        scroll.postOnAnimation(autoScroll)
    }

    private fun updateDrag() {
        val v = dragging ?: return
        val dy = lastRawY - startRawY + (scroll.scrollY - startScrollY)
        v.translationY = dy
        val center = v.top + v.height / 2f + dy
        val target = rows.indices.count { i -> i != from && rows[i].let { it.top + it.height / 2f } < center }
        if (target == to) return
        to = target
        val lp = v.layoutParams as ViewGroup.MarginLayoutParams
        val step = (v.height + lp.topMargin + lp.bottomMargin).toFloat()
        rows.forEachIndexed { i, row ->
            if (row === v) return@forEachIndexed
            val shift = when {
                i in (from + 1)..to -> -step
                i in to until from -> step
                else -> 0f
            }
            row.animate().translationY(shift).setDuration(150).start()
        }
    }

    private fun finishDrag() {
        val v = dragging ?: return
        dragging = null
        scroll.removeCallbacks(autoScroll)
        if (from == to) {
            v.animate().translationY(0f).scaleX(1f).scaleY(1f).translationZ(0f).setDuration(150).start()
            return
        }
        rows.forEach {
            it.animate().cancel()
            it.translationY = 0f
        }
        v.scaleX = 1f
        v.scaleY = 1f
        v.translationZ = 0f
        onMoved(from, to)
    }
}
