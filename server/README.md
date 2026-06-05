# Device Admin Remote Server

This server allows you to remotely control Android devices enrolled as Device Admins using WebSockets.

## 📁 Structure
- `/backend`: Node.js server using Socket.io.
- `/frontend`: React dashboard with Material 3 design to send commands.

## 🚀 Getting Started

### Unified Start (Recommended)
You can start both the backend and frontend with a single command from the `server` root folder:

```bash
# First time setup (installs all dependencies)
npm run setup

# Start both services
npm start

# To Stop
Press `Ctrl+C` in the terminal.
```

### Manual Setup
If you prefer starting them separately:

1. **Backend**: `cd backend && npm start` (Port: 3001)
2. **Frontend**: `cd frontend && npm start` (Port: 3000)

## 📡 Connecting a Device
1. Open the Android app.
2. Enter the server's **IP Address** and **Port** (e.g., `192.168.1.10:3001`).
3. Click **Connect**.
4. The device will appear in the web dashboard.

## 🛠️ Remote Commands
- **Lock Now**: Instantly locks the device.
- **Camera Control**: Remote enable/disable.
- **WiFi Restriction**: Block user from changing WiFi settings.
- **Wipe Device**: Factory reset (USE WITH CAUTION).
- **Password Policy**: Enforce complex passwords remotely.
