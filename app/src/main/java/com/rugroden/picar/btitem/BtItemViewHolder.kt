package com.rugroden.picar.btitem

import android.bluetooth.BluetoothDevice
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rugroden.R
import com.rugroden.picar.BtListAdapter
import com.rugroden.picar.utils.findView
import timber.log.Timber

class BtItemViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){

  private val titleView: TextView = itemView.findView(R.id.item_title)
  private val idView: TextView = itemView.findView(R.id.item_id)
  private var device:BluetoothDevice? = null

  fun bind(data: BluetoothDevice, clickListener: BtListAdapter.BtItemClickListener){
    device = data
    titleView.text = data.name
    idView.text = data.address
    itemView.setOnClickListener { view ->
      device?.let {
        Timber.e("item clicked")
        clickListener.onclick(it,view)
      }
    }
  }
}