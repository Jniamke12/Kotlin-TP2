package fr.istic.mob.networkNJ.vue

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import fr.istic.mob.networkNJ.modele.Connexion
import fr.istic.mob.networkNJ.modele.Graph
import fr.istic.mob.networkNJ.modele.ObjetConnecte
import fr.istic.mob.networkNJ.viewmodel.ModeApplication


class VueReseau @JvmOverloads constructor(
    contexte: Context,
    attributs: AttributeSet? = null
) : View(contexte, attributs) {

    // === DONNÉES ===
    private var graphe: Graph = Graph()
    private var modeActuel: ModeApplication = ModeApplication.AJOUT_OBJET
    private var planAppartement: Bitmap? = null

    // === CALLBACKS : La vue informe la MainActivity des actions de l'utilisateur ===
    var ecouteurAjoutObjet: ((x: Float, y: Float) -> Unit)? = null
    var ecouteurDeplacerObjet: ((id: Int, x: Float, y: Float) -> Unit)? = null
    var ecouteurConnexion: ((sourceId: Int, destinationId: Int) -> Unit)? = null
    var ecouteurLongClickObjet: ((id: Int) -> Unit)? = null
    var ecouteurLongClickConnexion: ((sourceId: Int, destinationId: Int) -> Unit)? = null
    var ecouteurCourbureConnexion: ((sourceId: Int, destinationId: Int, courbure: Float) -> Unit)? = null

    // === PINCEAUX (Paint) ===
    // Un Paint configure comment on dessine (couleur, épaisseur, police...)
    private val peintureObjet = Paint(Paint.ANTI_ALIAS_FLAG)
    private val peintureTexte = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private val peintureConnexion = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val peintureTexteConnexion = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    private val peintureConnexionTemp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f) // Ligne pointillée
    }

    init {
        isClickable = true
        isLongClickable = true
        isFocusable = true
    }

    // === ÉTAT DES TOUCHES ===
    private var objetDeplace: ObjetConnecte? = null        // Objet en cours de déplacement
    private var sourceConnexion: ObjetConnecte? = null      // 1er objet pour une connexion
    private var xDoigt: Float = 0f                          // Position actuelle du doigt
    private var yDoigt: Float = 0f
    private var connexionCourbeeEnCours: Connexion? = null  // Connexion dont on change la courbure
    private var longClicEnCours = false


    private val detecteurGestes = GestureDetector(
        contexte,
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onLongPress(e: MotionEvent) {
                traiterLongClic(e.x, e.y)
            }
        }
    )


    override fun onDraw(toile: Canvas) {
        super.onDraw(toile)

        planAppartement?.let { plan ->
            val rectSource = android.graphics.Rect(0, 0, plan.width, plan.height)
            val rectDest = android.graphics.RectF(
                0f,
                0f,
                width.toFloat(),
                height.toFloat()  // ← la vue elle-même ne commence qu'après la toolbar
            )
            toile.drawBitmap(plan, rectSource, rectDest, null)
        }

        for (connexion in graphe.connexions) {
            dessinerConnexion(toile, connexion)
        }

        if (modeActuel == ModeApplication.AJOUT_CONNEXION && sourceConnexion != null) {
            val cheminTemp = Path()
            cheminTemp.moveTo(sourceConnexion!!.positionX, sourceConnexion!!.positionY)
            cheminTemp.lineTo(xDoigt, yDoigt)
            toile.drawPath(cheminTemp, peintureConnexionTemp)
        }

        for (objet in graphe.objets) {
            dessinerObjet(toile, objet)
        }
    }


    private fun dessinerObjet(toile: Canvas, objet: ObjetConnecte) {
        // Dessine un cercle coloré
        peintureObjet.color = objet.couleur
        toile.drawCircle(objet.positionX, objet.positionY, objet.rayon, peintureObjet)

        // Contour noir du cercle
        peintureObjet.color = Color.BLACK
        peintureObjet.style = Paint.Style.STROKE
        peintureObjet.strokeWidth = 2f
        toile.drawCircle(objet.positionX, objet.positionY, objet.rayon, peintureObjet)
        peintureObjet.style = Paint.Style.FILL

        // Étiquette affichée à côté de l'objet
        peintureTexte.color = Color.BLACK
        peintureTexte.textSize = 24f
        toile.drawText(
            objet.etiquette,
            objet.positionX,
            objet.positionY + objet.rayon + 30f, // Un peu en dessous
            peintureTexte
        )
    }


    private fun dessinerConnexion(toile: Canvas, connexion: Connexion) {
        val source = graphe.trouverObjet(connexion.identifiantSource) ?: return
        val destination = graphe.trouverObjet(connexion.identifiantDestination) ?: return

        peintureConnexion.color = connexion.couleur
        peintureConnexion.strokeWidth = connexion.epaisseur

        val milieuxX = (source.positionX + destination.positionX) / 2f
        val milieuxY = (source.positionY + destination.positionY) / 2f

        val dx = destination.positionX - source.positionX
        val dy = destination.positionY - source.positionY
        val longueur = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val perpX = if (longueur > 0) -dy / longueur else 0f
        val perpY = if (longueur > 0) dx / longueur else 0f

        val ctrlX = milieuxX + perpX * connexion.courbure
        val ctrlY = milieuxY + perpY * connexion.courbure

        val chemin = Path()
        chemin.moveTo(source.positionX, source.positionY)
        chemin.quadTo(ctrlX, ctrlY, destination.positionX, destination.positionY)
        toile.drawPath(chemin, peintureConnexion)

        // Position de l'étiquette : milieu exact de la courbe
        // PathMeasure permet de trouver le point au milieu du chemin
        val mesure = PathMeasure(chemin, false)
        val positionMilieu = FloatArray(2)
        mesure.getPosTan(mesure.length / 2f, positionMilieu, null)

        toile.drawText(
            connexion.etiquette,
            positionMilieu[0] + 15f, // Décalage horizontal
            positionMilieu[1] - 10f, // Légèrement au-dessus
            peintureTexteConnexion
        )
    }

    override fun onTouchEvent(evenement: MotionEvent): Boolean {
        detecteurGestes.onTouchEvent(evenement)

        when (evenement.action) {
            MotionEvent.ACTION_DOWN -> {
                longClicEnCours = false
                traiterPressionDoigt(evenement.x, evenement.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!longClicEnCours) traiterDeplacementDoigt(evenement.x, evenement.y)
            }
            MotionEvent.ACTION_UP -> {
                if (!longClicEnCours) traiterLacherDoigt(evenement.x, evenement.y)
                longClicEnCours = false
            }
        }
        return true
    }

    private fun traiterPressionDoigt(x: Float, y: Float) {
        xDoigt = x; yDoigt = y
        when (modeActuel) {
            ModeApplication.AJOUT_OBJET -> {
            }
            ModeApplication.AJOUT_CONNEXION -> {
                sourceConnexion = trouverObjetSous(x, y)
            }
            ModeApplication.MODIFICATION -> {
                objetDeplace = trouverObjetSous(x, y)
                if (objetDeplace == null) {
                    connexionCourbeeEnCours = trouverConnexionSous(x, y)
                }
            }
        }
    }

    private fun traiterDeplacementDoigt(x: Float, y: Float) {
        xDoigt = x; yDoigt = y
        when (modeActuel) {
            ModeApplication.AJOUT_CONNEXION -> {
                // Redessine la connexion temporaire
                invalidate() // Demande un redessin
            }
            ModeApplication.MODIFICATION -> {
                objetDeplace?.let {
                    ecouteurDeplacerObjet?.invoke(it.identifiant, x, y)
                }
                connexionCourbeeEnCours?.let { conn ->
                    val src = graphe.trouverObjet(conn.identifiantSource) ?: return
                    val dst = graphe.trouverObjet(conn.identifiantDestination) ?: return
                    // Calcul de la distance perpendiculaire (voir cahier des charges)
                    val courbure = calculerCourbure(src, dst, x, y)
                    ecouteurCourbureConnexion?.invoke(
                        conn.identifiantSource, conn.identifiantDestination, courbure
                    )
                }
            }
            else -> {}
        }
    }

    private fun traiterLacherDoigt(x: Float, y: Float) {
        when (modeActuel) {
            ModeApplication.AJOUT_CONNEXION -> {
                val destination = trouverObjetSous(x, y)
                if (sourceConnexion != null && destination != null) {
                    // On prévient la MainActivity qu'une connexion a été créée
                    ecouteurConnexion?.invoke(sourceConnexion!!.identifiant, destination.identifiant)
                }
                sourceConnexion = null
                invalidate()
            }
            ModeApplication.MODIFICATION -> {
                objetDeplace = null
                connexionCourbeeEnCours = null
            }
            else -> {}
        }
    }

    private fun traiterLongClic(x: Float, y: Float) {
        longClicEnCours = true

        val objet = trouverObjetSous(x, y)
        if (objet != null) {
            post { ecouteurLongClickObjet?.invoke(objet.identifiant) }
            return
        }
        val connexion = trouverConnexionSous(x, y)
        if (connexion != null) {
            post {
                ecouteurLongClickConnexion?.invoke(
                    connexion.identifiantSource, connexion.identifiantDestination
                )
            }
            return
        }
        if (modeActuel == ModeApplication.AJOUT_OBJET) {
            post { ecouteurAjoutObjet?.invoke(x, y) }
        }
    }


    /** Cherche si l'utilisateur a touché un objet */
    private fun trouverObjetSous(x: Float, y: Float): ObjetConnecte? {
        return graphe.objets.find { objet ->
            val distanceX = x - objet.positionX
            val distanceY = y - objet.positionY
            // Distance de Pythagore < rayon + 20 (zone de confort)
            Math.sqrt((distanceX * distanceX + distanceY * distanceY).toDouble()) < (objet.rayon + 20)
        }
    }

    private fun trouverConnexionSous(x: Float, y: Float): Connexion? {
        return graphe.connexions.find { connexion ->
            val src = graphe.trouverObjet(connexion.identifiantSource) ?: return@find false
            val dst = graphe.trouverObjet(connexion.identifiantDestination) ?: return@find false
            val milieuxX = (src.positionX + dst.positionX) / 2f
            val milieuxY = (src.positionY + dst.positionY) / 2f
            // Zone de 40px autour du milieu
            Math.abs(x - milieuxX) < 40f && Math.abs(y - milieuxY) < 40f
        }
    }

    private fun calculerCourbure(src: ObjetConnecte, dst: ObjetConnecte,
                                 xDoigt: Float, yDoigt: Float): Float {
        val dx = dst.positionX - src.positionX
        val dy = dst.positionY - src.positionY
        val longueur = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (longueur < 1f) return 0f
        val perpX = -dy / longueur
        val perpY = dx / longueur
        val milieuxX = (src.positionX + dst.positionX) / 2f
        val milieuxY = (src.positionY + dst.positionY) / 2f
        // Distance du doigt projetée sur la perpendiculaire
        return (xDoigt - milieuxX) * perpX + (yDoigt - milieuxY) * perpY
    }

    fun mettreAJourGraphe(nouveauGraphe: Graph) {
        graphe = nouveauGraphe
        invalidate()
    }

    fun changerMode(mode: ModeApplication) {
        modeActuel = mode
        sourceConnexion = null
        invalidate()
    }

    fun changerPlan(planBitmap: Bitmap?) {
        planAppartement = planBitmap
        invalidate()
    }

    fun capturerEcran(): Bitmap {
        val capture = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val toile = Canvas(capture)
        draw(toile) // Redessine tout dans le bitmap
        return capture
    }
}