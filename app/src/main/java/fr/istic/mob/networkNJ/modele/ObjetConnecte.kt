package fr.istic.mob.networkNJ.modele

import android.graphics.Color
import java.io.Serializable

/**
 * Représente un appareil connecté dans l'appartement.
 * Serializable permet de sauvegarder cet objet dans un fichier.
 */
data class ObjetConnecte(
    val identifiant: Int,              // Numéro unique de l'objet
    var etiquette: String,             // Nom affiché (ex: 'TV Salon')
    var positionX: Float,             // Position horizontale sur l'écran
    var positionY: Float,             // Position verticale sur l'écran
    var couleur: Int = Color.BLUE,    // Couleur de l'objet
    val rayon: Float = 40f            // Taille du cercle représentant l'objet
) : Serializable
