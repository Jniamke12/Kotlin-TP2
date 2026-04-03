package fr.istic.mob.networkNJ


import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import fr.istic.mob.networkNJ.databinding.ActivityMainBinding
import fr.istic.mob.networkNJ.viewmodel.ModeApplication
import fr.istic.mob.networkNJ.viewmodel.ReseauViewModel
import fr.istic.mob.networkNJ.vue.VueReseau
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var liaisonVue: ActivityMainBinding

    private val viewModel: ReseauViewModel by viewModels()

    override fun onCreate(etatSauvegarde: Bundle?) {
        super.onCreate(etatSauvegarde)

        // Initialisation du ViewBinding
        liaisonVue = ActivityMainBinding.inflate(layoutInflater)
        setContentView(liaisonVue.root)

        configurerVueReseau()
        observerViewModel()
        afficherChoixPlan()
    }


    private fun configurerVueReseau() {
        val vueReseau = liaisonVue.vueReseau

        vueReseau.ecouteurAjoutObjet = { x, y ->
            demanderEtiquette("Nom de l'objet :") { etiquette ->
                viewModel.ajouterObjet(etiquette, x, y)
            }
        }

        vueReseau.ecouteurDeplacerObjet = { id, x, y ->
            viewModel.deplacerObjet(id, x, y)
        }

        vueReseau.ecouteurConnexion = { sourceId, destinationId ->
            demanderEtiquette("Nom de la connexion :") { etiquette ->
                viewModel.ajouterConnexion(etiquette, sourceId, destinationId)
            }
        }

        vueReseau.ecouteurLongClickObjet = { id ->
            afficherMenuObjet(id)
        }

        vueReseau.ecouteurLongClickConnexion = { sourceId, destinationId ->
            afficherMenuConnexion(sourceId, destinationId)
        }

        vueReseau.ecouteurCourbureConnexion = { sourceId, destinationId, courbure ->
            viewModel.modifierCourbureConnexion(sourceId, destinationId, courbure)
        }
    }

    // OBSERVATION DU VIEWMODEL (Reactive UI)
    private fun observerViewModel() {
        // Quand le graphe change → on redessine la vue
        viewModel.graphe.observe(this) { nouveauGraphe ->
            liaisonVue.vueReseau.mettreAJourGraphe(nouveauGraphe)
        }

        // Quand le mode change → on informe la vue
        viewModel.modeActuel.observe(this) { mode ->
            liaisonVue.vueReseau.changerMode(mode)
        }

        // Quand il y a un message → on l'affiche en Toast
        viewModel.messageUtilisateur.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // MENU
    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_principal, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reinitialiser -> {
                confirmerReinitialisation()
                true
            }
            R.id.action_sauvegarder -> {
                viewModel.sauvegarderReseau()
                true
            }
            R.id.action_charger -> {
                viewModel.chargerReseau()
                true
            }
            R.id.action_envoyer -> {
                envoyerParMail()
                true
            }
            R.id.action_changer_plan -> {
                afficherChoixPlan()
                true
            }
            R.id.mode_ajout_objets -> {
                viewModel.changerMode(ModeApplication.AJOUT_OBJET)
                true
            }
            R.id.mode_ajout_connexions -> {
                viewModel.changerMode(ModeApplication.AJOUT_CONNEXION)
                true
            }
            R.id.mode_modification -> {
                viewModel.changerMode(ModeApplication.MODIFICATION)
                true
            }
            else -> super.onOptionsItemSelected(item)

        }
    }

    // DIALOGUES

    private fun demanderEtiquette(titre: String, callback: (String) -> Unit) {
        val champTexte = EditText(this)
        champTexte.hint = "Entrez un nom..."
        AlertDialog.Builder(this)
            .setTitle(titre)
            .setView(champTexte)
            .setPositiveButton("OK") { _, _ ->
                val texte = champTexte.text.toString().trim()
                if (texte.isNotEmpty()) callback(texte)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun afficherMenuObjet(identifiant: Int) {
        val options = arrayOf(
            "Supprimer l'objet",
            "Modifier l'étiquette",
            "Changer la couleur"
        )
        AlertDialog.Builder(this)
            .setTitle("Options de l'objet")
            .setItems(options) { _, choix ->
                when (choix) {
                    0 -> viewModel.supprimerObjet(identifiant)
                    1 -> demanderEtiquette("Nouvelle étiquette :") { etiquette ->
                        val objet = viewModel.graphe.value?.trouverObjet(identifiant)
                        if (objet != null) viewModel.modifierObjet(identifiant, etiquette, objet.couleur)
                    }
                    2 -> afficherChoixCouleur { couleur ->
                        val objet = viewModel.graphe.value?.trouverObjet(identifiant)
                        if (objet != null) viewModel.modifierObjet(identifiant, objet.etiquette, couleur)
                    }
                }
            }
            .show()
    }

    private fun afficherMenuConnexion(sourceId: Int, destinationId: Int) {
        val connexion = viewModel.graphe.value?.connexions?.find {
            (it.identifiantSource == sourceId && it.identifiantDestination == destinationId) ||
                    (it.identifiantSource == destinationId && it.identifiantDestination == sourceId)
        } ?: return

        val options = arrayOf(
            "Supprimer la connexion",
            "Modifier l'étiquette",
            "Changer la couleur",
            "Changer l'épaisseur"
        )
        AlertDialog.Builder(this)
            .setTitle("Options de la connexion")
            .setItems(options) { _, choix ->
                when (choix) {
                    0 -> {
                        viewModel.graphe.value?.supprimerConnexion(sourceId, destinationId)
                        viewModel.graphe.value = viewModel.graphe.value // Force refresh
                    }
                    1 -> demanderEtiquette("Nouvelle étiquette :") { etiquette ->
                        viewModel.modifierConnexion(sourceId, destinationId,
                            etiquette, connexion.couleur, connexion.epaisseur)
                    }
                    2 -> afficherChoixCouleur { couleur ->
                        viewModel.modifierConnexion(sourceId, destinationId,
                            connexion.etiquette, couleur, connexion.epaisseur)
                    }
                    3 -> afficherChoixEpaisseur(connexion.epaisseur) { epaisseur ->
                        viewModel.modifierConnexion(sourceId, destinationId,
                            connexion.etiquette, connexion.couleur, epaisseur)
                    }
                }
            }
            .show()
    }

    private fun afficherChoixCouleur(callback: (Int) -> Unit) {
        val nomsCouleurs = arrayOf("Rouge", "Vert", "Bleu", "Orange", "Cyan", "Magenta", "Noir")
        val valeursCouelurs = intArrayOf(
            Color.RED, Color.GREEN, Color.BLUE,
            0xFFFF5700.toInt(), Color.CYAN, Color.MAGENTA, Color.BLACK
        )
        AlertDialog.Builder(this)
            .setTitle("Choisir une couleur")
            .setItems(nomsCouleurs) { _, index -> callback(valeursCouelurs[index]) }
            .show()
    }

    private fun afficherChoixEpaisseur(epaisseurActuelle: Float, callback: (Float) -> Unit) {
        val options = arrayOf("Fine (2px)", "Normale (4px)", "Épaisse (8px)", "Très épaisse (12px)")
        val valeurs = floatArrayOf(2f, 4f, 8f, 12f)
        AlertDialog.Builder(this)
            .setTitle("Choisir l'épaisseur")
            .setItems(options) { _, index -> callback(valeurs[index]) }
            .show()
    }

    private fun confirmerReinitialisation() {
        AlertDialog.Builder(this)
            .setTitle("Réinitialiser")
            .setMessage("Voulez-vous vraiment effacer tout le réseau ?")
            .setPositiveButton("Oui") { _, _ -> viewModel.reinitialiserReseau() }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun afficherChoixPlan() {
        val plans = arrayOf("Plan 1 : Studio", "Plan 2 : Appartement 2 pièces", "Pas de plan")
        AlertDialog.Builder(this)
            .setTitle("Choisissez un plan d'appartement")
            .setItems(plans) { _, index ->
                val planBitmap = when (index) {
                    0 -> {
                        try {
                            val flux = resources.openRawResource(R.raw.plan_appart_5)
                            android.graphics.BitmapFactory.decodeStream(flux)
                        } catch (e: Exception) { null }
                    }
                    1 -> {
                        try {
                            val flux = resources.openRawResource(R.raw.plan_appart_4)
                            android.graphics.BitmapFactory.decodeStream(flux)
                        } catch (e: Exception) { null }
                    }
                    else -> null
                }
                liaisonVue.vueReseau.changerPlan(planBitmap)
            }
            .show()
    }


    // ENVOI PAR MAIL

    private fun envoyerParMail() {
        try {
            val captureEcran = liaisonVue.vueReseau.capturerEcran()

            // Sauvegarder la capture dans un fichier temporaire
            val fichierTemp = File(cacheDir, "reseau_capture.png")
            FileOutputStream(fichierTemp).use { sortie ->
                captureEcran.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, sortie)
            }

            val uriImage = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                fichierTemp
            )

            val intentMail = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_SUBJECT, "Mon réseau domestique")
                putExtra(Intent.EXTRA_TEXT, "Voici mon réseau d'objets connectés.")
                putExtra(Intent.EXTRA_STREAM, uriImage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intentMail, "Envoyer via..."))

        } catch (e: Exception) {
            Toast.makeText(this, "Erreur lors de l'envoi : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

