package com.example.edgeglow

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Notification
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.SweepGradient
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.View
import android.view.WindowManager

class GlowView(
    context: Context,
    private val glowColor: Int,
    private val thickPx: Float,
    durationMs: Long
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()
    private val rot = Matrix()
    private var sweep: SweepGradient? = null
    private var angle = 0f
    private val corner = 36f * resources.displayMetrics.density
    private val animator = ValueAnimator.ofFloat(0f, 1f)

    init {
        alpha = 0f
        animator.duration = durationMs
        animator.addUpdateListener {
            val f = it.animatedValue as Float
            angle = f * 720f
            alpha = when {
                f < 0.15f -> f / 0.15f
                f > 0.75f -> (1f - f) / 0.25f
                else -> 1f
            }
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val clear = glowColor and 0x00FFFFFF
        sweep = SweepGradient(
            w / 2f, h / 2f,
            intArrayOf(clear, glowColor, clear, clear),
            floatArrayOf(0f, 0.35f, 0.7f, 1f)
        )
    }

    override fun onDraw(canvas: Canvas) {
        val sh = sweep ?: return
        rot.setRotate(angle, width / 2f, height / 2f)
        sh.setLocalMatrix(rot)
        paint.shader = sh
        val inset = thickPx / 2f
        rect.set(inset, inset, width - inset, height - inset)
        paint.strokeWidth = thickPx * 3f
        paint.alpha = 60
        canvas.drawRoundRect(rect, corner, corner, paint)
        paint.strokeWidth = thickPx
        paint.alpha = 255
        canvas.drawRoundRect(rect, corner, corner, paint)
    }

    fun start(onEnd: () -> Unit) {
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: Animator) {
                onEnd()
            }
        })
        animator.start()
    }

    fun stop() {
        animator.removeAllListeners()
        animator.cancel()
    }
}

object Glow {
    private var current: GlowView? = null
    private val handler = Handler(Looper.getMainLooper())

    fun show(context: Context) {
        val app = context.applicationContext
        handler.post {
            if (!Settings.canDrawOverlays(app)) return@post
            val prefs = app.getSharedPreferences("glow", Context.MODE_PRIVATE)
            val color = prefs.getInt("color", Color.parseColor("#00E5FF"))
            val thick = prefs.getInt("thick", 6)
            val dur = prefs.getInt("dur", 3)
            val density = app.resources.displayMetrics.density

            val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            remove(wm)

            val v = GlowView(app, color, thick * density, dur * 1000L)
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            if (Build.VERSION.SDK_INT >= 28) {
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            wm.addView(v, lp)
            current = v
            v.start {
                if (current === v) remove(wm)
            }
        }
    }

    private fun remove(wm: WindowManager) {
        val old = current
        current = null
        if (old != null) {
            old.stop()
            try {
                wm.removeView(old)
            } catch (e: Exception) {
            }
        }
    }
}

class GlowService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val flags = sbn.notification.flags
        if ((flags and Notification.FLAG_ONGOING_EVENT) != 0) return
        if ((flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        Glow.show(applicationContext)
    }
}
