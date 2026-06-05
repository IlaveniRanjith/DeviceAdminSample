# Device Admin Remote Server

This server allows you to remotely control Android devices enrolled as Device Admins using WebSockets.

## 📁 Structure
- `/backend`: Node.js server using Socket.io.
- `/frontend`: React dashboard with Material 3 design to send commands.

## 🚀 Getting Started

### 1. Backend Setup
```bash
cd backend
npm install
npm start
```
Default port: **3000**

### 2. Frontend Setup
```bash
cd frontend
npm install
npm start
```
Runs at: **http://localhost:3000** (Ensure backend is running)

## 📡 Connecting a Device
1. Open the Android app.
2. Enter the server's **IP Address** and **Port** (e.g., `192.168.1.10:3000`).
3. Click **Connect**.
4. The device will appear in the web dashboard.

## 🛠️ Remote Commands
- **Lock Now**: Instantly locks the device.
- **Camera Control**: Remote enable/disable.
- **WiFi Restriction**: Block user from changing WiFi settings.
- **Wipe Device**: Factory reset (USE WITH CAUTION).
- **Password Policy**: Enforce complex passwords remotely.
