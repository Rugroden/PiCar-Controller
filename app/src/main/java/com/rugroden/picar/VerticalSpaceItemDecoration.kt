package com.rugroden.picar

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VerticalSpaceItemDecoration(private val vSpace:Int):RecyclerView.ItemDecoration(){

  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    super.getItemOffsets(outRect, view, parent, state)
    outRect.top = vSpace
    outRect.bottom = vSpace
  }
}