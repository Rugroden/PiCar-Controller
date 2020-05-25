package com.rugroden.picar

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.DEVICE_TYPE_UNKNOWN
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rugroden.R
import com.rugroden.picar.utils.Constants
import com.rugroden.picar.utils.Tinter
import com.rugroden.picar.utils.findDrawable
import com.rugroden.picar.utils.findView
import timber.log.Timber
import java.lang.Exception
import java.util.*
import kotlin.math.roundToInt


class HomescreenFragment: Fragment(), BtListAdapter.BtItemClickListener{

  companion object {
    const val BT_REQUEST_CODE: Int = 25565
  }

  //VARS
  //Views
  private lateinit var scanButton:Button
  private lateinit var recyclerView:RecyclerView

  //Held stuff.
  private val btListAdapter:BtListAdapter = BtListAdapter(this)
  private var reveiverRegistered:Boolean = false
  private val broadcastReceiver:BroadcastReceiver = object : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent) {
      when(intent.action){
        BluetoothDevice.ACTION_FOUND -> {
          val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
          if(device.type != DEVICE_TYPE_UNKNOWN && device.name != null) {
            val list = btListAdapter.currentList.toMutableSet()
            list.add(device)
            btListAdapter.submitList(list.toList())
            btListAdapter.notifyDataSetChanged()
          }
        }
        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
          stopScan()
        }
      }
    }
  }


  ///region --------------- START Lifecycle Stuff ---------------

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.fragment_homescreen,container,false)

    recyclerView = view.findView(R.id.recycler_view){
      it.adapter = btListAdapter
//      it.addItemDecoration(DividerItemDecoration(it.context, DividerItemDecoration.VERTICAL))
      it.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimension(R.dimen.recycler_card_space).roundToInt()))
    }

    scanButton = view.findView(R.id.scan_button){
      it.setOnClickListener { startScan() }
    }

    return view
  }

  override fun onResume() {
    btListAdapter.submitList(emptyList())
    super.onResume()
  }

  override fun onPause() {
    stopScan()
    super.onPause()
  }

  override fun onDestroy() {
    stopScan()
    super.onDestroy()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if(requestCode == BT_REQUEST_CODE){
      when(resultCode){
        RESULT_OK -> startScan()
        RESULT_CANCELED -> Toast.makeText(requireContext(),R.string.need_bt_perm,Toast.LENGTH_SHORT).show()
        else -> Timber.e("Unexpected result from BT request: $resultCode")
      }
    }
    else {
      super.onActivityResult(requestCode, resultCode, data)
    }
  }
  //endregion ---------- END Lifecycle Stuff ----------


  //region --------------- START BtItemClickListener stuff ---------------

  override fun onclick(device: BluetoothDevice, view: View) {
    connectToDevice(device)
  }
  //endregion ---------- END BtItemClickListener stuff ----------

  private fun startScan(){
    val btAdapter = BluetoothAdapter.getDefaultAdapter()
    when {
      btAdapter == null -> {
        val drawable = requireContext().findDrawable(android.R.drawable.ic_dialog_alert)
        Tinter.tintDrawable(drawable,ResourcesCompat.getColor(resources, R.color.colorPrimary,null))

        AlertDialog.Builder(requireContext())
          .setTitle("Incompatible")
          .setMessage("This device does not support Bluetooth")
          .setIcon(drawable)
          .show()
      }
      btAdapter.isDiscovering -> {
        stopScan()
      }
      btAdapter.isEnabled -> {
        btListAdapter.submitList(emptyList())
        if(::scanButton.isInitialized){
          scanButton.text = resources.getString(R.string.scanning)
        }
        if(!reveiverRegistered) {
          val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
          }
          activity?.registerReceiver(broadcastReceiver, filter)
          reveiverRegistered = true
          btAdapter.startDiscovery()
        }
      }
      else -> {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(intent, BT_REQUEST_CODE)
      }
    }
  }

  private fun stopScan(){
    val btAdapter = BluetoothAdapter.getDefaultAdapter()
    if(::scanButton.isInitialized){
      scanButton.text = resources.getString(R.string.scan)
    }
    if(reveiverRegistered) {
      try {
        activity?.unregisterReceiver(broadcastReceiver)
        reveiverRegistered = false
      } catch (e: Exception) {
        Timber.e(e, "Unable to unregister broadcast receiver")
      }
    }

    btAdapter?.cancelDiscovery()
  }

  private fun connectToDevice(device: BluetoothDevice){
    stopScan()
    val socket:BluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(Constants.BT_UUID))
    (activity as? MainActivity)?.let {
      it.btDevice = device
      it.btSocket = socket
      try {
        socket.connect()
        it.openController()
      } catch (e: Exception) {
        Toast.makeText(requireContext(), "Unable to connect to device", Toast.LENGTH_SHORT).show()
        Timber.e(e, "Unable to connect to device.")
      }
    }

  }
}