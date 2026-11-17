# KumaLift Audio Assets & Sound Identity

## Sound Design Goal
Calm, warm, low-mid, masculine and premium, never startling or annoying. Quiet by default.
Engineered for high daily frequency (20+ exposures/day). Free of beeps, bells, alarms, buzzers, or looping artifacts.

## Master & Production Assets
- **Raw Sources**: `assets/audio/<name>.opus` (original untouched generation files)
- **Master WAVs**: `assets/audio/master/<item>.wav` (48 kHz, 16-bit PCM)
- **Android Production**: `composeApp/src/androidMain/res/raw/kumalift_<item>.ogg` (Ogg Vorbis q5)
- **iOS Status**: iOS audio playback is currently stubbed with no-op actuals. Audio implementation for iOS will be handled in a dedicated release phase.

## Processing Validation Table

| Item | Source File | Duration (s) | Target (s) | Peak (dBFS) | Loudness (LUFS) | Clipping | HF >8 kHz Rel Peak | Leading Silence | Channels | Size (bytes) | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `rest_end` | `rest_end.opus` | 1.000 | 1.00 | -8.28 | -20.0 LUFS | No | -90.3 dB | 0.0 ms | Mono | 8797 | **PASS** |
| `tick` | `tick.opus` | 0.120 | 0.12 | -18.00 | N/A (peak norm) | No | -74.0 dB | 0.0 ms | Mono | 4522 | **PASS** |
| `set_done` | `set_done.opus` | 0.150 | 0.15 | -12.00 | N/A (peak norm) | No | -69.6 dB | 4.0 ms | Mono | 4884 | **PASS** |
| `pr` | `pr.opus` | 1.400 | 1.40 | -9.21 | -20.0 LUFS | No | -85.5 dB | 4.0 ms | Stereo | 19432 | **PASS** |
| `workout_done` | `workout_done.opus` | 1.800 | 1.80 | -6.18 | -20.0 LUFS | No | -89.1 dB | 1.0 ms | Stereo | 22681 | **PASS** |
| `reminder` | `reminder.opus` | 1.200 | 1.20 | -9.86 | -20.0 LUFS | No | -91.7 dB | 0.0 ms | Mono | 10216 | **PASS** |
| `error` | `error.opus` | 0.500 | 0.50 | -10.72 | -23.0 LUFS | No | -80.4 dB | 0.0 ms | Mono | 6407 | **PASS** |

## Technical Tolerances & DSP Spec
- **Sample Rate**: 48,000 Hz float32 resampled.
- **Filtering**: Zero-phase 4th-order Butterworth bandpass (80 Hz high-pass, 7,000 Hz low-pass).
- **DC Offset**: Centered to 0.0 mean.
- **Onset Alignment**: 4 ms pre-roll before first sample exceeding -45 dBFS.
- **Fades**: 3 ms fade-in, 40 ms fade-out.
- **Levels**:
  - Tonal (`rest_end`, `pr`, `workout_done`, `reminder`): -20.0 LUFS integrated loudness, True Peak <= -3.0 dBFS.
  - Error (`error`): -23.0 LUFS integrated loudness, True Peak <= -3.0 dBFS.
  - Clicks (`tick`): Peak -18.0 dBFS.
  - Clicks (`set_done`): Peak -12.0 dBFS.
- **Spectral Envelope**: Energy above 8 kHz relative to spectral peak is strictly under -35 dB.
