const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');

const app = express();
app.use(cors());
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

let connectedDevices = {};

io.on('connection', (socket) => {
    console.log('New client connected:', socket.id);

    socket.on('register_device', (deviceInfo) => {
        connectedDevices[socket.id] = { ...deviceInfo, id: socket.id };
        console.log('Device registered:', deviceInfo.deviceName);
        io.emit('device_list', Object.values(connectedDevices));
    });

    socket.on('send_command', (data) => {
        const { targetId, command, params } = data;
        console.log(`Sending command ${command} to ${targetId}`);
        io.to(targetId).emit('execute_command', { command, params });
    });

    socket.on('command_result', (result) => {
        console.log('Command result received:', result);
        io.emit('command_feedback', result);
    });

    socket.on('disconnect', () => {
        console.log('Client disconnected:', socket.id);
        delete connectedDevices[socket.id];
        io.emit('device_list', Object.values(connectedDevices));
    });
});

const PORT = process.env.PORT || 3001;
server.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
