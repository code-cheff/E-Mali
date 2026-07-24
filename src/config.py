"""
Central configuration for the Plant Disease Identification training pipeline.

Keeping every "magic number" and path in one place is a clean-code practice:
both data loading and training import from here, so the system stays consistent
and is trivial to re-tune.
"""
from pathlib import Path

# --- Project paths -----------------------------------------------------------
BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = BASE_DIR / "dataset" / "color"  # PlantVillage RGB images, one folder per class
MODEL_DIR = BASE_DIR / "model"
MODEL_PATH = MODEL_DIR / "plant_disease_model.keras"   # full Keras model
TFLITE_PATH = MODEL_DIR / "plant_disease_model.tflite" # mobile model for Android
LABELS_PATH = MODEL_DIR / "labels.txt"                 # one class name per line
CLASS_MAP_PATH = MODEL_DIR / "class_names.json"

# Where the trained mobile model + labels are copied for the Android build.
ANDROID_ASSETS_DIR = BASE_DIR / "android-app" / "app" / "src" / "main" / "assets"

# --- Image / model hyper-parameters -----------------------------------------
IMG_SIZE = (224, 224)        # MobileNetV2 native input size
CHANNELS = 3
BATCH_SIZE = 32
EPOCHS_HEAD = 5              # epochs to train the new classifier head
EPOCHS_FINETUNE = 5         # epochs to fine-tune top conv layers
LEARNING_RATE = 1e-3
FINETUNE_LR = 1e-5
VALIDATION_SPLIT = 0.2
SEED = 42

# Ensure the directories the pipeline writes to exist.
MODEL_DIR.mkdir(parents=True, exist_ok=True)
