package app.rohy

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.RoundedCorner
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * The edge handle of phase 10: a thin bar on a side of the screen, drawn over other apps while the phone is
 * connected. Swiping it toward the middle opens a tab "Envoyer à l'ordinateur" that slides out of the edge.
 * The tab's window takes the focus, which is what lets Rohy read the clipboard (Android 10+); the transparent
 * activity of the tile is the fallback. It is turned off from the home screen's switch only.
 *
 * A long press then a drag moves it along the left or right edge; near a corner it bends with the screen's
 * rounded corner ([HandleTrack]). While moving, a second window dims the app and shows where it can go.
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
    private var guide: View? = null
    private var mode = HandleMode.REST
    private var dragging = false
    private var track = track(storage.handleSide)
    private var center = 0f

    /** Shows or removes the handle; called whenever the connection or the settings change. */
    fun update(visible: Boolean) {
        if (visible) attach() else detach()
    }

    /** After a rotation or a light/dark switch: same relative place, current colors. */
    fun reposition() {
        if (root == null) return
        load()
        render()
        place()
    }

    private fun attach() {
        if (root != null) return
        val view = Root(context)
        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            BASE_FLAGS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { title = "Rohy" }
        root = view
        params = layout
        mode = HandleMode.REST
        load()
        render()
        frame(layout)
        if (runCatching { windows.addView(view, layout) }.isFailure) {
            root = null
            params = null
        }
    }

    private fun detach() {
        main.removeCallbacks(fold)
        removeGuide()
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

    // Where the handle is.

    /** The usable window space: the screen without the status bar, the navigation area and the cutout. */
    private data class Space(val width: Int, val height: Int, val top: Int, val bottom: Int, val left: Int, val right: Int, val radius: Float)

    private fun space(): Space {
        if (Build.VERSION.SDK_INT >= 30) {
            val metrics = windows.currentWindowMetrics
            val bars = WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(bars)
            val radius = if (Build.VERSION.SDK_INT >= 31) {
                metrics.windowInsets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius?.toFloat()
            } else null
            return Space(
                metrics.bounds.width() - insets.left - insets.right, metrics.bounds.height() - insets.top - insets.bottom,
                insets.top, insets.bottom, insets.left, insets.right, maxOf(radius ?: 0f, dpF(MIN_CORNER)),
            )
        }
        // Android 10: no window metrics; the status bar is about 24 dp.
        val display = context.resources.displayMetrics
        val status = dp(24)
        return Space(display.widthPixels, display.heightPixels - status, status, 0, 0, 0, dpF(MIN_CORNER))
    }

    private fun track(side: HandleSide): HandleTrack {
        val space = space()
        // A side with a cutout or a navigation bar has no rounded corner to follow.
        val sideInset = if (side == HandleSide.RIGHT) space.right else space.left
        return HandleTrack(
            side = side,
            edge = if (side == HandleSide.RIGHT) space.width.toFloat() else 0f,
            top = 0f,
            bottom = space.height.toFloat(),
            screenTop = -space.top.toFloat(),
            screenBottom = (space.height + space.bottom).toFloat(),
            cornerRadius = if (sideInset == 0) space.radius else 0f,
            inset = dpF(BAR_INSET),
        )
    }

    private fun load() {
        track = track(storage.handleSide)
        center = track.clampCenter(storage.handlePosition * track.length, dpF(BAR_LENGTH))
    }

    private fun save() {
        storage.handleSide = track.side
        storage.handlePosition = if (track.length > 0f) center / track.length else EdgeHandleRules.DEFAULT_POSITION
    }

    /** Only the open tab may take the focus: the bar must never steal it from the app below. */
    private fun place() {
        val view = root ?: return
        val layout = params ?: return
        frame(layout)
        runCatching { windows.updateViewLayout(view, layout) }
    }

    /**
     * The bar's window hugs the bar (straight or bent) with a margin for the finger. The tab's window is as high
     * as that margin around the bar's middle, anchored to the side, so opening does not move it up or down.
     */
    private fun frame(layout: WindowManager.LayoutParams) {
        val space = space()
        layout.flags = if (mode == HandleMode.OPEN) BASE_FLAGS else BASE_FLAGS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (mode == HandleMode.REST) {
            val box = barBox(space)
            layout.gravity = Gravity.TOP or Gravity.LEFT
            layout.x = box.left
            layout.y = box.top
            layout.width = box.width()
            layout.height = box.height()
        } else {
            val middle = track.pointAt(center).y.toInt()
            layout.gravity = Gravity.TOP or if (track.side == HandleSide.RIGHT) Gravity.RIGHT else Gravity.LEFT
            layout.x = 0
            layout.y = (middle - dp(AREA_HEIGHT) / 2).coerceIn(0, (space.height - dp(AREA_HEIGHT)).coerceAtLeast(0))
            layout.width = WindowManager.LayoutParams.WRAP_CONTENT
            layout.height = dp(AREA_HEIGHT)
        }
    }

    private fun barBox(space: Space): Rect {
        val bounds = HandleTrack.bounds(
            track.segment(center, dpF(BAR_LENGTH)), dpF(TOUCH_MARGIN), space.width.toFloat(), space.height.toFloat(),
        )
        val box = Rect(bounds[0].toInt(), bounds[1].toInt(), bounds[2].toInt(), bounds[3].toInt())
        // At least a finger's width from the edge, like Samsung's handle.
        if (track.side == HandleSide.RIGHT) box.left = minOf(box.left, space.width - dp(AREA_WIDTH)) else box.right = maxOf(box.right, dp(AREA_WIDTH))
        return box
    }

    /** Like a drawer pulled from the edge: the tab enters from its side, before its first frame is drawn. */
    private fun slideIn(view: View) {
        val from = if (track.side == HandleSide.RIGHT) 1f else -1f
        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)
                view.translationX = from * view.width
                view.animate().translationX(0f).setDuration(SLIDE_MS).setInterpolator(DecelerateInterpolator()).start()
                return true
            }
        })
    }

    private fun render() {
        val view = root ?: return
        view.removeAllViews()
        if (mode == HandleMode.REST) {
            view.addView(bar(), FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        } else {
            val side = if (track.side == HandleSide.RIGHT) Gravity.RIGHT else Gravity.LEFT
            view.addView(tab(), FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, side or Gravity.CENTER_VERTICAL,
            ))
        }
    }

    // The bar.

    /**
     * Same look and gesture as the Samsung Edge panel handle: thin light gray bar, opened by a swipe toward
     * the middle of the screen, never by a tap (typing near the edge must not open it). TalkBack still opens
     * it with its click action. While it is being moved it is thicker and in the app's lagoon color.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun bar(): View = BarView(context).apply {
        contentDescription = "Rohy : glisser vers le centre pour envoyer le presse-papiers à l'ordinateur"
        isClickable = true
        setOnClickListener { show(HandleMode.OPEN) }
        setOnTouchListener(SwipeOrMove(this))
        // The swipe starts on the edge, where Android otherwise sees its Back gesture: keep this small area for us.
        addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            if (Build.VERSION.SDK_INT >= 29) view.systemGestureExclusionRects = listOf(Rect(0, 0, view.width, view.height))
        }
    }

    /** Draws the bar's part of the track, in the window's coordinates. */
    private inner class BarView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        override fun onDraw(canvas: Canvas) {
            val layout = params ?: return
            val points = track.segment(center, dpF(BAR_LENGTH))
            val path = Path()
            points.forEachIndexed { i, p ->
                val x = p.x - layout.x
                val y = p.y - layout.y
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            // While moving, the guide above draws the bar, thicker and in lagoon.
            if (guide != null) return
            paint.strokeWidth = dpF(BAR_WIDTH)
            paint.color = BAR_FILL
            canvas.drawPath(path, paint)
        }
    }

    /**
     * A swipe toward the middle of the screen opens the tab; a tap does nothing; a long press then a drag moves
     * the handle along the edges, to the other side too.
     */
    private inner class SwipeOrMove(private val bar: View) : View.OnTouchListener {
        private val slop = ViewConfiguration.get(context).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var moved = false
        private val startDrag = Runnable {
            dragging = true
            bar.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showGuide()
            bar.invalidate()
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    dragging = false
                    moved = false
                    main.postDelayed(startDrag, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (dragging) {
                        moveTo(event.rawX, event.rawY, bar)
                    } else if (abs(dy) > slop || abs(dx) > slop) {
                        moved = true
                        main.removeCallbacks(startDrag)
                        val normal = track.inwardNormal(center)
                        if (HandleTrack.opensTab(dx, dy, normal, dpF(SWIPE_DISTANCE))) {
                            show(HandleMode.OPEN)
                            return true
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    main.removeCallbacks(startDrag)
                    if (dragging) {
                        dragging = false
                        save()
                        removeGuide()
                        bar.invalidate()
                    }
                }
            }
            return true
        }
    }

    /** Follows the finger along the nearest side; a tick when the bar enters or leaves a corner. */
    private fun moveTo(rawX: Float, rawY: Float, bar: View) {
        val space = space()
        val x = rawX - space.left
        val y = rawY - space.top
        val side = HandleTrack.sideFor(x, space.width.toFloat())
        if (side != track.side) track = track(side)
        val wasInCorner = track.inCorner(center)
        center = track.clampCenter(track.project(x, y), dpF(BAR_LENGTH))
        if (track.inCorner(center) != wasInCorner) bar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        place()
        bar.invalidate()
        guide?.invalidate()
    }

    // The guide shown while moving.

    private fun showGuide() {
        removeGuide()
        val view = GuideView(context)
        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { title = "Rohy" }
        // Added above the handle without touching the handle's window, which would cancel the finger's gesture;
        // the guide does not take touches and draws the moving bar itself.
        if (runCatching { windows.addView(view, layout) }.isSuccess) guide = view
    }

    private fun removeGuide() {
        guide?.let { runCatching { windows.removeViewImmediate(it) } }
        guide = null
    }

    /** Dims the app and draws both tracks in dots, with a short hint in the middle (board, state 3). */
    private inner class GuideView(context: Context) : View(context) {
        private val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = dpF(2)
            color = GUIDE_DOTS
            pathEffect = DashPathEffect(floatArrayOf(dpF(1), dpF(7)), 0f)
        }
        private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = dpF(BAR_DRAG_WIDTH)
            color = DRAG_FILL
        }
        private val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GUIDE_PILL }
        private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GUIDE_TEXT
            textAlign = Paint.Align.CENTER
            textSize = spF(14)
            typeface = context.resources.getFont(R.font.inter_semibold)
        }
        private val hint = Paint(title).apply {
            color = GUIDE_HINT
            textSize = spF(12)
            typeface = context.resources.getFont(R.font.inter_regular)
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawColor(GUIDE_DIM)
            for (side in HandleSide.entries) {
                val path = Path()
                val t = track(side)
                var u = 0f
                while (u <= t.length) {
                    val p = t.pointAt(u)
                    if (u == 0f) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                    u += dpF(4)
                }
                canvas.drawPath(path, dots)
            }
            val moving = Path()
            track.segment(center, dpF(BAR_LENGTH)).forEachIndexed { i, p -> if (i == 0) moving.moveTo(p.x, p.y) else moving.lineTo(p.x, p.y) }
            canvas.drawPath(moving, bar)
            val cx = width / 2f
            val cy = height / 2f
            canvas.drawRoundRect(RectF(cx - dpF(120), cy - dpF(30), cx + dpF(120), cy + dpF(30)), dpF(30), dpF(30), pill)
            canvas.drawText("Glisse le long du bord", cx, cy - dpF(2), title)
            canvas.drawText("Lâche pour la poser", cx, cy + dpF(16), hint)
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
                // Rounded on the side facing the middle of the screen, square against the edge.
                val r = dp(TAB_HEIGHT / 2).toFloat()
                cornerRadii = if (track.side == HandleSide.RIGHT) floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
                else floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
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
            if (track.side == HandleSide.RIGHT) setPadding(dp(8), 0, dp(14), 0) else setPadding(dp(14), 0, dp(14), 0)
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

    private fun dpF(value: Number): Float = value.toFloat() * context.resources.displayMetrics.density

    private fun spF(value: Int): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value.toFloat(), context.resources.displayMetrics)

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

        /** The bar: 3 × 50 dp, its middle 5.5 dp from the edge, like Samsung's handle. */
        const val BAR_LENGTH = 50
        const val BAR_WIDTH = 3
        const val BAR_DRAG_WIDTH = 6
        const val BAR_INSET = 5.5f

        /** Room for the finger around the bar. */
        const val TOUCH_MARGIN = 13

        /**
         * Smallest curve in the corners: the screen's own radius when it is larger, else this, so the bar visibly
         * rounds in a corner even on nearly square screens (the S24 Ultra reports 6 px).
         */
        const val MIN_CORNER = 40

        /** Light gray at 80 %, like Samsung's handle: visible on light and dark apps alike. */
        const val BAR_FILL = 0xCCB8B8B8.toInt()

        /** While moving: the app's lagoon (dark-mode primary), the dots, the dim and the hint. */
        const val DRAG_FILL = 0xFF7FD0E6.toInt()
        const val GUIDE_DOTS = 0x8C7FD0E6.toInt()
        const val GUIDE_DIM = 0x59000000
        const val GUIDE_PILL = 0xFF262A2C.toInt()
        const val GUIDE_TEXT = 0xFFE1E3E3.toInt()
        const val GUIDE_HINT = 0xFFBFC8CA.toInt()

        /** Touches outside go to the app below; a touch outside is still reported, to fold the tab. */
        const val BASE_FLAGS = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }
}
