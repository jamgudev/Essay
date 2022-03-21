package com.jamgu.home.viewevent.simplesmtart.interpolator

import android.view.animation.Interpolator
import kotlin.math.exp

// interpolator type
var INTERPOLATOR_VISCOUS_FLUID = 0
var INTERPOLATOR_DECELERATE = 1

// must be set to 1.0 (used in viscousFluid())
private var VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluid(1.0f)
// account for very small floating-point error
private var VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluid(1.0f)
private const val VISCOUS_FLUID_SCALE = 8.0f

private fun viscousFluid(x: Float): Float {
    var lx = x
    lx *= VISCOUS_FLUID_SCALE
    if (lx < 1.0f) {
        lx -= 1.0f - exp(-lx.toDouble()).toFloat()
    } else {
        val start = 0.36787944117f // 1/e == exp(-1)
        lx = 1.0f - exp((1.0f - lx).toDouble()).toFloat()
        lx = start + lx * (1.0f - start)
    }
    return lx
}

/**
 * Created by jamgu on 2022/03/21
 */
class ReboundInterpolator(private val mInterpolatorType: Int): Interpolator {
    override fun getInterpolation(input: Float): Float {
        if (mInterpolatorType == INTERPOLATOR_DECELERATE) {
            return 1.0f - (1.0f - input) * (1.0f - input)
        }
        val interpolated: Float = VISCOUS_FLUID_NORMALIZE * viscousFluid(input)
        return if (interpolated > 0) {
            interpolated + VISCOUS_FLUID_OFFSET
        } else interpolated
    }
}