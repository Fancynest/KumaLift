#!/usr/bin/env python3
"""
KumaLift Sound Identity Processing Script
Processes raw Opus sounds from assets/audio/ into:
  1. 48 kHz 16-bit Master WAVs in assets/audio/master/<item>.wav
  2. Ogg Vorbis app resources in composeApp/src/androidMain/res/raw/kumalift_<item>.ogg
Generates validation table in assets/audio/README.md and license template in assets/audio/LICENSE-NOTES.md.
"""

from pathlib import Path
import subprocess
import numpy as np
import scipy.signal
import soundfile as sf
import pyloudnorm as pyln
import imageio_ffmpeg

REPO_ROOT = Path(__file__).resolve().parents[1]
INPUT_DIR = REPO_ROOT / "assets" / "audio"
MASTER_DIR = REPO_ROOT / "assets" / "audio" / "master"
APP_RES_DIR = REPO_ROOT / "composeApp" / "src" / "androidMain" / "res" / "raw"
README_PATH = REPO_ROOT / "assets" / "audio" / "README.md"
LICENSE_NOTES_PATH = REPO_ROOT / "assets" / "audio" / "LICENSE-NOTES.md"

TARGET_CONFIGS = {
    "rest_end": {
        "target_sec": 1.0,
        "type": "tonal",
        "target_lufs": -20.0,
        "stereo_allowed": False
    },
    "tick": {
        "target_sec": 0.12,
        "type": "click",
        "target_peak_dbfs": -18.0,
        "stereo_allowed": False
    },
    "set_done": {
        "target_sec": 0.15,
        "type": "click",
        "target_peak_dbfs": -12.0,
        "stereo_allowed": False
    },
    "pr": {
        "target_sec": 1.4,
        "type": "tonal",
        "target_lufs": -20.0,
        "stereo_allowed": True
    },
    "workout_done": {
        "target_sec": 1.8,
        "type": "tonal",
        "target_lufs": -20.0,
        "stereo_allowed": True
    },
    "reminder": {
        "target_sec": 1.2,
        "type": "tonal",
        "target_lufs": -20.0,
        "stereo_allowed": False
    },
    "error": {
        "target_sec": 0.5,
        "type": "tonal",
        "target_lufs": -23.0,
        "stereo_allowed": False
    }
}

def decode_audio(file_path: Path):
    """Decodes audio to float32 numpy array and sample rate."""
    try:
        data, sr = sf.read(str(file_path), dtype="float32")
        return data, sr
    except Exception:
        # Fallback using imageio_ffmpeg
        ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
        cmd = [
            ffmpeg_exe, "-i", str(file_path),
            "-f", "f32le", "-ar", "48000", "-ac", "2", "-"
        ]
        proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
        raw_bytes = proc.stdout
        data = np.frombuffer(raw_bytes, dtype=np.float32).reshape(-1, 2)
        return data, 48000

def write_ogg_vorbis(out_path: Path, data: np.ndarray, sr: int):
    """Writes audio to Ogg Vorbis format."""
    try:
        sf.write(str(out_path), data, sr, format="OGG", subtype="VORBIS")
    except Exception:
        # Fallback to ffmpeg encoding
        ffmpeg_exe = imageio_ffmpeg.get_ffmpeg_exe()
        temp_wav = out_path.with_suffix(".temp.wav")
        sf.write(str(temp_wav), data, sr, format="WAV", subtype="PCM_16")
        cmd = [
            ffmpeg_exe, "-y", "-i", str(temp_wav),
            "-c:a", "libvorbis", "-q:a", "5", str(out_path)
        ]
        subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
        temp_wav.unlink(missing_ok=True)

def main():
    MASTER_DIR.mkdir(parents=True, exist_ok=True)
    APP_RES_DIR.mkdir(parents=True, exist_ok=True)

    # 1. Discover files
    audio_files = [f for f in INPUT_DIR.iterdir() if f.is_file() and f.suffix.lower() in [".opus", ".ogg", ".wav", ".mp3"]]
    item_file_map = {}
    missing_items = []
    ambiguous_items = []

    for item in TARGET_CONFIGS.keys():
        matches = [f for f in audio_files if item.lower() in f.stem.lower()]
        if len(matches) == 1:
            item_file_map[item] = matches[0]
        elif len(matches) == 0:
            missing_items.append(item)
        else:
            ambiguous_items.append((item, [f.name for f in matches]))

    results = []

    meter = pyln.Meter(48000)

    for item, cfg in TARGET_CONFIGS.items():
        if item not in item_file_map:
            continue
        src_file = item_file_map[item]
        data, sr = decode_audio(src_file)

        # Resample to 48 kHz if needed
        if sr != 48000:
            num_samples = int(len(data) * 48000 / sr)
            data = scipy.signal.resample(data, num_samples, axis=0)
            sr = 48000

        # Downmix to mono if channels near-identical or stereo not allowed
        if data.ndim == 2:
            left, right = data[:, 0], data[:, 1]
            diff = np.max(np.abs(left - right))
            near_identical = diff < 1e-3
            if near_identical or not cfg["stereo_allowed"]:
                data = np.mean(data, axis=1)

        # Remove DC offset
        data = data - np.mean(data, axis=0)

        # Detect onset (-45 dBFS = 10^(-45/20) approx 0.005623)
        thresh_45 = 10.0 ** (-45.0 / 20.0)
        amp = np.max(np.abs(data), axis=1) if data.ndim > 1 else np.abs(data)
        onset_indices = np.where(amp >= thresh_45)[0]
        if len(onset_indices) > 0:
            first_onset = onset_indices[0]
        else:
            first_onset = 0

        pre_roll_samples = int(0.004 * 48000) # 4 ms
        start_idx = max(0, first_onset - pre_roll_samples)
        leading_silence_ms = (first_onset - start_idx) / 48000.0 * 1000.0
        data = data[start_idx:]

        # Trim / Cut to target length
        target_sec = cfg["target_sec"]
        target_samples = int(target_sec * 48000)
        is_shorter = False

        if len(data) < target_samples:
            is_shorter = True
            # Trim trailing silence below -60 dBFS first
            thresh_60 = 10.0 ** (-60.0 / 20.0)
            amp_tail = np.max(np.abs(data), axis=1) if data.ndim > 1 else np.abs(data)
            above_60 = np.where(amp_tail >= thresh_60)[0]
            if len(above_60) > 0:
                last_idx = above_60[-1] + 1
                data = data[:min(len(data), last_idx + int(0.040 * 48000))]
        else:
            data = data[:target_samples]

        # 3 ms fade-in, 40 ms fade-out
        fade_in_len = int(0.003 * 48000)
        fade_out_len = int(0.040 * 48000)

        if len(data) > fade_in_len + fade_out_len:
            fade_in = np.linspace(0.0, 1.0, fade_in_len)
            fade_out = np.linspace(1.0, 0.0, fade_out_len)
            if data.ndim == 1:
                data[:fade_in_len] *= fade_in
                data[-fade_out_len:] *= fade_out
            else:
                data[:fade_in_len, :] *= fade_in[:, np.newaxis]
                data[-fade_out_len:, :] *= fade_out[:, np.newaxis]

        # Filters: 80 Hz high-pass, 7 kHz low-pass (4th-order Butterworth, zero-phase)
        sos = scipy.signal.butter(4, [80.0, 7000.0], btype="bandpass", fs=48000, output="sos")
        data = scipy.signal.sosfiltfilt(sos, data, axis=0)

        # Levels
        if cfg["type"] == "tonal":
            current_lufs = meter.integrated_loudness(data)
            target_lufs = cfg["target_lufs"]
            data = pyln.normalize.loudness(data, current_lufs, target_lufs)
            # Limiter-free gain reduction if peak > -3 dBFS
            max_peak = 10.0 ** (-3.0 / 20.0)
            cur_peak = np.max(np.abs(data))
            if cur_peak > max_peak:
                data = data * (max_peak / cur_peak)
            final_lufs = meter.integrated_loudness(data)
        else:
            target_peak_dbfs = cfg["target_peak_dbfs"]
            target_peak = 10.0 ** (target_peak_dbfs / 20.0)
            cur_peak = np.max(np.abs(data))
            if cur_peak > 0:
                data = data * (target_peak / cur_peak)
            final_lufs = None

        final_peak = np.max(np.abs(data))
        final_peak_dbfs = 20.0 * np.log10(final_peak) if final_peak > 0 else -100.0
        final_duration_sec = len(data) / 48000.0

        # Calculate high frequency energy above 8 kHz relative to peak
        fft_data = np.fft.rfft(data, axis=0)
        power = np.abs(fft_data) ** 2
        if power.ndim > 1:
            power = np.max(power, axis=1)
        freqs = np.fft.rfftfreq(len(data), 1.0 / 48000.0)
        peak_power = np.max(power) if len(power) > 0 else 1e-12
        high_power = np.max(power[freqs >= 8000.0]) if np.any(freqs >= 8000.0) else 1e-12
        hf_energy_rel_peak_db = 10.0 * np.log10(high_power / peak_power) if peak_power > 0 and high_power > 0 else -100.0

        # Write outputs
        # Output 1: Master 48kHz 16-bit WAV
        master_path = MASTER_DIR / f"{item}.wav"
        sf.write(str(master_path), data, 48000, format="WAV", subtype="PCM_16")

        # Output 2: App Ogg Vorbis
        app_ogg_name = f"kumalift_{item}.ogg"
        app_path = APP_RES_DIR / app_ogg_name
        write_ogg_vorbis(app_path, data, 48000)
        file_size_bytes = app_path.stat().st_size

        # Validation checks
        dur_tolerance = 0.15
        target_dur = cfg["target_sec"]
        dur_ok = is_shorter or (abs(final_duration_sec - target_dur) / target_dur <= dur_tolerance)
        clipping_ok = final_peak <= (1.0 + 1e-5) and (cfg["type"] != "tonal" or final_peak_dbfs <= -2.99)
        hf_ok = hf_energy_rel_peak_db < -35.0
        silence_ok = leading_silence_ms <= 10.0

        status = "PASS" if (dur_ok and clipping_ok and hf_ok and silence_ok) else "FAIL"

        results.append({
            "item": item,
            "src_file": src_file.name,
            "duration": final_duration_sec,
            "target_duration": target_dur,
            "is_shorter": is_shorter,
            "peak_dbfs": final_peak_dbfs,
            "lufs": final_lufs,
            "clipping": "No" if clipping_ok else "YES",
            "hf_rel_peak_db": hf_energy_rel_peak_db,
            "leading_silence_ms": leading_silence_ms,
            "channels": "Stereo" if data.ndim == 2 else "Mono",
            "size_bytes": file_size_bytes,
            "status": status
        })

    # Print summary table
    print("\n=== KumaLift Sound Processing Results ===")
    header = f"{'Item':<14} | {'Duration (s)':<12} | {'Peak dBFS':<10} | {'LUFS':<8} | {'Clip':<5} | {'HF >8k dB':<10} | {'Pre-roll':<9} | {'Size':<8} | {'Status'}"
    print(header)
    print("-" * len(header))
    for r in results:
        lufs_str = f"{r['lufs']:.1f}" if r['lufs'] is not None else "N/A (click)"
        print(f"{r['item']:<14} | {r['duration']:<12.3f} | {r['peak_dbfs']:<10.2f} | {lufs_str:<8} | {r['clipping']:<5} | {r['hf_rel_peak_db']:<10.1f} | {r['leading_silence_ms']:<6.1f} ms | {r['size_bytes']:<8} | {r['status']}")

    if missing_items:
        print("\nMissing items:", missing_items)
    if ambiguous_items:
        print("\nAmbiguous items:", ambiguous_items)

    # Write assets/audio/README.md
    readme_content = generate_readme(results, missing_items, ambiguous_items)
    README_PATH.write_text(readme_content, encoding="utf-8")
    print(f"\nWritten documentation to {README_PATH}")

    # Write assets/audio/LICENSE-NOTES.md
    license_content = generate_license_notes()
    LICENSE_NOTES_PATH.write_text(license_content, encoding="utf-8")
    print(f"Written license template to {LICENSE_NOTES_PATH}")

def generate_readme(results, missing, ambiguous):
    lines = [
        "# KumaLift Audio Assets & Sound Identity",
        "",
        "## Sound Design Goal",
        "Calm, warm, low-mid, masculine and premium, never startling or annoying. Quiet by default.",
        "Engineered for high daily frequency (20+ exposures/day). Free of beeps, bells, alarms, buzzers, or looping artifacts.",
        "",
        "## Master & Production Assets",
        "- **Raw Sources**: `assets/audio/<name>.opus` (original untouched generation files)",
        "- **Master WAVs**: `assets/audio/master/<item>.wav` (48 kHz, 16-bit PCM)",
        "- **Android Production**: `composeApp/src/androidMain/res/raw/kumalift_<item>.ogg` (Ogg Vorbis q5)",
        "- **iOS Status**: iOS audio playback is currently stubbed with no-op actuals. Audio implementation for iOS will be handled in a dedicated release phase.",
        "",
        "## Processing Validation Table",
        "",
        "| Item | Source File | Duration (s) | Target (s) | Peak (dBFS) | Loudness (LUFS) | Clipping | HF >8 kHz Rel Peak | Leading Silence | Channels | Size (bytes) | Status |",
        "| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |"
    ]
    for r in results:
        lufs_str = f"{r['lufs']:.1f} LUFS" if r['lufs'] is not None else "N/A (peak norm)"
        lines.append(f"| `{r['item']}` | `{r['src_file']}` | {r['duration']:.3f} | {r['target_duration']:.2f} | {r['peak_dbfs']:.2f} | {lufs_str} | {r['clipping']} | {r['hf_rel_peak_db']:.1f} dB | {r['leading_silence_ms']:.1f} ms | {r['channels']} | {r['size_bytes']} | **{r['status']}** |")

    lines.append("")
    if missing:
        lines.append(f"**Missing Items**: {', '.join(missing)}\n")
    if ambiguous:
        lines.append(f"**Ambiguous Items**: {ambiguous}\n")

    lines.extend([
        "## Technical Tolerances & DSP Spec",
        "- **Sample Rate**: 48,000 Hz float32 resampled.",
        "- **Filtering**: Zero-phase 4th-order Butterworth bandpass (80 Hz high-pass, 7,000 Hz low-pass).",
        "- **DC Offset**: Centered to 0.0 mean.",
        "- **Onset Alignment**: 4 ms pre-roll before first sample exceeding -45 dBFS.",
        "- **Fades**: 3 ms fade-in, 40 ms fade-out.",
        "- **Levels**:",
        "  - Tonal (`rest_end`, `pr`, `workout_done`, `reminder`): -20.0 LUFS integrated loudness, True Peak <= -3.0 dBFS.",
        "  - Error (`error`): -23.0 LUFS integrated loudness, True Peak <= -3.0 dBFS.",
        "  - Clicks (`tick`): Peak -18.0 dBFS.",
        "  - Clicks (`set_done`): Peak -12.0 dBFS.",
        "- **Spectral Envelope**: Energy above 8 kHz relative to spectral peak is strictly under -35 dB.",
        ""
    ])
    return "\n".join(lines)

def generate_license_notes():
    return """# Audio Generation & Licensing Record

> [!IMPORTANT]
> Licensing and commercial terms must be verified before any public release or app store publication.

Please complete the fields below to document the provenance of the audio files in this directory:

- **Tool Name**: [e.g. ElevenLabs Sound Effects / AudioCraft / Suno / etc.]
- **Generation Date**: [YYYY-MM-DD]
- **Subscription Plan Active at Generation Time**: [e.g. Pro Commercial Plan / Creator / Free Tier]
- **Terms of Service URL**: [Link to tool TOS / commercial license terms]
- **Commercial Use Permitted**: [Yes / No]
- **Attribution Required**: [Yes / No / Details]

### Generation Prompts Record
- `rest_end`: [Insert text prompt used]
- `tick`: [Insert text prompt used]
- `set_done`: [Insert text prompt used]
- `pr`: [Insert text prompt used]
- `workout_done`: [Insert text prompt used]
- `reminder`: [Insert text prompt used]
- `error`: [Insert text prompt used]
"""

if __name__ == "__main__":
    main()
