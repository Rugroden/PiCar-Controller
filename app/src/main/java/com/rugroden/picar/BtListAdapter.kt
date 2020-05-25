package com.rugroden.picar

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.rugroden.R
import com.rugroden.picar.btitem.BtItemViewHolder

class BtListAdapter(private val itemListener:BtItemClickListener) :
  ListAdapter<BluetoothDevice, BtItemViewHolder>(DiffCallback())
{

  override fun getItemViewType(position: Int) = R.layout.bt_item_viewholder

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BtItemViewHolder {
    val itemView = LayoutInflater.from(parent.context)
      .inflate(viewType,parent,false)
    return BtItemViewHolder(itemView)
  }

  override fun onBindViewHolder(holder: BtItemViewHolder, position: Int) {
    holder.bind(getItem(position),itemListener)
  }

  private class DiffCallback:DiffUtil.ItemCallback<BluetoothDevice>(){
    override fun areItemsTheSame(old: BluetoothDevice, new: BluetoothDevice) = old.address == new.address

    override fun areContentsTheSame(old: BluetoothDevice, new: BluetoothDevice) = old == new
  }

  interface BtItemClickListener{
    fun onclick(device:BluetoothDevice, view: View)
  }

}