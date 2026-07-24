"""
Generate a tiny SYNTHETIC dataset so you can run train.py -> app.py end-to-end
without first downloading the multi-gigabyte PlantVillage dataset.

This is for smoke-testing the pipeline only. For real accuracy, replace the
`dataset/` folder with the actual PlantVillage images (see README).

    python make_sample_dataset.py
"""
import numpy as np
from PIL import Image

from src import config

# A few representative classes in PlantVillage naming style.
CLASSES = {
    "Tomato___healthy": (60, 160, 60),
    "Tomato___Late_blight": (110, 90, 40),
    "Potato___Early_blight": (150, 130, 50),
    "Pepper__bell___healthy": (40, 140, 80),
}
IMAGES_PER_CLASS = 40


def main():
    rng = np.random.default_rng(config.SEED)
    for cls, base_rgb in CLASSES.items():
        out_dir = config.DATA_DIR / cls
        out_dir.mkdir(parents=True, exist_ok=True)
        for i in range(IMAGES_PER_CLASS):
            # Solid base colour + noise so each class is separable but noisy.
            noise = rng.integers(-25, 25, size=(*config.IMG_SIZE, 3))
            arr = np.clip(np.array(base_rgb) + noise, 0, 255).astype("uint8")
            Image.fromarray(arr).save(out_dir / f"{cls}_{i:03d}.jpg")
        print(f"[sample] wrote {IMAGES_PER_CLASS} images -> {out_dir}")
    print("\nDone. Now run:  python train.py")


if __name__ == "__main__":
    main()
