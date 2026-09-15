package mg.hoaviko.app

// Règles Hoaviko pour le retrait avant l'âge légal de la retraite.
const val AGE_RETRAITE = 60
const val TAUX_PENALITE_ANTICIPEE = 10 // en pourcentage

class RetraitRepository(
    private val dao: RetraitDao
) {
    fun observerRetraits(utilisateurId: String) =
        dao.observerRetraits(utilisateurId)

    fun observerTotalRetire(utilisateurId: String) =
        dao.observerTotal(utilisateurId)

    suspend fun retirer(
        utilisateurId: String,
        montantBrutAr: Long,
        tauxPenalite: Int,
        modeVersement: String
    ) {
        require(utilisateurId.isNotBlank())
        require(montantBrutAr > 0)
        require(modeVersement in MODES_VERSEMENT)

        val penaliteAr = (montantBrutAr * tauxPenalite) / 100

        dao.ajouter(
            Retrait(
                utilisateurId = utilisateurId,
                montantBrutAr = montantBrutAr,
                montantNetAr = montantBrutAr - penaliteAr,
                tauxPenalite = tauxPenalite,
                modeVersement = modeVersement
            )
        )
    }
}


