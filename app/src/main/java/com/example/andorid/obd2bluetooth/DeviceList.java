package com.example.andorid.obd2bluetooth;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class DeviceList extends AppCompatActivity {
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private List<BluetoothDevice> listDiscoveredDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_device_list);
        //Cambiamos el resulado a Cancelado en caso de que el usuario regrese
        setResult(Activity.RESULT_CANCELED);
        Button scanButton = (Button) findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doDiscover();
                view.setVisibility(View.GONE);
            }
        });
        //Inicializamos los array adapters, uno para los dispositivos que ya
        //están emparejados y otor para los dispositivos encontrados
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        // Encontramos ListView para los dispositivos Emparejados
        ListView pairedListView = (ListView) findViewById(R.id.ListPaired);
        // Encontramos ListView para los dispositivos Encontrados
        ListView discoveredListView = (ListView) findViewById(R.id.ListDiscovered);
        // Configuramos ListView para los dispositivos Emparejados
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        // Configuramos ListView para los dispositivos Encontrados
        discoveredListView.setAdapter(mNewDevicesArrayAdapter);
        discoveredListView.setOnItemClickListener(mDeviceClickListener);
        // Registro por actualizaciones cuando un dispositivo es descubierto o cuando
        // se deja buscar dispositivos
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        // Obtenemos el adaptador Bluetooth local del dispositivo
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Obtenemos los dispositivos con los que se ha emparejado anteriormente
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        // Si existen dispositivos emparejados, cada uno se agregar al ArrayAdapter
        if (pairedDevices.size() > 0 ){
            for (BluetoothDevice device : pairedDevices){
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else{
            String noDevices = "No se han emparejado dispositvos anteriormente".toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
        listDiscoveredDevices = new ArrayList<BluetoothDevice>();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Nos aseguramos que no se están buscando dispositivos nunca más
        if (mBtAdapter != null){
            mBtAdapter.cancelDiscovery();
        }
        // Borramos el registro de notificadores
        this.unregisterReceiver(mReceiver);
    }

    private void doDiscover(){
        // Indicamos en el Título que estamos buscando dispositivos
        setProgressBarIndeterminateVisibility(true);
        setTitle("Buscando dispositivos...");
        // Si ya estamos buscando, los detenemos
        if (mBtAdapter.isDiscovering()){
            mBtAdapter.cancelDiscovery();
        }
        // Solicitamos búsqueda de BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    private final AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            // Cancelamos la busqueda de nuevos dispositivos
            mBtAdapter.cancelDiscovery();
            // Obtenemos la dirección MAC del Dispositivo, la cual está conformada por los últimos 17 caracteres en la vista
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);
            // Creamos el resultado Intent e incluimos la direción MAC
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            // Cambiamos el resultado y finalizamos la actividad
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // Cuando se encuentra un dispositivo
            if (BluetoothDevice.ACTION_FOUND.equals(action)){
                // Obtenemos el BluetoothDeveice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Esta solo es una prueba para saber que es lo que obtenemos de aquí
                BluetoothDevice deviceName = intent.getParcelableExtra(BluetoothDevice.EXTRA_NAME);
                // Si el dispositivo se encuentra en los dispositvos emparejados lo saltamos
                if (device.getBondState() != BluetoothDevice.BOND_BONDED && !listDiscoveredDevices.contains(device)){
                    listDiscoveredDevices.add(device);
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                setProgressBarIndeterminateVisibility(false);
                setTitle("Selcciona un dispositivo para conectar");
                if (mNewDevicesArrayAdapter.getCount() == 0){
                    String noDevices = "No se encontraron dispositivos".toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
}