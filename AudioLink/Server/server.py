import asyncio
import websockets
import pyaudio
import sys
import logging
import opuslib
import struct
from JitterBuffer import JitterBuffer

# Configure Logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger("AudioServer")

# Audio Configuration
FORMAT = pyaudio.paInt16
CHANNELS = 1
RATE = 48000
CHUNK = 960  # 20ms at 48kHz (samples)
FRAME_SIZE = 960  # Opus frame size for 20ms at 48kHz

class AudioServer:
    def __init__(self, use_opus=True):
        """
        Args:
            use_opus: If True, decode Opus. If False, expect raw PCM (backward compatibility).
        """
        self.p = pyaudio.PyAudio()
        self.stream = None
        self.connected = False
        self.use_opus = use_opus
        
        # Initialize Opus Decoder
        if self.use_opus:
            try:
                self.opus_decoder = opuslib.Decoder(RATE, CHANNELS)
                logger.info("Opus decoder initialized")
            except Exception as e:
                logger.error(f"Failed to initialize Opus decoder: {e}")
                sys.exit(1)
        else:
            self.opus_decoder = None
            logger.info("Running in PCM mode (no Opus)")
        
        # Initialize Jitter Buffer
        self.jitter_buffer = JitterBuffer(target_buffer_ms=40, frame_duration_ms=20)
        self.playback_task = None

    def start_audio_stream(self):
        """Initializes the PyAudio output stream."""
        try:
            self.stream = self.p.open(
                format=FORMAT,
                channels=CHANNELS,
                rate=RATE,
                output=True,
                frames_per_buffer=CHUNK,
                stream_callback=self._audio_callback
            )
            logger.info("Audio Output Stream Started (callback mode)")
        except Exception as e:
            logger.error(f"Failed to start audio stream: {e}")
            sys.exit(1)

    def _audio_callback(self, in_data, frame_count, time_info, status):
        """
        PyAudio callback function. Called by PyAudio when it needs more audio data.
        This runs in a separate thread managed by PortAudio.
        """
        # Pop frame from jitter buffer
        frame = self.jitter_buffer.pop()
        
        if frame is None:
            # Initial buffering or no data: return silence
            return (b'\x00' * (frame_count * 2), pyaudio.paContinue)
        
        # frame is already decoded PCM bytes
        return (frame, pyaudio.paContinue)

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

        # Statistics reporting
        stats_counter = 0

        try:
            async for message in websocket:
                if isinstance(message, bytes):
                    # Decode audio
                    if self.use_opus:
                        try:
                            # Opus frame -> PCM
                            pcm_data = self.opus_decoder.decode(message, FRAME_SIZE)
                        except opuslib.OpusError as e:
                            logger.warning(f"Opus decode error: {e}")
                            continue
                    else:
                        # Raw PCM
                        pcm_data = message
                    
                    # Push to jitter buffer
                    self.jitter_buffer.push(pcm_data)
                    
                    # Log statistics every 100 packets (~2 seconds)
                    stats_counter += 1
                    if stats_counter % 100 == 0:
                        stats = self.jitter_buffer.get_stats()
                        logger.info(
                            f"Stats: Depth={stats['current_depth']:.1f} "
                            f"Avg={stats['avg_depth']:.1f} "
                            f"Underruns={stats['underruns']} "
                            f"Overruns={stats['overruns']}"
                        )
                else:
                    logger.warning(f"Received non-binary message: {message}")

        except websockets.exceptions.ConnectionClosed:
            logger.info(f"Connection closed by {remote_ip}")
        except Exception as e:
            logger.error(f"Error in connection handler: {e}")
        finally:
            self.connected = False
            self.jitter_buffer.reset()
            logger.info("Client disconnected")

    async def start_server(self, host="0.0.0.0", port=8765):
        logger.info(f"Starting WebSocket server on {host}:{port}")
        async with websockets.serve(self.audio_handler, host, port):
            await asyncio.Future()  # run forever

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description='AudioLink Server')
    parser.add_argument('--pcm', action='store_true', help='Use PCM mode instead of Opus')
    args = parser.parse_args()
    
    server = AudioServer(use_opus=not args.pcm)
    try:
        # Check for PyAudio devices
        info = server.p.get_host_api_info_by_index(0)
        numdevices = info.get('deviceCount')
        logger.info(f"Found {numdevices} audio devices. Using default output.")
        
        asyncio.run(server.start_server())
    except KeyboardInterrupt:
        logger.info("Server stopping...")
        server.stop_audio_stream()
