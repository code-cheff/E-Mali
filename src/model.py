"""
CNN model definition using transfer learning.

The synopsis explicitly mentions using pre-trained models (MobileNet / ResNet /
VGG16) with transfer learning to "improve efficiency and accuracy" and reduce
training cost. We use MobileNetV2: it is lightweight, accurate, and small enough
to later deploy on mobile / low-resource hardware -- exactly the "scalable model
accessible to farmers" goal stated in the objectives.
"""
import tensorflow as tf
from tensorflow.keras import layers, models
from tensorflow.keras.applications import MobileNetV2

from src import config


def get_base(model: tf.keras.Model) -> tf.keras.Model:
    """Return the nested MobileNetV2 base inside our model.

    We locate it by type (it is the only nested Keras Model in the graph) rather
    than by name: in Keras 3, renaming a nested model is not reliable.
    """
    for layer in model.layers:
        if isinstance(layer, tf.keras.Model):
            return layer
    raise ValueError("Convolutional base layer not found in model.")


def build_model(num_classes: int) -> tf.keras.Model:
    """Build a MobileNetV2-based classifier with the convolutional base FROZEN.

    The returned model is ready for Stage-1 training (only the new head learns).
    Call :func:`enable_fine_tuning` afterwards for Stage-2.
    """
    # 1. Pre-trained convolutional base (weights learned on ImageNet).
    base = MobileNetV2(
        input_shape=(*config.IMG_SIZE, config.CHANNELS),
        include_top=False,          # drop ImageNet's 1000-class head
        weights="imagenet",
    )
    base.trainable = False          # Stage 1: freeze the base

    # 2. New classifier head tailored to our plant-disease classes.
    inputs = layers.Input(shape=(*config.IMG_SIZE, config.CHANNELS))
    # MobileNetV2 expects pixels scaled to [-1, 1] (done inside the graph).
    x = tf.keras.applications.mobilenet_v2.preprocess_input(inputs)
    x = base(x, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dropout(0.3)(x)                       # regularisation vs overfitting
    outputs = layers.Dense(num_classes, activation="softmax")(x)

    model = models.Model(inputs, outputs, name="plant_disease_mobilenetv2")
    _compile(model, config.LEARNING_RATE)
    return model


def enable_fine_tuning(model: tf.keras.Model, num_unfrozen: int = 30) -> tf.keras.Model:
    """Unfreeze the top ``num_unfrozen`` conv layers of the base for Stage-2.

    This keeps the head weights learned in Stage-1 and merely makes the top of
    the backbone trainable, then recompiles with a much smaller learning rate so
    the pre-trained features are refined, not destroyed.
    """
    base = get_base(model)
    base.trainable = True
    for layer in base.layers[:-num_unfrozen]:
        layer.trainable = False     # keep early generic filters frozen
    _compile(model, config.FINETUNE_LR)
    return model


def _compile(model: tf.keras.Model, lr: float) -> None:
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=lr),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )
