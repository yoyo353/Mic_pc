import asyncio
import websockets
import pyaudio
import sys
import logging

# Configure Logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("AudioServer")

# Audio Configuration
# These must match the Android AudioRecord settings
FORMAT = pyaudio.paInt16
CHANNELS = 1
RATE = 48000
CHUNK = 960  # 20ms at 48kHz

class AudioServer:
    def __init__(self):
        self.p = pyaudio.PyAudio()
        self.stream = None
        self.connected = False

    def start_audio_stream(self):
        """Initializes the PyAudio output stream."""
        try:
            self.stream = self.p.open(
                format=FORMAT,
                channels=CHANNELS,
                rate=RATE,
                output=True,
                frames_per_buffer=CHUNK
            )
            logger.info("Audio Output Stream Started")
        except Exception as e:
            logger.error(f"Failed to start audio stream: {e}")
            sys.exit(1)

    def stop_audio_stream(self):
        if self.stream:
            self.stream.stop_stream()
            self.stream.close()
        self.p.terminate()
        logger.info("Audio Output Stream Stopped")

    async def audio_handler(self, websocket):
        """Handles incoming WebSocket connections and audio data."""
        remote_ip = websocket.remote_address[0]
        logger.info(f"Client connected from {remote_ip}")
        self.connected = True

        if not self.stream:
            self.start_audio_stream()

        try:
            async for message in websocket:
                # We expect binary audio data
                if isinstance(message, bytes):
                    # Write to PyAudio stream
                    # In a production (Phase 2) app, we would use a jitter buffer here.
                    # For MVP, we write directly to the stream. PyAudio has an internal buffer.
                    try:
                        self.stream.write(message)
                    except OSError as e:
                        # Output buffer might be full or device lost
                        logger.warning(f"Audio write error: {e}")
                else:
                    logger.warning(f"Received non-binary message: {message}")

        except websockets.exceptions.ConnectionClosed:
            logger.info(f"Connection closed by {remote_ip}")
        except Exception as e:
            logger.error(f"Error in connection handler: {e}")
        finally:
            self.connected = False
            # We keep the stream open for re-connections in this MVP design
            logger.info("Client disconnected")

    async def start_server(self, host="0.0.0.0", port=8765):
        logger.info(f"Starting WebSocket server on {host}:{port}")
        # Need to pass the handler properly. websockets.serve takes a coroutine function.
        async with websockets.serve(self.audio_handler, host, port):
            await asyncio.Future()  # run forever

if __name__ == "__main__":
    server = AudioServer()
    try:
        # Check for PyAudio devices
        info = server.p.get_host_api_info_by_index(0)
        numdevices = info.get('deviceCount')
        logger.info(f"Found {numdevices} audio devices. Using default output.")
        
        asyncio.run(server.start_server())
    except KeyboardInterrupt:
        logger.info("Server stopping...")
        server.stop_audio_stream()
