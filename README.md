# 🌿 Plant Disease Identification — Android App

An Android app that identifies plant diseases from a photo of a leaf, running a
**Convolutional Neural Network (transfer learning, MobileNetV2)** **on-device**
with **TensorFlow Lite** — no internet connection required at prediction time.

> BCA Final Project (DCA3202) — *Plant Diseases Identification*
> Stack as per synopsis: Python · TensorFlow/Keras → **TensorFlow Lite** · CNN ·
> OpenCV/PIL · NumPy/Pandas · Matplotlib · **Android (Kotlin)** · Git

---

## ✨ Features

1. **Capture or pick** a leaf photo (**Camera** or **Gallery**).
2. **On-device CNN inference** via TensorFlow Lite — fully offline, no internet needed.
3. **Rich result card:** predicted disease, crop name, color-coded **Healthy / Diseased** status chip, and an animated confidence bar.
4. **Disease knowledge base:** a plain-language **description** and **recommended treatment/action** for each disease (bundled offline).
5. **Top-3 possibilities** shown as visual confidence bars.
6. **On-device inference time** displayed (e.g. "Analyzed on-device in 38 ms").
7. **Low-confidence warning** prompting a clearer photo.
8. **Share** the diagnosis as text to any app.
9. **Session scan history** of the last 5 scans with thumbnails.
10. **Material 3 UI** — app bar, cards, tonal/outlined buttons, progress indicators; background inference keeps the UI responsive.

There are **two parts**:

| Part | Where | Language | Role |
|------|-------|----------|------|
| **Model training** | project root (`train.py`, `src/`) | Python | Train the CNN once, export a `.tflite` model |
| **Android app** | `android-app/` | Kotlin | Ship the `.tflite` model and run it on the phone |

---

## 📁 Project structure

```
plantDisease/
├── train.py                 # Trains the CNN and exports model.tflite + labels.txt
├── make_sample_dataset.py   # Tiny synthetic dataset for smoke-testing the pipeline
├── requirements.txt         # Python (training) dependencies
├── src/
│   ├── config.py            # Paths & hyper-parameters
│   ├── data.py              # Dataset loading + augmentation
│   └── model.py             # MobileNetV2 CNN definition
├── model/                   # Trained artifacts (.keras, .tflite, labels, curves)
└── android-app/             # ← the Android Studio project
    ├── settings.gradle.kts
    ├── build.gradle.kts
    └── app/
        ├── build.gradle.kts
        └── src/main/
            ├── AndroidManifest.xml
            ├── assets/                 # .tflite model + labels.txt live here
            ├── java/com/sambhav/plantdisease/
            │   ├── MainActivity.kt     # UI: pick image → classify → show result
            │   └── PlantClassifier.kt  # TFLite inference wrapper
            └── res/                    # layout, colors, strings, theme, icon
```

---

## 🚀 Part 1 — Train & export the model (Python)

> Use **Python 3.10–3.12** (TensorFlow 2.16 does not support 3.13 yet).

```bash
cd plantDisease
python3.12 -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

**Get a dataset** (one folder per class, PlantVillage style):

- **Real data (recommended):** download the
  [PlantVillage dataset](https://www.kaggle.com/datasets/abdallahalidev/plantvillage-dataset)
  into `dataset/` as `dataset/Tomato___healthy/`, `dataset/Tomato___Late_blight/`, …
- **Quick smoke test (no download):** `python make_sample_dataset.py`

**Train** (also exports the mobile model automatically):
```bash
python train.py
```
This produces `model/plant_disease_model.tflite` and `model/labels.txt`, and
**copies both into `android-app/app/src/main/assets/`** so the app is build-ready.

---

## 🤖 Optional — AI treatment advice via OpenRouter

By default the app shows **bundled offline** treatment advice for every disease.
You can optionally have it fetch **AI-tailored** advice from
[OpenRouter](https://openrouter.ai) (an OpenAI-compatible LLM API):

1. Get an API key at https://openrouter.ai/keys
2. Open `android-app/local.properties` (Android Studio created it; it is gitignored)
   and add two lines (see `local.properties.sample`):
   ```properties
   OPENROUTER_API_KEY=sk-or-v1-your-key-here
   OPENROUTER_MODEL=nvidia/nemotron-3-ultra-550b-a55b:free
   ```
   The default model is **free** (`:free` = no usage cost). For higher-quality
   output you can switch to a paid slug like `anthropic/claude-3.5-sonnet` —
   browse all options at https://openrouter.ai/models.
3. Rebuild. After a diagnosis, the app shows offline advice instantly, then
   replaces it with AI advice when it arrives ("✦ AI advice via OpenRouter").

If no key is set, or the device is offline, the app silently uses the bundled
advice — detection itself is always on-device.

> ⚠️ **Security note:** embedding an API key in an APK is fine for a demo/college
> project, but the key can be extracted from a distributed app. For production,
> route the call through your own backend instead of calling OpenRouter directly.

## 📱 Part 2 — Build & run the Android app

1. Install **Android Studio** (Hedgehog or newer).
2. **File → Open** → select the `android-app/` folder. Android Studio downloads
   the Gradle wrapper and dependencies on first sync.
3. Confirm `app/src/main/assets/` contains `plant_disease_model.tflite` and
   `labels.txt` (created in Part 1).
4. Plug in an Android phone (USB debugging on) or start an emulator.
5. Click **▶ Run**. The app installs and launches.
6. Tap **Camera** or **Gallery**, choose a leaf image, and see the prediction.

> Build from the command line instead? After the first Android Studio sync (which
> generates `gradlew`): `cd android-app && ./gradlew assembleDebug`.

---

## 🧠 How it works

- **Transfer learning:** MobileNetV2 (pre-trained on ImageNet) supplies strong
  generic visual features; we replace its head with one sized to the disease classes.
- **Two-stage training:** train the new head with the base frozen, then fine-tune
  the top conv layers at a tiny learning rate.
- **TensorFlow Lite + quantization:** the Keras model is converted to a quantized
  `.tflite` (~4× smaller, faster), making on-device inference practical.
- **Preprocessing parity:** MobileNetV2's pixel scaling lives *inside* the model
  graph, so the phone only resizes the image — training and inference stay identical.
- **Confidence gating:** predictions below 60% (`PlantClassifier.CONFIDENCE_THRESHOLD`)
  are flagged uncertain.

---

## ⚠️ Notes & limitations

- The synthetic dataset is for pipeline testing only — train on PlantVillage for real accuracy.
- `TakePicturePreview` returns a thumbnail (fine for the MVP); for production, capture
  a full-resolution image via `FileProvider`.
- Accuracy depends on image quality; works best on a single, well-lit leaf on a plain background.
- Tune epochs / learning rates / image size in `src/config.py`.
