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

import com.example.tscdll.TSCActivity;

/**
 * BtprinterPlugin
 */
public class BtprinterPlugin extends Activity implements FlutterPlugin, MethodCallHandler {
    /******************************************************************************************************/
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CONNECTED = 0;
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
    private static String DEVICE_ADDRESS;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    public static BluetoothAdapter mBluetoothAdapter = null;
    public static BluetoothService mService = null;
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
                return new ConnectionHandler();
            }
        });

        final StreamsChannel zenpertConnectChannel = new StreamsChannel(flutterPluginBinding.getBinaryMessenger(), "zenpert_btprinter_stream");
        zenpertConnectChannel.setStreamHandlerFactory(new StreamsChannel.StreamHandlerFactory() {
            @Override
            public EventChannel.StreamHandler create(Object arguments) {
                return new ZenpertConnectionHandler();
            }
        });
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "btprinter");
        channel.setMethodCallHandler(new BtprinterPlugin());
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("setAddress")) {
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
        } else if (call.method.equals("discoveryNewDevices")) {
            initBluetooth();
            startDicoveryNewDevices();
            result.success("start discovery");
        } else if (call.method.equals("discoveryPariedDevices")) {
            List<String> pariedDevice = discoveryPariedDevice();
            result.success(pariedDevice);
        } else if (call.method.equals("zenpertPrintText")) {
            String msg = call.argument("msg").toString();
            zenpertText(msg);
            result.success("Success");
        } else if (call.method.equals("zenpertPrintBarcode")) {
            String msg = call.argument("msg").toString();
            zenpertBarcode(msg);
            result.success("Success");
        } else if (call.method.equals("zenpertPrintQrCode")) {
            String msg = call.argument("msg").toString();
            zenpertQrcode(msg);
            result.success("Success");
        } else if (call.method.equals("zenpertClose")) {
            zenpertClose();
            result.success("Success");
        } else if (call.method.equals("fujunClose")) {
            onStopConnect();
            result.success("Success");
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

    public static class ConnectionHandler extends Activity implements EventChannel.StreamHandler {

        // Name of the connected device
        private static final int REQUEST_ENABLE_BT = 2;

        private EventChannel.EventSink eventSink;

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
            if (BluetoothAdapter.checkBluetoothAddress(DEVICE_ADDRESS)) {
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
                mService.connect(device);

                return new String("Success");
            } else {
                return new String("can't connect Devices");
            }
        }

        private String disconnect() {
            mService.stop();
            return new String("Success");
        }

        @SuppressLint("HandlerLeak")
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:
                        switch (msg.arg1) {
                            case BluetoothService.STATE_CONNECTED:
                                eventSink.success(BtprinterPlugin.MESSAGE_STATE_CONNECTED);
                                break;
                            case BluetoothService.STATE_CONNECTING:
                                eventSink.success(BtprinterPlugin.MESSAGE_READ);
                                break;
                            case BluetoothService.STATE_LISTEN:
                                break;
                            case BluetoothService.STATE_NONE:
                                break;
                        }
                        break;
                    case MESSAGE_WRITE:
//                        eventSink.success(connectState.writing);
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
                        eventSink.success(BtprinterPlugin.MESSAGE_CONNECTION_LOST);
                        break;
                    case MESSAGE_UNABLE_CONNECT:
                        eventSink.success(BtprinterPlugin.MESSAGE_UNABLE_CONNECT);
                        break;
                }
            }
        };

        private final Handler handler = new Handler();
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    eventSink.success(BtprinterPlugin.MESSAGE_READ);
                    initBluetooth();
                    disconnect();
                    onConnect();
                } catch (Throwable t) {
                    eventSink.success(BtprinterPlugin.MESSAGE_UNABLE_CONNECT);
                }
            }
        };

        @Override
        public void onListen(Object arguments, EventChannel.EventSink events) {
            this.eventSink = events;
            runnable.run();
        }

        @Override
        public void onCancel(Object arguments) {
            handler.removeCallbacks(runnable);
        }
    }

    public static class ZenpertConnectionHandler implements EventChannel.StreamHandler {

        private final Handler handler = new Handler();
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {

                try {
                    String connection = TscDll.openport(DEVICE_ADDRESS);
                    if (connection == "1") {
                        eventSink.success(BtprinterPlugin.MESSAGE_STATE_CONNECTED);
                    } else if (connection == "-1") {
                        eventSink.success(BtprinterPlugin.MESSAGE_UNABLE_CONNECT);
                    }
                } catch (Throwable t) {
                    eventSink.success(BtprinterPlugin.MESSAGE_UNABLE_CONNECT);
                }

                TscDll.clearbuffer();
                BtprinterPlugin.initial(TscDll);
            }
        };

        private EventChannel.EventSink eventSink;

        @Override
        public void onListen(Object o, final EventChannel.EventSink eventSink) {
            this.eventSink = eventSink;
            runnable.run();
        }

        @Override
        public void onCancel(Object o) {
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
        byte[] code = PrinterCommand.getCodeBarCommand(str, 67, 4, 80, 0, 2);
        sendDataByte(new byte[]{0x1b, 0x61, 0x00});
        sendDataByte(code);
    }

    private void printQrCode(String str) {
        byte[] code = PrinterCommand.getBarCommand(str, 1, 3, 8);
        sendDataByte(new byte[]{0x1b, 0x61, 0x00});
        sendDataByte(code);
    }

    private void printString(String msg) {
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

    private String onStopConnect() {
        mService.stop();
        return new String("Success");
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
//        channel.setMethodCallHandler(null);
    }

    //====================================== Zenpert ===============================================

    public static TSCActivity TscDll = new TSCActivity();

    String newLine = "\r\n";
    String FONT = "courmon.TTF";
    int MAX_COLUMN = 31;
    int MARGIN_X_TEXT = 30; //dot
    int MARGIN_X_BARCODE = 30; //dot
    int MARGIN_X_QRCODE = 80; //dot
    int FONT_HEIGHT = 8;
    int FONT_WIDTH = 6;

    public void zenpertClose() {
        TscDll.closeport(1000);
    }

    public static void initial(TSCActivity tscDll) {
        String newLine = "\r\n";
        String FONT = "courmon.TTF";

        //Set Fronts
        tscDll.sendcommand("DOWNLOAD F,\"AUTO.BAS\"" + newLine);
        tscDll.sendcommand("COPY F,\"" + FONT + "\",\"" + FONT + "\"" + newLine);
        tscDll.sendcommand("EOP" + newLine);
        tscDll.sendcommand("AUTO.BAS" + newLine);

        // Set Paper
        tscDll.sendcommand("SIZE 58 mm, 5 mm" + newLine);
        tscDll.sendcommand("Q0,0" + newLine);
        tscDll.sendcommand("CLS" + newLine);
        tscDll.sendcommand("SPEED 4" + newLine);
        tscDll.sendcommand("DENSITY 12" + newLine);
        tscDll.sendcommand("CODEPAGE UTF-8" + newLine);
        tscDll.sendcommand("SET TEAR ON" + newLine);
    }

    private void copyFontToFlashMemory(TSCActivity tscDll) {
        tscDll.sendcommand("DOWNLOAD F,\"AUTO.BAS\"" + newLine);
        tscDll.sendcommand("COPY F,\"" + FONT + "\",\"" + FONT + "\"" + newLine);
        tscDll.sendcommand("EOP" + newLine);
        tscDll.sendcommand("AUTO.BAS" + newLine);
    }

    private void zenpertText(String text) {
        TscDll.sendcommand("TEXT 32, 0,\"" + FONT + "\",0,6,8,\"" + text + "\"\r\n");
        TscDll.printlabel(1, 1);
        TscDll.clearbuffer();
    }

    private void zenpertBarcode(String barcode) {
        TscDll.sendcommand("BARCODE 41,0,\"128\",80,2,0,3,1,0,\"" + barcode + "\"" + newLine + "");
        TscDll.printlabel(1, 1);
        TscDll.clearbuffer();
    }

    private void zenpertQrcode(String qrCode) {
        TscDll.sendcommand("QRCODE 160,0,H,10,A,0,M2,S7,X100,\"" + qrCode + "\"" + newLine + "");
        TscDll.printlabel(1, 1);
        TscDll.clearbuffer();
    }
}
