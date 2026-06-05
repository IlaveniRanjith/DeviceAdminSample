package com.ram.deviceadmin;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class RemoteConnectionManager {
    private static final String TAG = "RemoteConnection";
    private static RemoteConnectionManager instance;
    private Socket socket;
    private final Context context;
    private final DevicePolicyManagerGateway gateway;

    private RemoteConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.gateway = new DevicePolicyManagerGateway(this.context);
    }

    public static synchronized RemoteConnectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new RemoteConnectionManager(context);
        }
        return instance;
    }

    public void saveConnectionDetails(String ip, String port) {
        context.getSharedPreferences("remote_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("server_ip", ip)
                .putString("server_port", port)
                .apply();
    }

    public String getServerIp() {
        return context.getSharedPreferences("remote_prefs", Context.MODE_PRIVATE).getString("server_ip", "");
    }

    public String getServerPort() {
        return context.getSharedPreferences("remote_prefs", Context.MODE_PRIVATE).getString("server_port", "3000");
    }

    public void connect(String ip, String port, ConnectionCallback callback) {
        try {
            if (socket != null && socket.connected()) {
                socket.disconnect();
            }

            String url = "http://" + ip + ":" + port;
            socket = IO.socket(url);

            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "Connected to server");
                registerDevice();
                if (callback != null) callback.onConnected();
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "Disconnected from server");
                if (callback != null) callback.onDisconnected();
            });

            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.e(TAG, "Connection error: " + args[0]);
                if (callback != null) callback.onError(args[0].toString());
            });

            socket.on("execute_command", args -> {
                JSONObject data = (JSONObject) args[0];
                handleCommand(data);
            });

            socket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid URL", e);
            if (callback != null) callback.onError("Invalid IP/Port format");
        }
    }

    private void registerDevice() {
        try {
            JSONObject deviceStats = new JSONObject();
            deviceStats.put("deviceName", Build.MODEL);
            deviceStats.put("sdkVersion", Build.VERSION.SDK_INT);
            deviceStats.put("ip", "N/A"); // Can be improved
            socket.emit("register_device", deviceStats);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleCommand(JSONObject data) {
        try {
            String command = data.getString("command");
            JSONObject params = data.optJSONObject("params");
            Log.d(TAG, "Executing remote command: " + command);

            switch (command) {
                case "lock_now":
                    gateway.lockNow(msg -> sendFeedback(command, "success", msg), 
                                   e -> sendFeedback(command, "error", e.getMessage()));
                    break;
                case "disable_camera":
                    boolean camEnabled = params.getBoolean("enabled");
                    gateway.setCameraDisabled(camEnabled, 
                                   msg -> sendFeedback(command, "success", msg), 
                                   e -> sendFeedback(command, "error", e.getMessage()));
                    break;
                case "disallow_config_wifi":
                    boolean wifiDisabled = params.getBoolean("enabled");
                    gateway.setUserRestriction(android.os.UserManager.DISALLOW_CONFIG_WIFI, wifiDisabled,
                                   msg -> sendFeedback(command, "success", msg), 
                                   e -> sendFeedback(command, "error", e.getMessage()));
                    break;
                case "wipe_data":
                    gateway.wipeData(0, msg -> sendFeedback(command, "success", msg), 
                                   e -> sendFeedback(command, "error", e.getMessage()));
                    break;
                case "set_password_policy":
                    int quality = params.getInt("quality");
                    int length = params.getInt("minLength");
                    try {
                        gateway.getDpm().setPasswordQuality(gateway.getAdmin(), quality);
                        gateway.getDpm().setPasswordMinimumLength(gateway.getAdmin(), length);
                        sendFeedback(command, "success", "Password policy updated");
                    } catch (Exception e) {
                        sendFeedback(command, "error", e.getMessage());
                    }
                    break;
                default:
                    sendFeedback(command, "error", "Unknown command: " + command);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Command parsing error", e);
        }
    }

    private void sendFeedback(String command, String status, String message) {
        try {
            JSONObject feedback = new JSONObject();
            feedback.put("deviceName", Build.MODEL);
            feedback.put("command", command);
            feedback.put("status", status);
            feedback.put("message", message);
            socket.emit("command_result", feedback);
            
            // Also log locally in the app
            LogRepository.appendLog(context, "Remote Command [" + command + "]: " + message, status);
            context.sendBroadcast(new Intent(MyDeviceAdminReceiver.ACTION_LOG_EVENT).setPackage(context.getPackageName()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (socket != null) socket.disconnect();
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    public interface ConnectionCallback {
        void onConnected();
        void onDisconnected();
        void onError(String error);
    }
}