package th.co.gosoft.btprinter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import app.loup.streams_channel.StreamsChannel;
import th.co.gosoft.command.sdk.Command;
import th.co.gosoft.command.sdk.PrintPicture;
import th.co.gosoft.command.sdk.PrinterCommand;

/**
 * BtprinterPlugin
 */
public class BtprinterPlugin extends Activity implements FlutterPlugin, MethodCallHandler {
    /******************************************************************************************************/
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_CONNECTION_LOST = 6;
    public static final int MESSAGE_UNABLE_CONNECT = 7;
    /*******************************************************************************************************/
    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    private static final String THAI = "CP874";
    private String DEVICE_ADDRESS = "66:22:3E:0E:24:03";

    // Name of the connected device
    private String mConnectedDeviceName = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mService = null;
    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 2;
    // Member fields
    private BluetoothAdapter mBtAdapter;
    private List<String> mPairedDevices = new ArrayList<>();
    private List<String> mNewDevices = new ArrayList<>();
    private MethodChannel channel;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        final MethodChannel channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "btprinter");
        channel.setMethodCallHandler(new BtprinterPlugin());

        final StreamsChannel streamChannel = new StreamsChannel(flutterPluginBinding.getBinaryMessenger(), "blue_caps_chinese_printer_stream");
        streamChannel.setStreamHandlerFactory(new StreamsChannel.StreamHandlerFactory() {
            @Override
            public EventChannel.StreamHandler create(Object arguments) {
                return new StreamHandler();
            }
        });

        final StreamsChannel connectChannel = new StreamsChannel(flutterPluginBinding.getBinaryMessenger(), "btprinter_stream");
        connectChannel.setStreamHandlerFactory(new StreamsChannel.StreamHandlerFactory() {
            @Override
            public EventChannel.StreamHandler create(Object arguments) {
                return new ConnectionHandler(DEVICE_ADDRESS);
            }
        });
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "btprinter");
        channel.setMethodCallHandler(new BtprinterPlugin());
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("connectDevices")) {
            String address = call.argument("address").toString();
            setDEVICE_ADDRESS(address);
//            initBluetooth();
//            String msg = onConnect(address);
            result.success("Success");
        } else if (call.method.equals("printString")) {
            String msg = call.argument("msg").toString();
            printString(msg);
            result.success("Success");
        } else if (call.method.equals("printBarcode")) {
            String barcodeString = call.argument("barcodeString").toString();
            printEnBarcode(barcodeString);
            result.success("Success");
        } else if (call.method.equals("printQrCode")) {
            String qrString = call.argument("qrString").toString();
            printQrCode(qrString);
            result.success("Success");
        }else if (call.method.equals("discoveryNewDevices")) {
            initBluetooth();
            startDicoveryNewDevices();
            result.success("start discovery");
        } else if (call.method.equals("discoveryPariedDevices")) {
            List<String> pariedDevice = discoveryPariedDevice();
            result.success(pariedDevice);
        } else {
            result.notImplemented();
        }
    }

    public void setDEVICE_ADDRESS(String DEVICE_ADDRESS) {
        this.DEVICE_ADDRESS = DEVICE_ADDRESS;
    }

    //============================== Streaming Channel =============================================

    public static class StreamHandler implements EventChannel.StreamHandler {

        private final Handler handler = new Handler();
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (count > 10) {
                    eventSink.endOfStream();
                } else {
                    eventSink.success(count);
                }
                count++;
                handler.postDelayed(this, 1000);
            }
        };

        private EventChannel.EventSink eventSink;
        private int count = 1;

        @Override
        public void onListen(Object o, final EventChannel.EventSink eventSink) {
            this.eventSink = eventSink;
            // branch your code here.
            runnable.run();
        }

        @Override
        public void onCancel(Object o) {
            handler.removeCallbacks(runnable);
        }
    }

    public static class ConnectionHandler extends Activity implements EventChannel.StreamHandler{

        ConnectionHandler(String deviceAddress){
            this.deviceAddress = deviceAddress;
        }

        private String deviceAddress;
        // Name of the connected device
        private BluetoothAdapter mBluetoothAdapter;
        private BluetoothService mService;
        // Intent request codes
        private static final int REQUEST_ENABLE_BT = 2;

        public void initBluetooth() {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            // If the adapter is null, then Bluetooth is not supported
            if (mBluetoothAdapter == null) {
//            return "Bluetooth is not available"
            } else {
                startBluetooth();
            }
        }

        private void startBluetooth() {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                if (mService == null)
                    mService = new BluetoothService(this, mHandler);
            }
        }

        private String onConnect() {
            if (BluetoothAdapter.checkBluetoothAddress(this.deviceAddress)) {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(this.deviceAddress);
                mService.connect(device);

                return new String("Success");
            } else {
                return new String("can't connect Devices");
            }
        }

        @SuppressLint("HandlerLeak")
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:
                        switch (msg.arg1) {
                            case BluetoothService.STATE_CONNECTED:
                                eventSink.success(connectState.connected);
                                break;
                            case BluetoothService.STATE_CONNECTING:
                                eventSink.success(connectState.connecting);
                                break;
                            case BluetoothService.STATE_LISTEN:
                                eventSink.success(connectState.listening);
                                break;
                            case BluetoothService.STATE_NONE:
                                break;
                        }
                        break;
                    case MESSAGE_WRITE:
                        eventSink.success(connectState.writing);
                        break;
                    case MESSAGE_READ:
                        break;
                    case MESSAGE_DEVICE_NAME:
                        // save the connected device's name
                        //mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                        break;
                    case MESSAGE_TOAST:
                        break;
                    case MESSAGE_CONNECTION_LOST:
                        eventSink.success(connectState.lost);
                        break;
                    case MESSAGE_UNABLE_CONNECT:
                        eventSink.success(connectState.failed);
                        break;
                }
            }
        };

        private EventChannel.EventSink eventSink;

        enum connectState {
            connected, connecting, listening, writing, lost, failed
        }

        private final Handler handler = new Handler();
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                initBluetooth();
                onConnect();
            }
        };

        @Override
        public void onListen(Object arguments, EventChannel.EventSink events) {
            runnable.run();
        }

        @Override
        public void onCancel(Object arguments) {
            handler.removeCallbacks(runnable);
        }
    }

//================================ Print command ===============================================

    private void sendDataString(String data) {
        if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            return;
        } else if (data.length() > 0) {
            try {
                data = data + '\n';
                mService.write(data.getBytes(THAI));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private void printImg(Drawable img) {
        Bitmap mBitmap = ((BitmapDrawable) img).getBitmap();
        int nMode = 0;
        int nPaperWidth = 384;
        if (mBitmap != null) {
            byte[] data = PrintPicture.POS_PrintBMP(mBitmap, nPaperWidth, nMode);
            sendDataByte(Command.ESC_Init);
            sendDataByte(Command.LF);
            sendDataByte(data);
            sendDataByte(PrinterCommand.POS_Set_PrtAndFeedPaper(30));
            sendDataByte(PrinterCommand.POS_Set_Cut(1));
            sendDataByte(PrinterCommand.POS_Set_PrtInit());
        }
    }

    private void printEnBarcode(String str) {
        byte[] code = PrinterCommand.getCodeBarCommand(str, 73, 3, 168, 1, 2);
        sendDataByte(new byte[]{0x1b, 0x61, 0x00});
        sendDataByte(code);
    }

    private void printQrCode(String str) {
        byte[] code = PrinterCommand.getBarCommand(str, 1, 3, 8);
        sendDataByte(new byte[]{0x1b, 0x61, 0x00});
        sendDataByte(code);
    }

    private void printString(String msg) {
        msg = msg + '\n';
        sendDataByte(PrinterCommand.POS_Print_Text(msg, THAI, 255, 0, 0, 0));
    }

    private void sendDataByte(byte[] data) {
        if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            return;
        }
        mService.write(data);
    }

    //================================ BT Connection ===============================================
    public void initBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
//            return "Bluetooth is not available"
        } else {
            startBluetooth();
        }
    }

    private void startBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mService == null)
                mService = new BluetoothService(this, mHandler);
        }
    }

    private void onInitBluetooth() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    private String onConnect(String address) {
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            mService.connect(device);

            return new String("Success");
        } else {
            return new String("can't connect Devices");
        }
    }

    private void startDicoveryNewDevices() {
        onInitBluetooth();

        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        mNewDevices.clear();
        mBtAdapter.startDiscovery();
    }

    private List<String> dicoveryNewDevices() {
        return this.mNewDevices;
    }

    private List<String> discoveryPariedDevice() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            mPairedDevices.add("noDevices");
        }

        return this.mPairedDevices;
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            break;
                        case BluetoothService.STATE_CONNECTING:

                            break;
                        case BluetoothService.STATE_LISTEN:
                            break;
                        case BluetoothService.STATE_NONE:
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    break;
                case MESSAGE_READ:
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    //mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
                case MESSAGE_TOAST:
                    break;
                case MESSAGE_CONNECTION_LOST:
                    break;
                case MESSAGE_UNABLE_CONNECT:
                    break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevices.add(device.getName() + "\n" + device.getAddress());
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mNewDevices.isEmpty()) {
                    mNewDevices.add("noDevices");
                }
            }
        }
    };

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }
}
