package com.example.andorid.obd2bluetooth;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_CONNECT_DEVICE = 1;

    BluetoothAdapter mBluetoothAdapter;
    private boolean DeviceSelected;
    private ArrayAdapter<String> mMessageReceived;
    private String mConnectedDeviceName = null;
    private BluetoothService mBTService;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Inicializamos el adaptador  de Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Se verifica si el dispositivo soporta conexones por Bluetooth
        if(BluetoothAdapter.getDefaultAdapter() == null){
            Toast.makeText(this, "Tu dispositivo no soporta conexiones bluetooth",Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        DeviceSelected = false;
        mMessageReceived = new ArrayAdapter<String>(this,R.layout.device_name);
        ListView ListMessageReceived = (ListView) findViewById(R.id.messageReceived);
        ListMessageReceived.setAdapter(mMessageReceived);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBTService = new BluetoothService(this, mHandler);
        // Verficamos que se tengan los permisos necesarios en el dispositivo
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            // Si se tienen los permisos
            Toast mensaje = Toast.makeText(this,"Permiso otorgado exitosamente con anterioridad",Toast.LENGTH_LONG);
            mensaje.setGravity(Gravity.BOTTOM,0,0);
            mensaje.show();
            // Se verifica si el bluetooth está activo
            if (!mBluetoothAdapter.isEnabled()) {
                // Si no está activo se crea un Intent Para solicitar su activación
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // Se registra el Intent esperando una respuesta
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }else if(!DeviceSelected){ // Este if es para verificar si el dispositivo está conectado
                // Se inicia activity DeviceList, para seleccionar un dispositivo para conectarse
                Intent intent = new Intent(this, DeviceList.class);
                // Se registra el Intent esperando una respuesta
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
            }else{

                sendMessage("hola, estoy probando la conexión");
            }
        }else {
            // Si no se cuenta con los permisos es necesario que se solicite al usuario los permisos necesarios
            Toast.makeText(this,"Permiso debe ser otorgado",Toast.LENGTH_LONG).show();
            // Se crea un request para solicitar los permisos
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },200);
        }
    }

    private void sendMessage(String message) {

        // Check that we're actually connected before trying anything
        if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "You are not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBTService.write(send);
            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Toast mensaje;
        switch (requestCode) {
            // Se verifica la respuesta obtenida, para evitar errores
            case 200:
                // Se verifica si los permisos fueron otorgados
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED || grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"Permiso otorgado exitosamente",Toast.LENGTH_LONG).show();
                    // Se verifica si el bluetooth está activo
                    if (!mBluetoothAdapter.isEnabled()) {
                        // Si no está activo se crea un Intent Para solicitar su activación
                        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                    }else if(!DeviceSelected){
                        // Se inicia activity DeviceList, para seleccionar un dispositivo para conectarse
                        Intent intent = new Intent(this, DeviceList.class);
                        // Se registra el Intent esperando una respuesta
                        startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
                    }
                    //doDiscovery();
                } else {
                    // Si no se otorga el permiso, mostramos un mensaje y cerramos la aplicación
                    Toast.makeText(this,"Permiso no se otorgó",Toast.LENGTH_LONG).show();
                    finish();
                    //doDiscovery();
                }
                break;
            default:
                // Si existe un error se indica
                Toast.makeText(this,"Ocurrió un error",Toast.LENGTH_LONG).show();
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // Cuando DeviceListActivity retorna un dispositivo para conectar
                if (resultCode == Activity.RESULT_OK) {
                    // Obtenemos la dirección MAC del dispositivos
                    String address = data.getExtras().getString(DeviceList.EXTRA_DEVICE_ADDRESS);
                    DeviceSelected = true;
                    // Obtenemos el objeto BLuetoothDevice
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Intentamos conectarnos con el dispositivo
                    mBTService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // Cuando la solicitud de activar el Bluetooth retorna
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth está activo
                    Toast.makeText(this, "El bluetooth se activó correctamente.", Toast.LENGTH_SHORT).show();
                    // Se crea un Intent para abrir Activity DeviceList que nos permite encontrar dispositivos cercanos
                    Intent intent = new Intent(this, DeviceList.class);
                    startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
                } else {
                    // El usuario no permitió que se activara el Bluetooth
                    Toast.makeText(this, "El bluetooth debe estar activo para poder utilizar la aplicación.", Toast.LENGTH_SHORT).show();
                    // Cerramos la aplicación
                    finish();
                }
                break;
            default:
                Toast.makeText(this,"Ocurrió un error",Toast.LENGTH_LONG).show();
                break;
        }
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mAdapter.notifyDataSetChanged();
                    //messageList.add(new androidRecyclerView.Message(counter++, writeMessage, "Me"));
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mAdapter.notifyDataSetChanged();
                    //messageList.add(new androidRecyclerView.Message(counter++, readMessage, mConnectedDeviceName));
                    mMessageReceived.add(readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}