package mg.hoaviko.app

val MODES_VERSEMENT = listOf(
    "Orange Money",
    "Airtel Money",
    "MVola"
)

class CotisationRepository(
    private val dao: CotisationDao
) {
    fun observerCotisations(utilisateurId: String) =
        dao.observerCotisations(utilisateurId)

    fun observerTotal(utilisateurId: String) =
        dao.observerTotal(utilisateurId)

    suspend fun ajouter(
        utilisateurId: String,
        montantAr: Long,
        modeVersement: String
    ) {
        require(utilisateurId.isNotBlank())
        require(montantAr > 0)
        require(modeVersement in MODES_VERSEMENT)

        dao.ajouter(
            Cotisation(
                utilisateurId = utilisateurId,
                montantAr = montantAr,
                modeVersement = modeVersement
            )
        )
    }
}
