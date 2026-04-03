package fr.istic.mob.networkNJ.viewmodel


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import fr.istic.mob.networkNJ.modele.Connexion
import fr.istic.mob.networkNJ.modele.Graph
import fr.istic.mob.networkNJ.modele.ObjetConnecte
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/** Les modes disponibles dans l'application */
enum class ModeApplication {
    AJOUT_OBJET,        // L'utilisateur peut ajouter des objets
    AJOUT_CONNEXION,    // L'utilisateur peut créer des liens
    MODIFICATION        // L'utilisateur peut modifier/supprimer
}


class ReseauViewModel(application: Application) : AndroidViewModel(application) {

    val graphe = MutableLiveData(Graph())

    val modeActuel = MutableLiveData(ModeApplication.AJOUT_OBJET)

    val planSelectionne = MutableLiveData(0)

    val messageUtilisateur = MutableLiveData<String>()

    private val nomFichierSauvegarde = "reseau_sauvegarde.dat"

    fun changerMode(nouveauMode: ModeApplication) {
        modeActuel.value = nouveauMode
    }

    fun ajouterObjet(etiquette: String, x: Float, y: Float) {
        val grapheActuel = graphe.value ?: return
        val nouvelObjet = ObjetConnecte(
            identifiant = grapheActuel.genererIdentifiant(),
            etiquette = etiquette,
            positionX = x,
            positionY = y
        )
        grapheActuel.ajouterObjet(nouvelObjet)
        // On notifie la vue qu'il faut se redessiner
        graphe.value = grapheActuel
    }

    fun deplacerObjet(identifiant: Int, nouvelleX: Float, nouvelleY: Float) {
        val grapheActuel = graphe.value ?: return
        val objet = grapheActuel.trouverObjet(identifiant) ?: return
        objet.positionX = nouvelleX
        objet.positionY = nouvelleY
        graphe.value = grapheActuel  // Notifie la vue
    }

    fun ajouterConnexion(etiquette: String, sourceId: Int, destinationId: Int) {
        val grapheActuel = graphe.value ?: return
        val nouvelleConnexion = Connexion(
            identifiantSource = sourceId,
            identifiantDestination = destinationId,
            etiquette = etiquette
        )
        val succes = grapheActuel.ajouterConnexion(nouvelleConnexion)
        if (!succes) {
            messageUtilisateur.value = "Connexion impossible (doublon ou boucle)"
        }
        graphe.value = grapheActuel
    }

    fun modifierObjet(identifiant: Int, etiquette: String, couleur: Int) {
        val grapheActuel = graphe.value ?: return
        val objet = grapheActuel.trouverObjet(identifiant) ?: return
        objet.etiquette = etiquette
        objet.couleur = couleur
        graphe.value = grapheActuel
    }

    fun supprimerObjet(identifiant: Int) {
        val grapheActuel = graphe.value ?: return
        grapheActuel.supprimerObjet(identifiant)
        graphe.value = grapheActuel
    }

    fun modifierConnexion(sourceId: Int, destinationId: Int,
                          etiquette: String, couleur: Int, epaisseur: Float) {
        val grapheActuel = graphe.value ?: return
        val connexion = grapheActuel.connexions.find {
            (it.identifiantSource == sourceId && it.identifiantDestination == destinationId) ||
                    (it.identifiantSource == destinationId && it.identifiantDestination == sourceId)
        } ?: return
        connexion.etiquette = etiquette
        connexion.couleur = couleur
        connexion.epaisseur = epaisseur
        graphe.value = grapheActuel
    }

    fun reinitialiserReseau() {
        graphe.value?.reinitialiser()
        graphe.value = graphe.value  // Force la notification
    }

    fun sauvegarderReseau() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contexte = getApplication<Application>()
                val flux = contexte.openFileOutput(nomFichierSauvegarde, 0) // MODE_PRIVATE = 0
                ObjectOutputStream(flux).use { it.writeObject(graphe.value) }
                withContext(Dispatchers.Main) {
                    messageUtilisateur.value = "Réseau sauvegardé avec succès !"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messageUtilisateur.value = "Erreur lors de la sauvegarde : ${e.message}"
                }
            }
        }
    }

    fun chargerReseau() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val contexte = getApplication<Application>()
                val flux = contexte.openFileInput(nomFichierSauvegarde)
                val grapheCharge = ObjectInputStream(flux).use { it.readObject() as Graph }
                withContext(Dispatchers.Main) {
                    graphe.value = grapheCharge
                    messageUtilisateur.value = "Réseau chargé avec succès !"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messageUtilisateur.value = "Aucune sauvegarde trouvée."
                }
            }
        }
    }

    fun modifierCourbureConnexion(sourceId: Int, destinationId: Int, courbure: Float) {
        val grapheActuel = graphe.value ?: return
        val connexion = grapheActuel.connexions.find {
            (it.identifiantSource == sourceId && it.identifiantDestination == destinationId) ||
                    (it.identifiantSource == destinationId && it.identifiantDestination == sourceId)
        } ?: return
        connexion.courbure = courbure
        graphe.value = grapheActuel
    }
}
