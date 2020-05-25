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
import androidx.recyclerview.widget.RecyclerView
import com.rugroden.R
import com.rugroden.picar.utils.Constants
import com.rugroden.picar.utils.Tinter
import com.rugroden.picar.utils.findDrawable
import com.rugroden.picar.utils.findView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_homescreen.*
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
  private var connectionObservable: Disposable? = null
  private val btListAdapter:BtListAdapter = BtListAdapter(this)
  private var receiverRegistered:Boolean = false
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
      it.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimension(R.dimen.recycler_card_space).roundToInt()))
    }

    scanButton = view.findView(R.id.scan_button){
      it.setOnClickListener { startScan() }
    }

    return view
  }

  override fun onPause() {
    stopScan()
    btListAdapter.submitList(emptyList())
    connectionObservable?.dispose()
    connectionObservable = null
    super.onPause()
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
    stopScan()
    if(::scanButton.isInitialized){
      scanButton.text = getString(R.string.connecting)
      scanButton.setOnClickListener(null)
      scanButton.isEnabled = false
    }
    connectionObservable?.dispose()
    connectionObservable = null
    connectionObservable = connectToDevice(device)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
        {
          scanButton.apply {
            text = getString(R.string.scan)
            isEnabled = true
            setOnClickListener { startScan() }
          }
        },
        {error ->
          scanButton.apply {
            text = getString(R.string.scan)
            isEnabled = true
            setOnClickListener { startScan() }
          }
          Toast.makeText(requireContext(), "Unable to connect to '${device.name ?: "device"}'", Toast.LENGTH_SHORT).show()
          Timber.e(error, "Unable to connect to device.")
        }
      )
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
          scanButton.text = getString(R.string.scanning)
        }
        if(!receiverRegistered) {
          val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
          }
          activity?.registerReceiver(broadcastReceiver, filter)
          receiverRegistered = true
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
      scanButton.text = getString(R.string.scan)
    }
    if(receiverRegistered) {
      try {
        activity?.unregisterReceiver(broadcastReceiver)
        receiverRegistered = false
      } catch (e: Exception) {
        Timber.e(e, "Unable to unregister broadcast receiver")
      }
    }

    btAdapter?.cancelDiscovery()
  }

  private fun connectToDevice(device: BluetoothDevice):Observable<Any> {
    return Observable.create {
      val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(Constants.BT_UUID))
      (activity as? MainActivity)?.let {
        it.btDevice = device
        it.btSocket = socket
        socket.connect()
        it.openController()
      }
    }
  }
}