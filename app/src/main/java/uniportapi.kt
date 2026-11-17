package com.example.proteinexplorer.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class ProteinRecord(
    val accession: String,
    val name: String,
    val organism: String,
    val sequence: String
)

object UniProtApi {

    suspend fun fetch(id: String): ProteinRecord =
        withContext(Dispatchers.IO) {

            val accession = id.trim().uppercase()

            require(accession.matches(Regex("[A-Z0-9]{6,10}"))) {
                "Invalid accession"
            }

            val fasta = URL(
                "https://rest.uniprot.org/uniprotkb/$accession.fasta"
            ).readText()

            val sequence = fasta.lines()
                .filterNot { it.startsWith(">") }
                .joinToString("")
                .trim()
                .uppercase()

            require(sequence.isNotEmpty()) {
                "Protein sequence not found"
            }

            val json = JSONObject(
                URL(
                    "https://rest.uniprot.org/uniprotkb/$accession.json"
                ).readText()
            )

            val name = json
                .getJSONObject("proteinDescription")
                .getJSONObject("recommendedName")
                .getJSONObject("fullName")
                .getString("value")

            val organism = json
                .getJSONObject("organism")
                .getString("scientificName")

            ProteinRecord(
                accession,
                name,
                organism,
                sequence
            )
        }
}