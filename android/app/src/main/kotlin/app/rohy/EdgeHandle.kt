package app.rohy

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * The edge handle of phase 10: a thin bar on the right edge, drawn over other apps while the phone is
 * connected. Swiping it toward the middle opens a tab "Envoyer à l'ordinateur" that slides out of the edge.
 * The tab's window takes the focus, which is what lets Rohy read the clipboard (Android 10+); the transparent
 * activity of the tile is the fallback. It is turned off from the home screen's switch only (no ✕ on the tab).
 * The window keeps the bar's height and place in every state, so opening never moves it; only its width
 * follows the content, which stays anchored to the right edge.
 * Main thread only. Board: "Rohy Android — refonte", row "Poignée « Envoyer à l'ordinateur »".
 */
class EdgeHandle(
    private val context: Context,
    private val storage: Storage,
    private val onSend: (String) -> Unit,
) {
    private val windows = context.getSystemService(WindowManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val fold = Runnable { show(HandleMode.REST) }
    private var root: Root? = null
    private var params: WindowManager.LayoutParams? = null
    private var mode = HandleMode.REST

    /** Shows or removes the handle; called whenever the connection or the settings change. */
    fun update(visible: Boolean) {
        if (visible) attach() else detach()
    }

    /** After a rotation or a light/dark switch: same relative place, current colors. */
    fun reposition() {
        if (root == null) return
        render()
        place()
    }

    private fun attach() {
        if (root != null) return
        val view = Root(context)
        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            dp(AREA_HEIGHT),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            BASE_FLAGS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            title = "Rohy"
        }
        root = view
        params = layout
        mode = HandleMode.REST
        render()
        layout.y = top()
        if (runCatching { windows.addView(view, layout) }.isFailure) {
            root = null
            params = null
        }
    }

    private fun detach() {
        main.removeCallbacks(fold)
        root?.let { runCatching { windows.removeViewImmediate(it) } }
        root = null
        params = null
    }

    private fun show(next: HandleMode) {
        if (root == null) return
        main.removeCallbacks(fold)
        val opening = mode == HandleMode.REST && next == HandleMode.OPEN
        mode = next
        render()
        if (opening) root?.getChildAt(0)?.let(::slideIn)
        place()
        EdgeHandleRules.timeoutMillis(next)?.let { main.postDelayed(fold, it) }
    }

    /** Only the open tab may take the focus: the bar must never steal it from the app below. */
    private fun place() {
        val view = root ?: return
        val layout = params ?: return
        layout.flags = if (mode == HandleMode.OPEN) BASE_FLAGS else BASE_FLAGS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        layout.y = top()
        runCatching { windows.updateViewLayout(view, layout) }
    }

    /** Top of the window, the same for the bar and the tab: the tab is centered on the bar, where the finger is. */
    private fun top(): Int = EdgeHandleRules.topOffset(storage.handlePosition, availableHeight(), dp(AREA_HEIGHT))

    /** Like a drawer pulled from the edge: the tab enters from the right, before its first frame is drawn. */
    private fun slideIn(view: View) {
        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)
                view.translationX = view.width.toFloat()
                view.animate().translationX(0f).setDuration(SLIDE_MS).setInterpolator(DecelerateInterpolator()).start()
                return true
            }
        })
    }

    private fun availableHeight(): Int =
        if (Build.VERSION.SDK_INT >= 30) windows.currentWindowMetrics.bounds.height()
        else context.resources.displayMetrics.heightPixels

    private fun render() {
        val view = root ?: return
        view.removeAllViews()
        val anchored = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL,
        )
        if (mode == HandleMode.REST) view.addView(bar(), FrameLayout.LayoutParams(dp(AREA_WIDTH), dp(AREA_HEIGHT), Gravity.END))
        else view.addView(tab(), anchored)
    }

    // The bar.

    /**
     * Same look and gesture as the Samsung Edge panel handle: thin light gray bar, opened by a swipe toward
     * the middle of the screen, never by a tap (typing near the edge must not open it). TalkBack still opens
     * it with its click action.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun bar(): View {
        val area = FrameLayout(context).apply {
            contentDescription = "Rohy : glisser vers le centre pour envoyer le presse-papiers à l'ordinateur"
            isClickable = true
        }
        val line = View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(2).toFloat()
                setColor(BAR_FILL)
            }
        }
        area.addView(line, FrameLayout.LayoutParams(dp(3), dp(50), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(4)
        })
        area.setOnClickListener { show(HandleMode.OPEN) }
        area.setOnTouchListener(SwipeOrMove(area))
        // The swipe starts on the edge, where Android otherwise sees its Back gesture: keep this small area for us.
        area.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            if (Build.VERSION.SDK_INT >= 29) view.systemGestureExclusionRects = listOf(android.graphics.Rect(0, 0, view.width, view.height))
        }
        return area
    }

    /**
     * A swipe toward the middle of the screen opens the tab; a tap does nothing; a long press then a vertical
     * drag moves the handle along the edge.
     */
    private inner class SwipeOrMove(private val area: View) : View.OnTouchListener {
        private val slop = ViewConfiguration.get(context).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var startTop = 0
        private var dragging = false
        private var moved = false
        private val startDrag = Runnable {
            dragging = true
            area.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val layout = params ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startTop = layout.y
                    dragging = false
                    moved = false
                    main.postDelayed(startDrag, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (dragging) {
                        val max = (availableHeight() - dp(AREA_HEIGHT)).coerceAtLeast(0)
                        layout.y = (startTop + dy).toInt().coerceIn(0, max)
                        runCatching { windows.updateViewLayout(root, layout) }
                    } else if (abs(dy) > slop || abs(dx) > slop) {
                        moved = true
                        main.removeCallbacks(startDrag)
                        if (EdgeHandleRules.opensTab(dx = dx, dy = dy, threshold = dp(SWIPE_DISTANCE).toFloat())) {
                            show(HandleMode.OPEN)
                            return true
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    main.removeCallbacks(startDrag)
                    if (dragging) {
                        storage.handlePosition = EdgeHandleRules.positionFrom(layout.y, availableHeight(), dp(AREA_HEIGHT))
                    }
                }
                MotionEvent.ACTION_CANCEL -> main.removeCallbacks(startDrag)
            }
            return true
        }
    }

    // The tab.

    private fun tab(): View {
        val colors = TabColors.of(mode, dark())
        val tab = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(TAB_HEIGHT)
            background = GradientDrawable().apply {
                val r = dp(TAB_HEIGHT / 2).toFloat()
                cornerRadii = floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
                setColor(colors.background)
                setStroke(dp(1), colors.ring)
            }
        }
        val (icon, title, subtitle) = when (mode) {
            HandleMode.SENT -> Triple(R.drawable.ic_handle_check, "Envoyé", storage.pairedMac?.displayName)
            HandleMode.EMPTY -> Triple(R.drawable.ic_handle_clipboard, "Rien à envoyer", "Copie d'abord un texte")
            else -> Triple(R.drawable.ic_handle_send, "Envoyer à l'ordinateur", null)
        }
        val action = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(14), 0)
            minimumHeight = dp(TAB_HEIGHT)
            addView(iconDisc(icon, colors), LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(10) })
            addView(texts(title, subtitle, colors.text))
        }
        tab.addView(action)
        if (mode == HandleMode.OPEN) {
            action.contentDescription = "Envoyer le presse-papiers à l'ordinateur"
            action.isClickable = true
            action.setOnClickListener { sendClipboard() }
        } else {
            tab.isFocusable = true
            tab.contentDescription = listOfNotNull(title, subtitle).joinToString(", ")
        }
        return tab
    }

    private fun iconDisc(icon: Int, colors: TabColors) = ImageView(context).apply {
        setImageResource(icon)
        imageTintList = ColorStateList.valueOf(colors.iconTint)
        scaleType = ImageView.ScaleType.CENTER
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colors.iconBackground)
        }
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun texts(title: String, subtitle: String?, color: Int) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        addView(label(title, 14f, R.font.inter_semibold, color))
        subtitle?.let { addView(label(it, 12f, R.font.inter_medium, color).apply { alpha = 0.85f }) }
    }

    private fun label(text: String, size: Float, font: Int, color: Int) = TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        typeface = context.resources.getFont(font)
        setTextColor(color)
        maxLines = 1
    }

    /** Reads the clipboard now that the tab's window has the focus, then shows the result. */
    private fun sendClipboard() {
        val focused = root?.hasWindowFocus() == true
        val text = context.getSystemService(ClipboardManager::class.java).primaryClip
            ?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()?.takeIf { it.isNotEmpty() }
        if (text == null && !focused) {
            // Android did not give the focus to the tab: let the tile's transparent activity read instead.
            context.startActivity(Intent(context, SendClipboardActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return show(HandleMode.REST)
        }
        show(EdgeHandleRules.resultMode(text))
        text?.let(onSend)
    }

    private fun dark() =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    /** Root of the handle's window: Back folds the tab, a touch outside it too. */
    private inner class Root(context: Context) : FrameLayout(context) {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && mode != HandleMode.REST) {
                if (event.action == KeyEvent.ACTION_UP) show(HandleMode.REST)
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE && mode == HandleMode.OPEN) show(HandleMode.REST)
            return super.onTouchEvent(event)
        }
    }

    /** Colors of the board for each tab state, in light and dark mode. */
    private data class TabColors(
        val background: Int, val ring: Int, val text: Int, val iconBackground: Int, val iconTint: Int, val divider: Int,
    ) {
        companion object {
            fun of(mode: HandleMode, dark: Boolean): TabColors = when (mode) {
                HandleMode.SENT -> TabColors(0xFF1E6B45.toInt(), 0x1AFFFFFF, WHITE, 0x2EFFFFFF, WHITE, 0x38FFFFFF)
                HandleMode.EMPTY -> if (dark) {
                    TabColors(0xFF3A2A17.toInt(), 0x33F2B36B, 0xFFF2B36B.toInt(), 0x29F2B36B, 0xFFF2B36B.toInt(), 0x4DF2B36B)
                } else {
                    TabColors(0xFFFFF1E0.toInt(), 0x1F7A3D00, 0xFF7A3D00.toInt(), 0x1F9A4D00, 0xFF7A3D00.toInt(), 0x337A3D00)
                }
                // Lagoon of the app icon in both appearances: white text stays readable over any app.
                else -> TabColors(0xFF1B6E85.toInt(), 0x1AFFFFFF, WHITE, 0x29FFFFFF, WHITE, 0x38FFFFFF)
            }
        }
    }

    private companion object {
        const val AREA_WIDTH = 28
        const val AREA_HEIGHT = 76
        const val TAB_HEIGHT = 52
        const val SWIPE_DISTANCE = 24
        const val SLIDE_MS = 200L
        const val WHITE = 0xFFFFFFFF.toInt()

        /** Light gray at 80 %, like Samsung's handle: visible on light and dark apps alike. */
        const val BAR_FILL = 0xCCB8B8B8.toInt()

        /** Touches outside go to the app below; a touch outside is still reported, to fold the tab. */
        const val BASE_FLAGS = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }
}
