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
  MQTT_BROKER=tcp://192.168.1.9:1883 python voice_control.py --mqtt

Примеры фраз: «включи коридор», «выключи кухню», «выключи всё».
Список правил — phrases.json (можно править).
"""

from __future__ import annotations

import argparse
import json
import os
import queue
import sys
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent
DEFAULT_MODEL = "vosk-model-small-ru-0.22"
MODEL_URL = f"https://alphacephei.com/vosk/models/{DEFAULT_MODEL}.zip"


def log(msg: str) -> None:
    print(msg, flush=True)


def download_model(dest: Path) -> Path:
    dest.mkdir(parents=True, exist_ok=True)
    model_dir = dest / DEFAULT_MODEL
    if model_dir.is_dir():
        log(f"модель уже есть: {model_dir}")
        return model_dir

    zip_path = dest / f"{DEFAULT_MODEL}.zip"
    log(f"скачиваю {MODEL_URL} …")
    urllib.request.urlretrieve(MODEL_URL, zip_path)
    log(f"распаковываю в {dest} …")
    with zipfile.ZipFile(zip_path, "r") as zf:
        zf.extractall(dest)
    zip_path.unlink(missing_ok=True)
    if not model_dir.is_dir():
        sys.exit(f"после распаковки нет каталога {model_dir}")
    log(f"готово: {model_dir}")
    return model_dir


def load_commands(path: Path) -> list[dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    cmds = data.get("commands") or []
    if not cmds:
        sys.exit(f"в {path} нет commands")
    return cmds


# Приводим разговорные варианты к канону из phrases.json.
_WORD_MAP = {
    "отключи": "выключи",
    "отключить": "выключи",
    "выключить": "выключи",
    "выруби": "выключи",
    "вырубить": "выключи",
    "включить": "включи",
    "зажигай": "включи",
    "зажги": "включи",
    "погаси": "выключи",
    "погас": "выключи",
    "зал": "комната",
    "зале": "комната",
    "залу": "комната",
    "гостиной": "комната",
    "гостиная": "комната",
    "лоджию": "лоджия",
    "лоджие": "лоджия",
    "ванной": "ванна",
    "ванную": "ванна",
    "ваннои": "ванна",
    "кухне": "кухня",
    "кухню": "кухня",
    "коридоре": "коридор",
    "коридору": "коридор",
    "туалете": "туалет",
    "кладовке": "кладовка",
    "кладовую": "кладовка",
    "полы": "пол",
    "пола": "пол",
    "окна": "окна",
    "окно": "окна",
    "окном": "окна",
    "двери": "двери",
    "дверь": "двери",
    "дверью": "двери",
}


def normalize(text: str) -> str:
    t = text.lower().replace("ё", "е")
    for ch in ".,!?;:«»\"'()-":
        t = t.replace(ch, " ")
    words = []
    for w in t.split():
        words.append(_WORD_MAP.get(w, w))
    return " ".join(words)


def match_command(text: str, commands: list[dict]) -> dict | None:
    norm = normalize(text)
    if not norm:
        return None
    # Более длинные наборы match — приоритетнее (специфичнее).
    ranked = sorted(commands, key=lambda c: -len(c.get("match") or []))
    for cmd in ranked:
        needles = [normalize(x) for x in (cmd.get("match") or [])]
        if needles and all(n in norm for n in needles):
            return cmd
    return None


def cmd_actions(cmd: dict) -> list[tuple[str, str]]:
    """Список (topic, payload) для одной или нескольких публикаций."""
    actions = cmd.get("actions")
    if isinstance(actions, list) and actions:
        out: list[tuple[str, str]] = []
        for a in actions:
            if not isinstance(a, dict):
                continue
            topic = str(a.get("topic") or "").strip()
            if not topic:
                continue
            out.append((topic, str(a.get("payload", ""))))
        return out
    topic = str(cmd.get("topic") or "").strip()
    if not topic:
        return []
    return [(topic, str(cmd.get("payload", "")))]


def cmd_topic(state_or_power: str) -> str:
    t = state_or_power.strip()
    if t.endswith("/on"):
        return t
    return t + "/on"


class MqttOut:
    def __init__(self, broker: str, dry_run: bool) -> None:
        self.dry_run = dry_run
        self.client = None
        self.host = "localhost"
        self.port = 1883
        if dry_run:
            return
        try:
            import paho.mqtt.client as mqtt
        except ImportError:
            sys.exit("нужен paho-mqtt: pip install paho-mqtt")

        host = broker
        port = 1883
        if "://" in broker:
            # tcp://host:1883
            rest = broker.split("://", 1)[1]
            if ":" in rest:
                host, port_s = rest.rsplit(":", 1)
                port = int(port_s)
            else:
                host = rest
        self.host = host
        self.port = port
        client_id = f"vosk-voice-{os.getpid()}"
        try:
            self.client = mqtt.Client(
                mqtt.CallbackAPIVersion.VERSION2,
                client_id=client_id,
            )
        except AttributeError:
            self.client = mqtt.Client(client_id=client_id)
        self.client.reconnect_delay_set(min_delay=1, max_delay=30)
        if not self._connect():
            log(f"MQTT пока недоступен ({self.host}:{self.port}) — слушаю, подключусь при команде")

    def _connect(self) -> bool:
        assert self.client is not None
        try:
            self.client.connect(self.host, self.port, 30)
            self.client.loop_start()
            log(f"MQTT подключён: {self.host}:{self.port}")
            return True
        except OSError as e:
            log(f"MQTT connect: {e}")
            return False

    def _ensure(self) -> bool:
        if self.client is None:
            return False
        if self.client.is_connected():
            return True
        log("MQTT нет связи — переподключаюсь…")
        try:
            # loop мог не стартовать при первом неудачном connect
            try:
                self.client.loop_start()
            except Exception:
                pass
            self.client.reconnect()
            return True
        except Exception as e:
            log(f"MQTT reconnect не удался: {e}")
            return False

    def publish(self, topic: str, payload: str) -> None:
        on_topic = cmd_topic(topic)
        if self.dry_run or self.client is None:
            log(f"  [dry-run] MQTT {on_topic} <- {payload!r}")
            return
        if not self._ensure():
            log(f"  (пропуск) MQTT {on_topic} <- {payload!r}")
            return
        try:
            info = self.client.publish(on_topic, payload, qos=0, retain=False)
            info.wait_for_publish(timeout=5)
            log(f"  MQTT {on_topic} <- {payload!r}")
        except RuntimeError as e:
            log(f"  MQTT ошибка: {e} — повтор…")
            if self._ensure():
                try:
                    info = self.client.publish(on_topic, payload, qos=0, retain=False)
                    info.wait_for_publish(timeout=5)
                    log(f"  MQTT {on_topic} <- {payload!r}")
                    return
                except RuntimeError as e2:
                    log(f"  (пропуск) {e2}")
            else:
                log(f"  (пропуск) MQTT {on_topic} <- {payload!r}")

    def close(self) -> None:
        if self.client is None:
            return
        self.client.loop_stop()
        self.client.disconnect()


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
                cmd = match_command(text, commands)
                if not cmd:
                    log("  (нет совпадения в phrases.json)")
                    continue
                log(f"  команда: {cmd.get('name', '?')}")
                acts = cmd_actions(cmd)
                if not acts:
                    log("  (у команды нет topic/actions)")
                    continue
                for topic, payload in acts:
                    mqtt.publish(topic, payload)
            else:
                # частичные гипотезы можно включить флагом — пока тихо
                pass


def list_commands(commands: list[dict]) -> None:
    log(f"навыков: {len(commands)}\n")
    for i, cmd in enumerate(commands, 1):
        name = cmd.get("name", "?")
        match = " + ".join(cmd.get("match") or [])
        acts = cmd_actions(cmd)
        topics = ", ".join(f"{t}/on={p!r}" for t, p in acts) if acts else "(нет topic)"
        log(f"{i:2}. {name}")
        log(f"    фраза: {match}")
        log(f"    MQTT:  {topics}")


def main() -> None:
    ap = argparse.ArgumentParser(description="Vosk → MQTT для home / Wiren Board")
    ap.add_argument("--download-model", action="store_true", help="скачать small RU модель и выйти")
    ap.add_argument("--model", type=Path, default=None, help="путь к каталогу модели Vosk")
    ap.add_argument("--phrases", type=Path, default=ROOT / "phrases.json")
    ap.add_argument("--list", action="store_true", help="показать навыки и выйти")
    ap.add_argument("--mqtt", action="store_true", help="публиковать в брокер (иначе dry-run)")
    ap.add_argument("--broker", default=os.environ.get("MQTT_BROKER", "tcp://192.168.1.9:1883"))
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
