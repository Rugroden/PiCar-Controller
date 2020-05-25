package com.rugroden.picar.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat


inline fun <reified T:View> View.findView(resId:Int):T {
  return findViewById<T>(resId) ?:
  error("Unable to find view: $resId -> ${context.resources.getResourceName(resId)}")
}

inline fun <reified T:View> View.findView(resId:Int, crossinline body:(view:T) -> Unit ):T {
  val view:T = findView(resId)
  view.apply(body)
  return view
}

fun Context.findDrawable(resId:Int):Drawable{
  return ContextCompat.getDrawable(this,resId)
    ?: error("Unable to find drawable: $resId -> ${resources.getResourceName(resId)}")
}