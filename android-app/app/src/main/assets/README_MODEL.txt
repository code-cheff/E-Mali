PLACE THE TRAINED MODEL HERE
============================

This folder must contain:
    plant_disease_model.tflite   <-- the trained model (NOT included; it's large)
    labels.txt                   <-- class names, one per line (a sample is provided)

How to generate them:
    1. From the project root, train the model:   python train.py
    2. train.py automatically copies the freshly trained
       plant_disease_model.tflite and labels.txt INTO this assets folder.
    3. Rebuild/run the Android app in Android Studio.

Until the .tflite file exists here, the app will launch but show
"Model not found" when you try to classify an image.
