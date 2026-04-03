package fr.istic.mob.networkNJ.modele

import android.graphics.Color
import java.io.Serializable

/**
 * Représente un lien entre deux objets connectés.
 * courbure : décalage du point de contrôle de la courbe de Bézier.
 * 0f = ligne droite, autre valeur = connexion courbée.
 */
data class Connexion(
    val identifiantSource: Int,        // ID de l'objet de départ
    val identifiantDestination: Int,   // ID de l'objet d'arrivée
    var etiquette: String,             // Texte affiché au milieu
    var couleur: Int = Color.BLACK,   // Couleur de la ligne
    var epaisseur: Float = 4f,        // Épaisseur de la ligne en pixels
    var courbure: Float = 0f          // Décalage pour courber la connexion
) : Serializable
