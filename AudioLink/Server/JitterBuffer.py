import collections
import time
import logging

logger = logging.getLogger("JitterBuffer")

class JitterBuffer:
    """
    Adaptive Jitter Buffer for audio streaming.
    
    Handles:
    - Packet reordering (if sequence numbers are added later)
    - Dynamic buffering based on network jitter
    - Underrun protection (frame repetition)
    - Statistics tracking
    """
    
    def __init__(self, target_buffer_ms=40, min_buffer_ms=20, max_buffer_ms=100, frame_duration_ms=20):
        """
        Args:
            target_buffer_ms: Target buffer depth in milliseconds
            min_buffer_ms: Minimum buffer depth
            max_buffer_ms: Maximum buffer depth
            frame_duration_ms: Duration of each audio frame (20ms for Opus)
        """
        self.target_frames = target_buffer_ms // frame_duration_ms
        self.min_frames = min_buffer_ms // frame_duration_ms
        self.max_frames = max_buffer_ms // frame_duration_ms
        self.frame_duration_ms = frame_duration_ms
        
        self.buffer = collections.deque(maxlen=self.max_frames * 2)
        self.last_frame = None  # For frame repetition on underrun
        
        # Statistics
        self.stats = {
            'underruns': 0,
            'overruns': 0,
            'packets_received': 0,
            'packets_played': 0,
            'current_depth': 0,
            'avg_depth': 0
        }
        self.depth_samples = collections.deque(maxlen=100)
        
    def push(self, frame_data):
        """Add a frame to the buffer."""
        if len(self.buffer) >= self.max_frames * 2:
            self.stats['overruns'] += 1
            logger.warning(f"Buffer overrun! Dropping oldest frame. Depth: {len(self.buffer)}")
            # Let deque handle it with maxlen
        
        self.buffer.append(frame_data)
        self.stats['packets_received'] += 1
        self.last_frame = frame_data
        
    def pop(self):
        """
        Get the next frame to play.
        Returns None if buffer is building up (initial buffering).
        Returns repeated frame if underrun occurs.
        """
        current_depth = len(self.buffer)
        self.stats['current_depth'] = current_depth
        self.depth_samples.append(current_depth)
        
        if self.depth_samples:
            self.stats['avg_depth'] = sum(self.depth_samples) / len(self.depth_samples)
        
        # Initial buffering: wait until we have target_frames
        if current_depth < self.target_frames and self.stats['packets_played'] == 0:
            logger.debug(f"Initial buffering... {current_depth}/{self.target_frames}")
            return None
        
        # Normal operation
        if current_depth > 0:
            frame = self.buffer.popleft()
            self.stats['packets_played'] += 1
            self.last_frame = frame
            return frame
        else:
            # Underrun: repeat last frame
            self.stats['underruns'] += 1
            if self.stats['underruns'] % 10 == 1:  # Log every 10th underrun
                logger.warning(f"Buffer underrun! Repeating last frame. Total underruns: {self.stats['underruns']}")
            return self.last_frame
    
    def get_stats(self):
        """Return current buffer statistics."""
        return self.stats.copy()
    
    def reset(self):
        """Clear the buffer and reset statistics."""
        self.buffer.clear()
        self.last_frame = None
        self.stats = {
            'underruns': 0,
            'overruns': 0,
            'packets_received': 0,
            'packets_played': 0,
            'current_depth': 0,
            'avg_depth': 0
        }
        self.depth_samples.clear()
