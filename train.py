"""
Training entry point for the Plant Disease Identification CNN.

Two-stage transfer learning, then export to TensorFlow Lite for the Android app:
  Stage 1 - freeze the MobileNetV2 base, train only the new classifier head.
  Stage 2 - unfreeze the top conv layers and fine-tune at a tiny learning rate.
  Export  - convert the trained model to .tflite (quantized) and write labels.txt,
            then copy both into android-app/app/src/main/assets/.

Run:
    python train.py

Outputs:
    model/plant_disease_model.keras    - full Keras model (for re-training)
    model/plant_disease_model.tflite   - mobile model shipped in the Android app
    model/labels.txt                   - class names, one per line
    model/training_history.png         - accuracy/loss curves
    android-app/app/src/main/assets/   - tflite + labels copied here automatically
"""
import json
import shutil

import matplotlib

matplotlib.use("Agg")  # headless backend so it works on servers / Colab
import matplotlib.pyplot as plt
import tensorflow as tf

from src import config
from src.data import load_datasets
from src.model import build_model, enable_fine_tuning


def _plot_history(histories, out_path):
    """Save accuracy & loss curves across both training stages."""
    acc, val_acc, loss, val_loss = [], [], [], []
    for h in histories:
        acc += h.history["accuracy"]
        val_acc += h.history["val_accuracy"]
        loss += h.history["loss"]
        val_loss += h.history["val_loss"]

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 4))
    ax1.plot(acc, label="train"); ax1.plot(val_acc, label="val")
    ax1.set_title("Accuracy"); ax1.set_xlabel("epoch"); ax1.legend()
    ax2.plot(loss, label="train"); ax2.plot(val_loss, label="val")
    ax2.set_title("Loss"); ax2.set_xlabel("epoch"); ax2.legend()
    fig.tight_layout()
    fig.savefig(out_path)
    print(f"[train] saved training curves -> {out_path}")


def export_tflite(model, class_names):
    """Convert the Keras model to a quantized .tflite and write labels.txt.

    Dynamic-range quantization shrinks the model ~4x and speeds up inference on
    phones, addressing the synopsis goal of an efficient mobile deployment.

    NOTE: we convert through an exported SavedModel rather than
    `from_keras_model(model)`. On TensorFlow 2.16 + Keras 3, the direct
    keras-model converter can fail in MLIR ("Failed to infer result type");
    exporting a SavedModel first freezes the variables and converts reliably.
    """
    saved_dir = config.MODEL_DIR / "saved_model"
    model.export(str(saved_dir))  # Keras 3 -> TF SavedModel with serving signature

    converter = tf.lite.TFLiteConverter.from_saved_model(str(saved_dir))
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    tflite_bytes = converter.convert()
    config.TFLITE_PATH.write_bytes(tflite_bytes)
    config.LABELS_PATH.write_text("\n".join(class_names))
    print(f"[train] exported TFLite model -> {config.TFLITE_PATH} "
          f"({len(tflite_bytes) / 1e6:.1f} MB)")

    # Copy straight into the Android app's assets so the app is build-ready.
    config.ANDROID_ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    shutil.copy(config.TFLITE_PATH, config.ANDROID_ASSETS_DIR / "plant_disease_model.tflite")
    shutil.copy(config.LABELS_PATH, config.ANDROID_ASSETS_DIR / "labels.txt")
    print(f"[train] copied model + labels into {config.ANDROID_ASSETS_DIR}")


def main():
    print("[train] loading datasets ...")
    train_ds, val_ds, class_names = load_datasets()
    num_classes = len(class_names)
    print(f"[train] {num_classes} classes: {class_names}")

    # Persist the label map now so inference never guesses the ordering.
    config.CLASS_MAP_PATH.write_text(json.dumps(class_names, indent=2))

    # --- Stage 1: train the head (frozen base) ----------------------------
    print("[train] stage 1: training classifier head (frozen base) ...")
    model = build_model(num_classes)
    h1 = model.fit(train_ds, validation_data=val_ds, epochs=config.EPOCHS_HEAD)

    # Safety checkpoint: persist after Stage 1 so a Stage-2 failure never
    # wastes the (expensive) head-training run.
    model.save(config.MODEL_PATH)
    print(f"[train] stage-1 checkpoint saved -> {config.MODEL_PATH}")

    # --- Stage 2: fine-tune (continues from the Stage-1 weights) ----------
    print("[train] stage 2: fine-tuning top conv layers ...")
    enable_fine_tuning(model)
    h2 = model.fit(train_ds, validation_data=val_ds, epochs=config.EPOCHS_FINETUNE)

    model.save(config.MODEL_PATH)
    print(f"[train] Keras model saved -> {config.MODEL_PATH}")

    _plot_history([h1, h2], config.MODEL_DIR / "training_history.png")
    export_tflite(model, class_names)
    print("[train] done. Build the Android app in android-app/ and run it.")


if __name__ == "__main__":
    main()
