package com.aquafit.app.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.sin

/** Cores da praia: mar profundo, turquesa, espuma, céu, areia, coral e sol. */
object Palette {
    val DEEP = 0xFF023E8A.toInt()
    val OCEAN = 0xFF0077B6.toInt()
    val TURQUOISE = 0xFF00B4D8.toInt()
    val FOAM = 0xFF90E0EF.toInt()
    val SKY = 0xFFE0F7FA.toInt()
    val SAND = 0xFFFFF3E0.toInt()
    val SAND_DARK = 0xFFFFE0B2.toInt()
    val CORAL = 0xFFFF8C42.toInt()
    val SUN = 0xFFFFD166.toInt()
    val WHITE = 0xFFFFFFFF.toInt()
    val TEXT = 0xFF023E8A.toInt()
    val MUTED = 0xFF5C7C99.toInt()
    val DONE_BG = 0xFFDFF7F1.toInt()
    val DANGER = 0xFFE5534B.toInt()
}

const val INPUT_TEXT = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
const val INPUT_MULTILINE = INPUT_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
const val INPUT_INT = InputType.TYPE_CLASS_NUMBER
const val INPUT_DECIMAL_SIGNED =
    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density

fun rounded(color: Int, radius: Float, strokeColor: Int? = null, strokeWidth: Int = 0) =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokeColor != null) setStroke(strokeWidth, strokeColor)
    }

fun ripple(base: Drawable, rippleColor: Int = 0x33FFFFFF) =
    RippleDrawable(ColorStateList.valueOf(rippleColor), base, null)

fun lp(
    width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
    height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    weight: Float = 0f
) = LinearLayout.LayoutParams(width, height, weight)

fun LinearLayout.LayoutParams.margins(l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
    apply { setMargins(l, t, r, b) }

fun Context.text(
    value: String,
    size: Float = 15f,
    color: Int = Palette.TEXT,
    bold: Boolean = false
) = TextView(this).apply {
    text = value
    textSize = size
    setTextColor(color)
    if (bold) typeface = Typeface.DEFAULT_BOLD
}

fun Context.vertical(padding: Int = 0) = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(padding, padding, padding, padding)
}

fun Context.horizontal() = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
}

/** Card branco arredondado, como uma prancha flutuando no mar. */
fun Context.card(): LinearLayout = vertical(dp(16)).apply {
    background = rounded(Palette.WHITE, dpf(20f), 0x2290E0EF, dp(1))
    elevation = dpf(3f)
    layoutParams = lp().margins(t = dp(6), b = dp(6))
}

enum class BtnStyle { PRIMARY, SECONDARY, OUTLINE, DANGER, GHOST }

fun Context.button(label: String, style: BtnStyle = BtnStyle.PRIMARY, onClick: () -> Unit): TextView =
    TextView(this).apply {
        text = label
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(11), dp(16), dp(11))
        val radius = dpf(24f)
        val (bg, fg, rip) = when (style) {
            BtnStyle.PRIMARY -> Triple(rounded(Palette.CORAL, radius), Palette.WHITE, 0x44FFFFFF)
            BtnStyle.SECONDARY -> Triple(rounded(Palette.TURQUOISE, radius), Palette.WHITE, 0x44FFFFFF)
            BtnStyle.OUTLINE -> Triple(rounded(Palette.WHITE, radius, Palette.TURQUOISE, dp(2)), Palette.OCEAN, 0x3300B4D8)
            BtnStyle.DANGER -> Triple(rounded(Palette.WHITE, radius, Palette.DANGER, dp(2)), Palette.DANGER, 0x33E5534B)
            BtnStyle.GHOST -> Triple(rounded(0x00000000, radius), Palette.OCEAN, 0x3300B4D8)
        }
        background = ripple(bg, rip)
        setTextColor(fg)
        isClickable = true
        setOnClickListener { onClick() }
    }

fun Context.input(hint: String, value: String = "", type: Int = INPUT_TEXT): EditText =
    EditText(this).apply {
        this.hint = hint
        setText(value)
        inputType = type
        textSize = 16f
        setTextColor(Palette.TEXT)
        setHintTextColor(Palette.MUTED)
        background = rounded(Palette.WHITE, dpf(12f), Palette.FOAM, dp(2))
        setPadding(dp(14), dp(10), dp(14), dp(10))
    }

/** Rótulo pequeno acima de um campo. */
fun Context.field(label: String, view: View): LinearLayout = vertical().apply {
    addView(text(label, 12f, Palette.MUTED, bold = true), lp().margins(b = dp(4)))
    addView(view, lp())
    layoutParams = lp().margins(t = dp(10))
}

/** Etiqueta arredondada (ex.: "3 exercícios"). */
fun Context.badge(label: String, bg: Int = Palette.SKY, fg: Int = Palette.OCEAN): TextView =
    text(label, 12f, fg, bold = true).apply {
        background = rounded(bg, dpf(12f))
        setPadding(dp(10), dp(4), dp(10), dp(4))
    }

fun Context.dialogBuilder(): AlertDialog.Builder =
    AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)

fun Context.confirm(title: String, message: String, okLabel: String, onDismiss: () -> Unit = {}, onOk: () -> Unit) {
    dialogBuilder()
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(okLabel) { _, _ -> onOk() }
        .setNegativeButton("Cancelar", null)
        .setOnDismissListener { onDismiss() }
        .show()
}

/** Ondas desenhadas na base do cabeçalho. */
class WaveView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawWave(canvas, 0x9990E0EF.toInt(), 0.62f, 1.3f)
        drawWave(canvas, Palette.TURQUOISE, 0.38f, 0f)
    }

    private fun drawWave(canvas: Canvas, color: Int, level: Float, phase: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        val mid = h * level
        val amp = h * 0.28f
        val wavelength = w / 1.6f
        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(w, 0f)
        var x = w
        while (x >= 0f) {
            path.lineTo(x, mid + amp * sin(2 * PI * x / wavelength + phase).toFloat())
            x -= 4f
        }
        path.lineTo(0f, mid + amp * sin(phase.toDouble()).toFloat())
        path.close()
        paint.color = color
        canvas.drawPath(path, paint)
    }
}

/**
 * Base das telas: fundo céu→areia, cabeçalho oceano com ondas, conteúdo rolável,
 * barra inferior opcional e botão flutuante opcional.
 */
abstract class OceanActivity : Activity() {
    protected lateinit var content: LinearLayout
    protected lateinit var scroll: ScrollView
    private lateinit var frame: FrameLayout
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var noteView: TextView
    private lateinit var actionsBox: LinearLayout
    private lateinit var bottomBar: LinearLayout

    protected fun setupScreen(title: String, subtitle: String? = null, showBack: Boolean = true) {
        window.statusBarColor = Palette.OCEAN
        window.navigationBarColor = Palette.SAND

        frame = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Palette.SKY, Palette.SAND)
            )
        }
        val column = vertical()

        val header = vertical().apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Palette.OCEAN, Palette.TURQUOISE)
            )
            setPadding(dp(12), dp(14), dp(12), dp(4))
        }
        val row = horizontal()
        if (showBack) {
            row.addView(
                text("←", 24f, Palette.WHITE, bold = true).apply {
                    setPadding(dp(8), dp(2), dp(12), dp(2))
                    background = ripple(rounded(0x00000000, dpf(20f)))
                    setOnClickListener { onBackPressed() }
                }
            )
        }
        val titles = vertical()
        titleView = text(title, 22f, Palette.WHITE, bold = true)
        subtitleView = text(subtitle.orEmpty(), 13f, 0xDDFFFFFF.toInt()).apply {
            visibility = if (subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        noteView = text("", 11f, 0xAAFFFFFF.toInt()).apply { visibility = View.GONE }
        titles.addView(titleView)
        titles.addView(subtitleView)
        titles.addView(noteView)
        row.addView(titles, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = if (showBack) 0 else dp(6)))
        actionsBox = horizontal()
        row.addView(actionsBox)
        header.addView(row)
        column.addView(header)
        column.addView(WaveView(this), lp(height = dp(26)))

        scroll = ScrollView(this).apply { isFillViewport = true }
        content = vertical().apply { setPadding(dp(16), 0, dp(16), dp(100)) }
        scroll.addView(content)
        column.addView(scroll, lp(height = 0, weight = 1f))

        bottomBar = horizontal().apply {
            setPadding(dp(12), dp(10), dp(12), dp(12))
            background = rounded(Palette.WHITE, dpf(0f))
            elevation = dpf(8f)
            visibility = View.GONE
        }
        column.addView(bottomBar)

        frame.addView(column, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
    }

    protected fun setHeader(title: String, subtitle: String? = null) {
        titleView.text = title
        subtitleView.text = subtitle.orEmpty()
        subtitleView.visibility = if (subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    /** Linha discreta abaixo do subtítulo (ex.: versão do app). */
    protected fun setHeaderNote(note: String) {
        noteView.text = note
        noteView.visibility = if (note.isEmpty()) View.GONE else View.VISIBLE
    }

    protected fun addHeaderAction(label: String, onClick: () -> Unit) {
        actionsBox.addView(
            text(label, 14f, Palette.WHITE, bold = true).apply {
                setPadding(dp(12), dp(7), dp(12), dp(7))
                background = ripple(rounded(0x33FFFFFF, dpf(18f)))
                setOnClickListener { onClick() }
            },
            lp(ViewGroup.LayoutParams.WRAP_CONTENT).margins(l = dp(6))
        )
    }

    protected fun addBottomButton(label: String, style: BtnStyle, onClick: () -> Unit) {
        bottomBar.visibility = View.VISIBLE
        bottomBar.addView(
            button(label, style, onClick),
            lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).margins(l = dp(4), r = dp(4))
        )
    }

    protected fun addFab(label: String, onClick: () -> Unit) {
        val size = dp(62)
        val fab = text(label, 30f, Palette.WHITE, bold = true).apply {
            gravity = Gravity.CENTER
            background = ripple(GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Palette.CORAL)
            })
            elevation = dpf(8f)
            setOnClickListener { onClick() }
        }
        frame.addView(fab, FrameLayout.LayoutParams(size, size, Gravity.BOTTOM or Gravity.END).apply {
            setMargins(0, 0, dp(20), dp(24))
        })
    }

    /** Estado vazio com um toque de praia. */
    protected fun emptyState(emoji: String, title: String, message: String) {
        content.addView(vertical(dp(24)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(text(emoji, 56f).apply { gravity = Gravity.CENTER })
            addView(text(title, 18f, Palette.DEEP, bold = true).apply { gravity = Gravity.CENTER }, lp().margins(t = dp(8)))
            addView(text(message, 14f, Palette.MUTED).apply { gravity = Gravity.CENTER }, lp().margins(t = dp(6)))
        }, lp().margins(t = dp(40)))
    }

    protected fun sectionTitle(label: String) {
        content.addView(text(label, 13f, Palette.OCEAN, bold = true).apply {
            letterSpacing = 0.08f
        }, lp().margins(t = dp(14), b = dp(2), l = dp(4)))
    }

    /** Dica curta abaixo do título da seção (ex.: como usar os gestos). */
    protected fun hint(label: String) {
        content.addView(text(label, 12f, Palette.MUTED), lp().margins(b = dp(4), l = dp(4)))
    }
}
