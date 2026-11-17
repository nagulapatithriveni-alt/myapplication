package com.example.myapplicationplr

import android.util.Log
import kotlin.collections.iterator

kotlin
package com.example.myapplicationplr

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope

import com.example.proteinexplorer.analysis.BioKtAdapter
import com.example.proteinexplorer.data.ProteinDatabase
import com.example.proteinexplorer.data.ProteinEntity
import com.example.proteinexplorer.net.UniProtApi

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val dao by lazy {
        ProteinDatabase.get(this).proteinDao()
    }

    private var auth: FirebaseAuth? = null

    private lateinit var authStatus: TextView
    private lateinit var btnSignIn: Button
    private lateinit var accession: EditText
    private lateinit var result: TextView
    private lateinit var compositionTitle: TextView
    private lateinit var compositionContainer: LinearLayout
    private lateinit var spinner1: Spinner
    private lateinit var spinner2: Spinner

    private var proteins = listOf<ProteinEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        bindViews()

        initializeFirebase()

        initAuth()

        loadSavedProteins()
    }

    // Firebase initialization
    private fun initializeFirebase() {

        try {
            val firebaseApp = FirebaseApp.initializeApp(this)

            if (firebaseApp != null) {
                auth = FirebaseAuth.getInstance()

                authStatus.text = "Firebase initialized successfully"

            } else {
                authStatus.text = "Firebase configuration not found"
                btnSignIn.isEnabled = false

                Toast.makeText(
                    this,
                    "Check google-services.json",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            auth = null
            btnSignIn.isEnabled = false

            authStatus.text = "Firebase initialization failed"

            Log.e(
                "FIREBASE",
                "Firebase initialization error",
                e
            )
        }
    }

    // Bind UI components
    private fun bindViews() {

        authStatus = findViewById(R.id.authStatus)
        btnSignIn = findViewById(R.id.btnSignIn)

        accession = findViewById(R.id.accession)
        result = findViewById(R.id.result)

        compositionTitle = findViewById(R.id.compositionTitle)
        compositionContainer =
            findViewById(R.id.compositionContainer)

        spinner1 = findViewById(R.id.spinner1)
        spinner2 = findViewById(R.id.spinner2)

        findViewById<Button>(R.id.scan).setOnClickListener {
            scanQR()
        }

        findViewById<Button>(R.id.search).setOnClickListener {

            val code = accession.text.toString().trim()

            if (code.isNotEmpty()) {
                loadProtein(code)
            } else {
                Toast.makeText(
                    this,
                    "Please enter an accession ID",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        findViewById<Button>(R.id.refresh).setOnClickListener {
            loadSavedProteins()
        }

        findViewById<Button>(R.id.compare).setOnClickListener {
            compareProteins()
        }

        btnSignIn.setOnClickListener {
            handleAuthAction()
        }
    }

    // Firebase authentication status
    private fun initAuth() {

        val firebaseAuth = auth

        if (firebaseAuth == null) {
            authStatus.text = "Firebase unavailable"
            btnSignIn.isEnabled = false
            return
        }

        val currentUser = firebaseAuth.currentUser

        if (currentUser != null) {

            authStatus.text =
                "Signed in as: ${currentUser.email ?: currentUser.uid}"

            btnSignIn.text = "Sign Out"

        } else {

            authStatus.text = "Firebase Auth: Not Signed In"
            btnSignIn.text = "Sign In"
        }
    }

    // Sign in and sign out
    private fun handleAuthAction() {

        val firebaseAuth = auth ?: return

        val currentUser = firebaseAuth.currentUser

        if (currentUser != null) {

            firebaseAuth.signOut()

            Toast.makeText(
                this,
                "Signed Out",
                Toast.LENGTH_SHORT
            ).show()

            initAuth()

        } else {

            firebaseAuth.signInAnonymously()
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Firebase Sign-In Successful",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        Toast.makeText(
                            this,
                            "Sign-In: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    initAuth()
                }
        }
    }

    // QR scanner
    private fun scanQR() {

        GmsBarcodeScanning.getClient(this)
            .startScan()
            .addOnSuccessListener { barcode ->

                val code = barcode.rawValue

                if (!code.isNullOrBlank()) {

                    accession.setText(code)

                    loadProtein(code)
                }
            }
            .addOnFailureListener {

                result.text =
                    "QR Scanning failed: ${it.message}"
            }
    }

    // UniProt protein search
    private fun loadProtein(id: String) {

        lifecycleScope.launch {

            try {

                result.text =
                    "Downloading protein details from UniProt..."

                compositionTitle.visibility = View.GONE
                compositionContainer.removeAllViews()

                val protein = UniProtApi.fetch(id)

                val stats = withContext(Dispatchers.Default) {

                    BioKtAdapter.analyze(protein.sequence)
                }

                dao.save(
                    ProteinEntity(
                        protein.accession,
                        protein.name,
                        protein.organism,
                        protein.sequence
                    )
                )

                result.text = """
                    Protein Name: ${protein.name}
                    Accession: ${protein.accession}
                    Organism: ${protein.organism}
                    Length: ${protein.sequence.length}

                    --- bioKT Descriptors ---

                    Molecular Weight:
                    ${String.format("%.2f", stats.molecularWeight)} Da

                    Isoelectric Point (pI):
                    ${String.format("%.2f", stats.pI)}

                    Instability Index:
                    ${String.format("%.2f", stats.instabilityIndex)}

                    Aromaticity:
                    ${String.format("%.2f%%", stats.aromaticity * 100)}

                    GRAVY:
                    ${String.format("%.3f", stats.gravy)}

                    Sequence:
                    ${protein.sequence}

                """.trimIndent()

                displayCompositionBars(stats.composition)

                loadSavedProteins()

            } catch (e: Exception) {

                result.text =
                    "Error loading protein: ${e.message}"

                Log.e(
                    "PROTEIN",
                    "Protein loading failed",
                    e
                )
            }
        }
    }

    // Amino acid composition chart
    private fun displayCompositionBars(
        composition: Map<Char, Double>
    ) {

        compositionContainer.removeAllViews()

        if (composition.isEmpty()) {

            compositionTitle.visibility = View.GONE
            return
        }

        compositionTitle.visibility = View.VISIBLE

        for ((aa, percent) in composition.toSortedMap()) {

            val row = LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 4)
                }
            }

            val label = TextView(this).apply {

                text = String.format(
                    "%c: %5.1f%%",
                    aa,
                    percent
                )

                textSize = 12f

                layoutParams = LinearLayout.LayoutParams(
                    (80 * resources.displayMetrics.density).toInt(),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val progressBar = ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {

                max = 100

                progress = percent.toInt().coerceIn(0, 100)

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            row.addView(label)
            row.addView(progressBar)

            compositionContainer.addView(row)
        }
    }

    // Load saved proteins
    private fun loadSavedProteins() {

        lifecycleScope.launch {

            try {

                proteins = dao.getAll()

                val names = proteins.map {
                    "${it.accession} - ${it.name}"
                }

                val adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_item,
                    names
                )

                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                spinner1.adapter = adapter
                spinner2.adapter = adapter

                if (proteins.size > 1) {
                    spinner2.setSelection(1)
                }

            } catch (e: Exception) {

                result.text =
                    "Database error: ${e.message}"

                Log.e(
                    "DATABASE",
                    "Loading saved proteins failed",
                    e
                )
            }
        }
    }

    // Compare two proteins
    private fun compareProteins() {

        if (proteins.size < 2) {

            result.text =
                "Save at least two proteins first to compare."

            return
        }

        val pos1 = spinner1.selectedItemPosition
        val pos2 = spinner2.selectedItemPosition

        if (
            pos1 < 0 ||
            pos2 < 0 ||
            pos1 >= proteins.size ||
            pos2 >= proteins.size
        ) {

            result.text =
                "Please select valid proteins to compare."

            return
        }

        val p1 = proteins[pos1]
        val p2 = proteins[pos2]

        if (p1.accession == p2.accession) {

            result.text =
                "Select two different proteins to compare."

            return
        }

        lifecycleScope.launch {

            try {

                result.text =
                    "Running bioKT PairwiseAligner (GLOBAL mode, BLOSUM62)..."

                compositionTitle.visibility = View.GONE
                compositionContainer.removeAllViews()

                val alignment = withContext(Dispatchers.Default) {

                    BioKtAdapter.align(
                        p1.sequence,
                        p2.sequence
                    )
                }

                result.text = """
                    === bioKT PAIRWISE ALIGNMENT ===

                    Protein 1: ${p1.name} (${p1.accession})
                    Protein 2: ${p2.name} (${p2.accession})

                    Alignment Mode: GLOBAL
                    Matrix: BLOSUM62

                    Score: ${alignment.score}

                    Identity:
                    ${String.format("%.2f", alignment.identityPercent)}%

                    Alignment View:

                    ${alignment.pretty}

                """.trimIndent()

            } catch (e: Exception) {

                result.text =
                    "Alignment error: ${e.message}"

                Log.e(
                    "ALIGNMENT",
                    "Protein alignment failed",
                    e
                )
            }
        }
    }
}
```