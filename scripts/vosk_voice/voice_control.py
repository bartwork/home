#!/usr/bin/env python3
"""
Локальный голосовой контроль для home (Vosk → MQTT Wiren Board).

Установка:
  cd scripts/vosk_voice
  python3 -m venv .venv
  source .venv/bin/activate          # Windows: .venv\\Scripts\\activate
  pip install -r requirements.txt
  python voice_control.py --download-model

Запуск (сначала dry-run — только печать):
  python voice_control.py

С реальной отправкой в MQTT:
  MQTT_BROKER=tcp://192.168.1.2:1883 python voice_control.py --mqtt

Планшет по сети — см. voice_ws_server.py.

Примеры фраз: «включи коридор», «выключи кухню», «выключи всё».
Список правил — phrases.json (можно править).
"""

from __future__ import annotations

import argparse
import json
import os
import queue
import sys
from pathlib import Path

from voice_core import (
    DEFAULT_BROKER,
    DEFAULT_MODEL,
    ROOT,
    MqttOut,
    apply_command,
    download_model,
    list_commands,
    load_commands,
    log,
)


def run_mic(model_path: Path, commands: list[dict], mqtt: MqttOut, sample_rate: int) -> None:
    try:
        import sounddevice as sd
        from vosk import KaldiRecognizer, Model, SetLogLevel
    except ImportError as e:
        sys.exit(f"зависимости: pip install -r requirements.txt\n{e}")

    SetLogLevel(-1)
    model = Model(str(model_path))
    rec = KaldiRecognizer(model, sample_rate)
    rec.SetWords(False)

    q: queue.Queue[bytes] = queue.Queue()

    def callback(indata, frames, time_info, status) -> None:  # noqa: ARG001
        if status:
            log(f"audio: {status}")
        q.put(bytes(indata))

    log("слушаю микрофон… примеры:")
    log("  «включи коридор» · «отключи кухню» · «включи лоджию»")
    log("  «включи тёплый пол в коридоре» · «выключи всё»")
    log("Ctrl+C — выход")
    with sd.RawInputStream(
        samplerate=sample_rate,
        blocksize=8000,
        dtype="int16",
        channels=1,
        callback=callback,
    ):
        while True:
            data = q.get()
            if rec.AcceptWaveform(data):
                payload = json.loads(rec.Result())
                text = (payload.get("text") or "").strip()
                if not text:
                    continue
                log(f"→ {text}")
                apply_command(text, commands, mqtt)


def main() -> None:
    ap = argparse.ArgumentParser(description="Vosk → MQTT для home / Wiren Board")
    ap.add_argument("--download-model", action="store_true", help="скачать small RU модель и выйти")
    ap.add_argument("--model", type=Path, default=None, help="путь к каталогу модели Vosk")
    ap.add_argument("--phrases", type=Path, default=ROOT / "phrases.json")
    ap.add_argument("--list", action="store_true", help="показать навыки и выйти")
    ap.add_argument("--mqtt", action="store_true", help="публиковать в брокер (иначе dry-run)")
    ap.add_argument("--broker", default=os.environ.get("MQTT_BROKER", DEFAULT_BROKER))
    ap.add_argument("--rate", type=int, default=16000)
    args = ap.parse_args()

    models_root = ROOT / "models"
    if args.download_model:
        download_model(models_root)
        return

    commands = load_commands(args.phrases)
    if args.list:
        list_commands(commands)
        return

    model_path = args.model or (models_root / DEFAULT_MODEL)
    if not model_path.is_dir():
        log(f"нет модели {model_path}")
        log("сначала: python voice_control.py --download-model")
        sys.exit(1)

    log(f"правил: {len(commands)} из {args.phrases}")
    if not args.mqtt:
        log("режим dry-run (без MQTT). Для отправки: добавьте --mqtt")

    mqtt = MqttOut(args.broker, dry_run=not args.mqtt)
    try:
        run_mic(model_path, commands, mqtt, args.rate)
    except KeyboardInterrupt:
        log("\nстоп")
    finally:
        mqtt.close()


if __name__ == "__main__":
    main()
