package fr.istic.mob.networkNJ.modele


import java.io.Serializable

/**
 * Le modèle principal : contient tous les objets et connexions du réseau.
 * C'est cet objet qui est sauvegardé dans un fichier.
 */
class Graph : Serializable {

    // Liste de tous les objets connectés
    val objets = mutableListOf<ObjetConnecte>()

    // Liste de toutes les connexions entre objets
    val connexions = mutableListOf<Connexion>()

    // Compteur pour générer des identifiants uniques
    private var prochainIdentifiant = 0

    /** Crée un identifiant unique à chaque appel */
    fun genererIdentifiant(): Int {
        return prochainIdentifiant++
    }

    /**
     * Ajoute un objet connecté au réseau.
     * @param objet L'objet à ajouter
     */
    fun ajouterObjet(objet: ObjetConnecte) {
        objets.add(objet)
    }

    /**
     * Supprime un objet et TOUTES ses connexions (très important !).
     * @param identifiant L'ID de l'objet à supprimer
     */
    fun supprimerObjet(identifiant: Int) {
        objets.removeIf { it.identifiant == identifiant }
        // On supprime aussi toutes les connexions liées à cet objet
        connexions.removeIf {
            it.identifiantSource == identifiant || it.identifiantDestination == identifiant
        }
    }

    /**
     * Ajoute une connexion entre deux objets.
     * Vérifie qu'on ne crée pas de doublon ni de boucle (règle du TP).
     */
    fun ajouterConnexion(connexion: Connexion): Boolean {
        // Règle 1 : Pas de boucle (objet connecté à lui-même)
        if (connexion.identifiantSource == connexion.identifiantDestination) return false

        // Règle 2 : Pas de doublon (connexion déjà existante entre ces 2 objets)
        val dejaExiste = connexions.any {
            (it.identifiantSource == connexion.identifiantSource &&
                    it.identifiantDestination == connexion.identifiantDestination) ||
                    (it.identifiantSource == connexion.identifiantDestination &&
                            it.identifiantDestination == connexion.identifiantSource)
        }
        if (dejaExiste) return false

        connexions.add(connexion)
        return true
    }

    /**
     * Supprime une connexion entre deux objets.
     */
    fun supprimerConnexion(source: Int, destination: Int) {
        connexions.removeIf {
            (it.identifiantSource == source && it.identifiantDestination == destination) ||
                    (it.identifiantSource == destination && it.identifiantDestination == source)
        }
    }

    /** Cherche un objet par son identifiant */
    fun trouverObjet(id: Int): ObjetConnecte? {
        return objets.find { it.identifiant == id }
    }

    /** Vide complètement le réseau */
    fun reinitialiser() {
        objets.clear()
        connexions.clear()
        prochainIdentifiant = 0
    }
}

