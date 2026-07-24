"""
Dataset loading & augmentation.

Expects a PlantVillage-style directory layout under ``dataset/``::

    dataset/
        Tomato___healthy/
            img001.jpg ...
        Tomato___Late_blight/
            img101.jpg ...
        Potato___Early_blight/
            ...

Each sub-folder name is the class label. ``tf.keras`` infers the labels from the
folder names, so adding a new disease is as simple as dropping in a new folder.
"""
import tensorflow as tf

from src import config


def _augment() -> tf.keras.Sequential:
    """Data-augmentation pipeline (synopsis: rotation, flipping, zooming).

    Augmentation runs on-GPU as part of the model graph and only during
    training, increasing effective dataset diversity to fight overfitting.
    """
    return tf.keras.Sequential(
        [
            tf.keras.layers.RandomFlip("horizontal"),
            tf.keras.layers.RandomRotation(0.15),
            tf.keras.layers.RandomZoom(0.15),
            tf.keras.layers.RandomContrast(0.1),
        ],
        name="augmentation",
    )


def load_datasets():
    """Return (train_ds, val_ds, class_names) ready for ``model.fit``."""
    common = dict(
        directory=config.DATA_DIR,
        labels="inferred",
        label_mode="categorical",
        image_size=config.IMG_SIZE,
        batch_size=config.BATCH_SIZE,
        seed=config.SEED,
        validation_split=config.VALIDATION_SPLIT,
    )
    train_ds = tf.keras.utils.image_dataset_from_directory(subset="training", **common)
    val_ds = tf.keras.utils.image_dataset_from_directory(subset="validation", **common)

    class_names = train_ds.class_names  # capture before mapping transforms the ds

    augment = _augment()
    autotune = tf.data.AUTOTUNE
    train_ds = (
        train_ds.map(lambda x, y: (augment(x, training=True), y), num_parallel_calls=autotune)
        .prefetch(autotune)
    )
    val_ds = val_ds.prefetch(autotune)
    return train_ds, val_ds, class_names
