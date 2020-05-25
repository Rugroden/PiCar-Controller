package com.rugroden.picar.utils

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable

object Tinter {

  fun tintDrawable(drawable: Drawable, tintColor:Int){
    val colorFilter = PorterDuffColorFilter(tintColor,PorterDuff.Mode.SRC_IN)
    drawable.colorFilter = colorFilter
  }

}