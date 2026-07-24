package com.sambhav.plantdisease

/**
 * Offline knowledge base mapping each PlantVillage class to a human-readable
 * crop / condition, a short description, and practical treatment advice.
 *
 * This turns a raw model label (e.g. "Tomato___Late_blight") into actionable
 * guidance for the user — addressing the project goal of providing farmers with
 * recommendations, not just a diagnosis. All content is bundled in the app, so
 * it works fully offline.
 */
object DiseaseInfo {

    data class Info(
        val crop: String,
        val condition: String,
        val healthy: Boolean,
        val description: String,
        val treatment: String,
    )

    // raw label -> (description, treatment). Healthy classes are handled generically.
    private val DETAILS: Map<String, Pair<String, String>> = mapOf(
        "Apple___Apple_scab" to (
            "A fungal disease causing olive-green to black velvety spots on leaves and fruit, often leading to leaf drop." to
            "Apply fungicides (e.g. captan or myclobutanil) at bud break; rake and destroy fallen leaves to reduce spores."),
        "Apple___Black_rot" to (
            "Fungal infection producing 'frog-eye' leaf spots and rotting fruit with concentric rings." to
            "Prune out cankers and mummified fruit, improve air circulation, and apply protective fungicides during the season."),
        "Apple___Cedar_apple_rust" to (
            "A rust fungus causing bright orange-yellow spots on leaves; needs both apple and cedar hosts to spread." to
            "Remove nearby junipers/cedars where possible and apply fungicides from pink bud through early summer."),
        "Cherry_(including_sour)___Powdery_mildew" to (
            "White powdery fungal growth on leaves and shoots that stunts growth and distorts foliage." to
            "Apply sulfur or potassium-bicarbonate fungicides, prune for airflow, and avoid excess nitrogen fertiliser."),
        "Corn_(maize)___Cercospora_leaf_spot Gray_leaf_spot" to (
            "Fungal disease causing rectangular grey-to-tan lesions that run parallel to leaf veins." to
            "Rotate crops, use resistant hybrids, manage residue by tillage, and apply foliar fungicides if severe."),
        "Corn_(maize)___Common_rust_" to (
            "Reddish-brown raised pustules on both leaf surfaces caused by the fungus Puccinia sorghi." to
            "Plant resistant hybrids; apply fungicides early if infection is heavy on susceptible varieties."),
        "Corn_(maize)___Northern_Leaf_Blight" to (
            "Long cigar-shaped grey-green lesions that turn tan, reducing photosynthesis and yield." to
            "Use resistant hybrids, rotate crops, bury residue, and apply fungicides at early lesion onset."),
        "Grape___Black_rot" to (
            "Fungal disease causing brown circular leaf spots and shrivelled black 'mummy' berries." to
            "Remove mummified fruit and infected canes, improve canopy airflow, and apply fungicides from bud break."),
        "Grape___Esca_(Black_Measles)" to (
            "A complex trunk disease showing tiger-stripe leaf patterns and spotted ('measles') berries." to
            "Prune out and destroy infected wood, protect pruning wounds, and avoid stressing vines through drought."),
        "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)" to (
            "Fungal leaf spot producing irregular dark-brown lesions that can cause early defoliation." to
            "Improve air circulation, remove infected leaves, and apply protective fungicides in warm humid weather."),
        "Orange___Haunglongbing_(Citrus_greening)" to (
            "A serious bacterial disease (spread by psyllids) causing blotchy mottled leaves and bitter, misshapen fruit." to
            "There is no cure; remove infected trees, control psyllid insects, and plant certified disease-free stock."),
        "Peach___Bacterial_spot" to (
            "Bacterial infection causing small dark angular leaf spots, shot-holes, and cracked fruit lesions." to
            "Plant resistant cultivars, apply copper-based sprays during dormancy, and avoid overhead irrigation."),
        "Pepper,_bell___Bacterial_spot" to (
            "Bacterial disease producing water-soaked spots that turn brown with yellow halos on leaves and fruit." to
            "Use certified clean seed, rotate crops, apply copper sprays, and avoid working among wet plants."),
        "Potato___Early_blight" to (
            "Fungal disease with dark concentric 'target' spots on older leaves, reducing tuber yield." to
            "Rotate crops, ensure good nutrition, remove infected debris, and apply fungicides (chlorothalonil/mancozeb)."),
        "Potato___Late_blight" to (
            "An aggressive disease (cause of the Irish famine) with greasy dark blotches that rapidly kill foliage." to
            "Destroy infected plants immediately, use resistant varieties, and apply preventive fungicides in cool wet weather."),
        "Squash___Powdery_mildew" to (
            "White powdery fungal patches on leaves and stems that cause yellowing and early decline." to
            "Choose resistant varieties, space plants for airflow, and spray sulfur or potassium-bicarbonate fungicides."),
        "Strawberry___Leaf_scorch" to (
            "Fungal disease causing many small dark-purple spots that merge, giving leaves a scorched look." to
            "Remove old infected leaves after harvest, improve airflow, and apply fungicides at early growth stages."),
        "Tomato___Bacterial_spot" to (
            "Bacterial disease with small dark greasy spots on leaves and scabby spots on fruit." to
            "Use disease-free seed, rotate crops, apply copper-based sprays, and avoid overhead watering."),
        "Tomato___Early_blight" to (
            "Fungal disease showing dark concentric-ring 'bull's-eye' spots on lower leaves first." to
            "Mulch to prevent soil splash, remove infected leaves, rotate crops, and apply fungicides preventively."),
        "Tomato___Late_blight" to (
            "A fast-spreading disease with large greasy grey-green blotches that can destroy plants in days." to
            "Remove and destroy infected plants, avoid leaf wetness, and apply protective fungicides in cool humid conditions."),
        "Tomato___Leaf_Mold" to (
            "Fungal disease (common in greenhouses) with pale yellow spots above and olive mould beneath leaves." to
            "Increase ventilation, reduce humidity, space plants, and apply fungicides if the disease persists."),
        "Tomato___Septoria_leaf_spot" to (
            "Numerous small circular spots with dark borders and grey centres, starting on lower leaves." to
            "Remove infected foliage, avoid overhead watering, mulch the soil, and apply fungicides regularly."),
        "Tomato___Spider_mites Two-spotted_spider_mite" to (
            "Tiny mites that cause fine yellow stippling and webbing, leading to bronzed, dried leaves." to
            "Spray plants with water, introduce predatory mites, and use insecticidal soap or miticides if severe."),
        "Tomato___Target_Spot" to (
            "Fungal disease producing brown spots with concentric rings on leaves, stems, and fruit." to
            "Improve airflow, remove infected debris, rotate crops, and apply fungicides at first symptoms."),
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus" to (
            "A whitefly-transmitted virus causing upward leaf curling, yellowing, and severely stunted plants." to
            "Control whiteflies, use resistant varieties and reflective mulch, and remove infected plants promptly."),
        "Tomato___Tomato_mosaic_virus" to (
            "A virus causing mottled light/dark green leaves, distortion, and reduced fruit quality." to
            "Use resistant seed, disinfect tools and hands, remove infected plants, and control aphid carriers."),
    )

    /** Disease-only display label, e.g. "Late blight" or "Healthy" (no crop name). */
    fun conditionLabel(rawLabel: String): String {
        val info = lookup(rawLabel)
        return if (info.healthy) "Healthy" else info.condition
    }

    /** Convert a raw model label into structured, displayable information. */
    fun lookup(rawLabel: String): Info {
        val parts = rawLabel.split("___")
        val crop = parts[0].replace("_", " ").replace(",", "").trim()
        val conditionRaw = if (parts.size > 1) parts[1] else ""
        val healthy = conditionRaw.equals("healthy", ignoreCase = true)
        val condition = conditionRaw.replace("_", " ").trim()

        val details = DETAILS[rawLabel]
        val description: String
        val treatment: String
        when {
            details != null -> { description = details.first; treatment = details.second }
            healthy -> {
                description = "The leaf appears healthy, with no visible signs of disease."
                treatment = "No treatment needed. Maintain good practices: proper watering, " +
                        "balanced nutrition, adequate spacing, and regular monitoring."
            }
            else -> {
                description = "Detailed information for this condition is not available offline."
                treatment = "Consult a local agricultural expert for accurate guidance."
            }
        }
        return Info(crop, if (healthy) "Healthy" else condition, healthy, description, treatment)
    }
}
