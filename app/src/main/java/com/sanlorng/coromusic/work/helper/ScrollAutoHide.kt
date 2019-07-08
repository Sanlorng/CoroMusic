package com.sanlorng.coromusic.work.helper

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

class ScrollAutoHide(context: Context, attr: AttributeSet) : CoordinatorLayout.Behavior<View>(context, attr) {
    private val interpolator = FastOutSlowInInterpolator()
    private var isAnimate = false
    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, directTargetChild: View, target: View, axes: Int, type: Int): Boolean {
        return  axes == ViewCompat.SCROLL_AXIS_VERTICAL|| super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type)
    }

    override fun onNestedScroll(coordinatorLayout: CoordinatorLayout, child: View, target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        if (type == ViewCompat.TYPE_TOUCH) {
            if (dy > 0 && !isAnimate)
                animateOut(child)
            else if (dy <0 &&! isAnimate)
                animateIn(child)
        }
    }
    private fun animateOut(target: View) {
        val layoutParams = target.layoutParams as CoordinatorLayout.LayoutParams
        val bottomMargin = layoutParams.bottomMargin
        target.animate().translationY((target.height + bottomMargin).toFloat()).setInterpolator(interpolator).setDuration(500).apply {
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationCancel(animation: Animator?) {
                    animateIn(target)
                }

                override fun onAnimationEnd(animation: Animator?) {
                    isAnimate = false
                }

                override fun onAnimationRepeat(animation: Animator?) {

                }

                override fun onAnimationStart(animation: Animator?) {
                    isAnimate = true
                }
            })
        }.start()

    }

    private fun animateIn(target: View) {
        target.animate().translationY(0f).setInterpolator(interpolator).setDuration(500).apply {
            setListener(object : Animator.AnimatorListener {
                override fun onAnimationCancel(animation: Animator?) {
                    animateOut(target)
                }

                override fun onAnimationEnd(animation: Animator?) {
                    isAnimate = false
                }

                override fun onAnimationRepeat(animation: Animator?) {

                }

                override fun onAnimationStart(animation: Animator?) {
                    isAnimate = true
                }
            })
        }.start()
    }
}