import React, { useState, useEffect } from 'react';
import {
  Container, Typography, Box, Card, CardContent, Button,
  List, ListItem, ListItemText, Divider, TextField,
  Grid, Chip, AppBar, Toolbar, IconButton, Paper
} from '@mui/material';
import {
  SettingsRemote, Security, AppSettingsAlt,
  Wifi, CameraAlt, PhonelinkLock, DeleteForever
} from '@mui/icons-material';
import { io } from 'socket.io-client';
import { createTheme, ThemeProvider } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: { main: '#6750A4' }, // Material 3 Primary
    secondary: { main: '#625B71' },
    error: { main: '#B3261E' },
    background: { default: '#FFFBFE' },
  },
  typography: {
    fontFamily: 'Roboto, sans-serif',
  },
});

const socket = io('http://localhost:3000');

function App() {
  const [devices, setDevices] = useState([]);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [logs, setLogs] = useState([]);

  useEffect(() => {
    socket.on('device_list', (deviceList) => {
      setDevices(deviceList);
    });

    socket.on('command_feedback', (result) => {
      setLogs(prev => [`[${new Date().toLocaleTimeString()}] ${result.status.toUpperCase()}: ${result.message}`, ...prev]);
    });

    return () => {
      socket.off('device_list');
      socket.off('command_feedback');
    };
  }, []);

  const sendCommand = (command, params = {}) => {
    if (!selectedDevice) {
      alert("Please select a device first");
      return;
    }
    socket.emit('send_command', {
      targetId: selectedDevice.id,
      command,
      params
    });
  };

  return (
    <ThemeProvider theme={theme}>
      <Box sx={{ flexGrow: 1, minHeight: '100vh', bgcolor: 'background.default' }}>
        <AppBar position="static" elevation={0} sx={{ bgcolor: 'primary.main' }}>
          <Toolbar>
            <SettingsRemote sx={{ mr: 2 }} />
            <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
              Device Admin Dashboard (Remote)
            </Typography>
          </Toolbar>
        </AppBar>

        <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
          <Grid container spacing={3}>
            {/* Devices List */}
            <Grid item xs={12} md={4}>
              <Paper sx={{ p: 2, borderRadius: 3 }}>
                <Typography variant="h6" gutterBottom>Connected Devices</Typography>
                <Divider />
                <List>
                  {devices.map((device) => (
                    <ListItem
                      button
                      key={device.id}
                      selected={selectedDevice?.id === device.id}
                      onClick={() => setSelectedDevice(device)}
                      sx={{ borderRadius: 2, mb: 1 }}
                    >
                      <ListItemText
                        primary={device.deviceName}
                        secondary={`SDK: ${device.sdkVersion} | IP: ${device.ip}`}
                      />
                      <Chip label="Online" color="success" size="small" />
                    </ListItem>
                  ))}
                  {devices.length === 0 && <Typography variant="body2" sx={{ p: 2, color: 'text.secondary' }}>No devices connected</Typography>}
                </List>
              </Paper>
            </Grid>

            {/* Controls */}
            <Grid item xs={12} md={8}>
              <Paper sx={{ p: 2, borderRadius: 3, mb: 3 }}>
                <Typography variant="h6" gutterBottom>
                  Remote Controls {selectedDevice ? ` - ${selectedDevice.deviceName}` : '(Select a device)'}
                </Typography>
                <Divider sx={{ mb: 2 }} />

                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6} md={4}>
                    <Button fullWidth variant="contained" startIcon={<PhonelinkLock />} onClick={() => sendCommand('lock_now')}>
                      Lock Now
                    </Button>
                  </Grid>
                  <Grid item xs={12} sm={6} md={4}>
                    <Button fullWidth variant="outlined" startIcon={<CameraAlt />} onClick={() => sendCommand('disable_camera', { enabled: true })}>
                      Disable Camera
                    </Button>
                  </Grid>
                  <Grid item xs={12} sm={6} md={4}>
                    <Button fullWidth variant="outlined" onClick={() => sendCommand('disable_camera', { enabled: false })}>
                      Enable Camera
                    </Button>
                  </Grid>
                  <Grid item xs={12} sm={6} md={4}>
                    <Button fullWidth variant="outlined" startIcon={<Security />} onClick={() => sendCommand('set_password_policy', { quality: 393216, minLength: 6 })}>
                      Enforce Password
                    </Button>
                  </Grid>
                  <Grid item xs={12} sm={6} md={4}>
                    <Button fullWidth variant="outlined" startIcon={<Wifi />} onClick={() => sendCommand('disallow_config_wifi', { enabled: true })}>
                      Block WiFi Config
                    </Button>
                  </Grid>
                  <Grid item xs={12} sm={6} md={4}>
                    <Button fullWidth variant="contained" color="error" startIcon={<DeleteForever />} onClick={() => {
                      if(window.confirm("Wipe device? This is irreversible!")) sendCommand('wipe_data');
                    }}>
                      Wipe Device
                    </Button>
                  </Grid>
                </Grid>
              </Paper>

              {/* Logs */}
              <Paper sx={{ p: 2, borderRadius: 3, bgcolor: '#1E1E1E', color: '#E0E0E0' }}>
                <Typography variant="h6" gutterBottom sx={{ color: 'white' }}>Live Feed</Typography>
                <Box sx={{ height: '300px', overflowY: 'auto', fontFamily: 'monospace', p: 1 }}>
                  {logs.map((log, i) => (
                    <Typography key={i} variant="body2" sx={{
                      color: log.includes('SUCCESS') ? '#4CAF50' : log.includes('ERROR') ? '#F44336' : '#2196F3'
                    }}>
                      {log}
                    </Typography>
                  ))}
                  {logs.length === 0 && "Waiting for actions..."}
                </Box>
              </Paper>
            </Grid>
          </Grid>
        </Container>
      </Box>
    </ThemeProvider>
  );
}

export default App;
