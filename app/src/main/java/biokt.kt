package com.example.proteinexplorer.analysis

class ProteinSequence(val sequence: String) {

    fun molecularWeight(): Double {
        val weights = mapOf(
            'A' to 89.09, 'R' to 174.20, 'N' to 132.12, 'D' to 133.10, 'C' to 121.16,
            'E' to 147.13, 'Q' to 146.15, 'G' to 75.07, 'H' to 155.16, 'I' to 131.17,
            'L' to 131.17, 'K' to 146.19, 'M' to 149.21, 'F' to 165.19, 'P' to 115.13,
            'S' to 105.09, 'T' to 119.12, 'W' to 204.23, 'Y' to 181.19, 'V' to 117.15
        )
        val seqUpper = sequence.uppercase().filter { it in weights.keys }
        if (seqUpper.isEmpty()) return 0.0
        val total = seqUpper.sumOf { weights[it] ?: 0.0 }
        val water = (seqUpper.length - 1) * 18.015
        return (total - water).coerceAtLeast(0.0)
    }

    fun isoelectricPoint(): Double {
        val seqUpper = sequence.uppercase().filter { it in "ACDEFGHIKLMNPQRSTVWY" }
        if (seqUpper.isEmpty()) return 7.0
        var minPi = 3.0
        var maxPi = 10.0
        var pi = 6.5
        for (step in 0..15) {
            pi = (minPi + maxPi) / 2.0
            val charge = computeCharge(seqUpper, pi)
            if (charge > 0) minPi = pi else maxPi = pi
        }
        return (minPi + maxPi) / 2.0
    }

    private fun computeCharge(seq: String, pH: Double): Double {
        var charge = 0.0
        charge += 1.0 / (1.0 + Math.pow(10.0, pH - 9.69))
        charge -= 1.0 / (1.0 + Math.pow(10.0, 2.34 - pH))
        for (c in seq) {
            when (c) {
                'K' -> charge += 1.0 / (1.0 + Math.pow(10.0, pH - 10.54))
                'R' -> charge += 1.0 / (1.0 + Math.pow(10.0, pH - 12.48))
                'H' -> charge += 1.0 / (1.0 + Math.pow(10.0, pH - 6.04))
                'D' -> charge -= 1.0 / (1.0 + Math.pow(10.0, 3.86 - pH))
                'E' -> charge -= 1.0 / (1.0 + Math.pow(10.0, 4.25 - pH))
                'C' -> charge -= 1.0 / (1.0 + Math.pow(10.0, 8.33 - pH))
                'Y' -> charge -= 1.0 / (1.0 + Math.pow(10.0, 10.07 - pH))
            }
        }
        return charge
    }

    fun instabilityIndex(): Double {
        val seqUpper = sequence.uppercase().filter { it in "ACDEFGHIKLMNPQRSTVWY" }
        if (seqUpper.length < 2) return 0.0
        var sum = 0.0
        for (i in 0 until seqUpper.length - 1) {
            val d1 = seqUpper[i]
            val d2 = seqUpper[i + 1]
            val diValue = when {
                d1 == 'M' || d2 == 'M' -> 1.0
                d1 == 'W' || d2 == 'W' -> 24.6
                d1 == 'P' || d2 == 'P' -> 20.0
                d1 == 'C' || d2 == 'C' -> 15.0
                d1 == d2 -> 1.0
                else -> 1.0
            }
            sum += diValue
        }
        return (10.0 / seqUpper.length) * sum
    }

    fun aromaticity(): Double {
        val seqUpper = sequence.uppercase().filter { it in "ACDEFGHIKLMNPQRSTVWY" }
        if (seqUpper.isEmpty()) return 0.0
        val aromaticCount = seqUpper.count { it == 'F' || it == 'W' || it == 'Y' }
        return aromaticCount.toDouble() / seqUpper.length
    }

    fun aminoAcidComposition(): Map<Char, Double> {
        val seqUpper = sequence.uppercase().filter { it in "ACDEFGHIKLMNPQRSTVWY" }
        if (seqUpper.isEmpty()) return emptyMap()
        val counts = mutableMapOf<Char, Int>()
        for (c in seqUpper) {
            counts[c] = (counts[c] ?: 0) + 1
        }
        val total = seqUpper.length.toDouble()
        return counts.mapValues { (it.value / total) * 100.0 }
    }
}

object ProteinDescriptors {
    private val hydropathy = mapOf(
        'A' to 1.8, 'R' to -4.5, 'N' to -3.5, 'D' to -3.5, 'C' to 2.5,
        'Q' to -3.5, 'E' to -3.5, 'G' to -0.4, 'H' to -3.2, 'I' to 4.5,
        'L' to 3.8, 'K' to -3.9, 'M' to 1.9, 'F' to 2.8, 'P' to -1.6,
        'S' to -0.8, 'T' to -0.7, 'W' to -0.9, 'Y' to -1.3, 'V' to 4.2
    )

    fun gravyIndex(protein: ProteinSequence): Double {
        val seqUpper = protein.sequence.uppercase().filter { it in hydropathy.keys }
        if (seqUpper.isEmpty()) return 0.0
        val sum = seqUpper.sumOf { hydropathy[it] ?: 0.0 }
        return sum / seqUpper.length
    }
}

object ScoringMatrices {
    val BLOSUM62 = "BLOSUM62"

    private const val AA_ORDER = "ARNDCQEGHILKMFPSTWYV"
    private val matrix = arrayOf(
        intArrayOf( 4,-1,-2,-2, 0,-1,-1, 0,-2,-1,-1,-1,-1,-2,-1, 1, 0,-3,-2, 0), // A
        intArrayOf(-1, 5, 0,-2,-3, 1, 0,-2, 0,-3,-2, 2,-1,-3,-2,-1,-1,-3,-2,-3), // R
        intArrayOf(-2, 0, 6, 1,-3, 0, 0, 0, 1,-3,-3, 0,-2,-3,-2, 1, 0,-4,-2,-3), // N
        intArrayOf(-2,-2, 1, 6,-3, 0, 2,-1,-1,-3,-4,-1,-3,-3,-1, 0,-1,-4,-3,-3), // D
        intArrayOf( 0,-3,-3,-3, 9,-3,-4,-3,-3,-1,-1,-3,-1,-2,-3,-1,-1,-2,-2,-1), // C
        intArrayOf(-1, 1, 0, 0,-3, 5, 2,-2, 0,-3,-2, 1, 0,-3,-1, 0,-1,-2,-1,-2), // Q
        intArrayOf(-1, 0, 0, 2,-4, 2, 5,-2, 0,-3,-3, 1,-2,-3,-1, 0,-1,-3,-2,-2), // E
        intArrayOf( 0,-2, 0,-1,-3,-2,-2, 6,-2,-4,-4,-2,-3,-3,-2, 0,-2,-2,-3,-3), // G
        intArrayOf(-2, 0, 1,-1,-3, 0, 0,-2, 8,-3,-3,-1,-2,-1,-2,-1,-2,-2, 2,-3), // H
        intArrayOf(-1,-3,-3,-3,-1,-3,-3,-4,-3, 4, 2,-3, 1, 0,-3,-1,-1,-1,-1, 3), // I
        intArrayOf(-1,-2,-3,-4,-1,-2,-3,-4,-3, 2, 4,-2, 2, 0,-3,-2,-1,-2,-1, 1), // L
        intArrayOf(-1, 2, 0,-1,-3, 1, 1,-2,-1,-3,-2, 5,-1,-3,-1, 0,-1,-3,-2,-2), // K
        intArrayOf(-1,-1,-2,-3,-1, 0,-2,-3,-2, 1, 2,-1, 5, 0,-2,-1,-1,-1,-1, 1), // M
        intArrayOf(-2,-3,-3,-3,-2,-3,-3,-3,-1, 0, 0,-3, 0, 6,-4,-2,-2, 1, 3,-1), // F
        intArrayOf(-1,-2,-2,-1,-3,-1,-1,-2,-2,-3,-3,-1,-2,-4, 7,-1,-1,-4,-3,-2), // P
        intArrayOf( 1,-1, 1, 0,-1, 0, 0, 0,-1,-1,-2, 0,-1,-2,-1, 4, 1,-3,-2,-2), // S
        intArrayOf( 0,-1, 0,-1,-1,-1,-1,-2,-2,-1,-1,-1,-1,-2,-1, 1, 5,-2,-2, 0), // T
        intArrayOf(-3,-3,-4,-4,-2,-2,-3,-2,-2,-1,-2,-3,-1, 1,-4,-3,-2,11, 2,-3), // W
        intArrayOf(-2,-2,-2,-3,-2,-1,-2,-3, 2,-1,-1,-2,-1, 3,-3,-2,-2, 2, 7,-1), // Y
        intArrayOf( 0,-3,-3,-3,-1,-2,-2,-3,-3, 3, 1,-2, 1,-1,-2,-2, 0,-3,-1, 4)  // V
    )

    fun score(a: Char, b: Char): Int {
        val i = AA_ORDER.indexOf(a.uppercaseChar())
        val j = AA_ORDER.indexOf(b.uppercaseChar())
        return if (i >= 0 && j >= 0) matrix[i][j] else -4
    }
}

class PairwiseAligner(
    val mode: Mode = Mode.GLOBAL,
    val substitutionMatrix: Any? = ScoringMatrices.BLOSUM62,
    val gapOpen: Double = -11.0,
    val gapExtend: Double = -1.0
) {
    enum class Mode { GLOBAL, LOCAL }

    data class Alignment(
        val score: Double,
        val identity: Double,
        val alignedSeq1: String,
        val alignedSeq2: String
    ) {
        fun prettyPrint(): String {
            val matches = StringBuilder()
            for (i in alignedSeq1.indices) {
                if (i < alignedSeq2.length) {
                    val c1 = alignedSeq1[i]
                    val c2 = alignedSeq2[i]
                    if (c1 == c2 && c1 != '-') {
                        matches.append("|")
                    } else if (c1 != '-' && c2 != '-' && ScoringMatrices.score(c1, c2) > 0) {
                        matches.append(":")
                    } else {
                        matches.append(" ")
                    }
                }
            }
            return "$alignedSeq1\n$matches\n$alignedSeq2"
        }
    }

    fun align(sequence1: String, sequence2: String): Alignment {
        val s1 = sequence1.uppercase()
        val s2 = sequence2.uppercase()
        val n = s1.length
        val m = s2.length
        if (n == 0 || m == 0) {
            return Alignment(0.0, 0.0, s1, s2)
        }

        val gapPenalty = gapOpen.toInt()
        val dp = Array(n + 1) { IntArray(m + 1) }

        for (i in 0..n) dp[i][0] = i * gapPenalty
        for (j in 0..m) dp[0][j] = j * gapPenalty

        for (i in 1..n) {
            for (j in 1..m) {
                val matchScore = ScoringMatrices.score(s1[i - 1], s2[j - 1])
                val scoreDiag = dp[i - 1][j - 1] + matchScore
                val scoreUp = dp[i - 1][j] + gapPenalty
                val scoreLeft = dp[i][j - 1] + gapPenalty
                dp[i][j] = maxOf(scoreDiag, scoreUp, scoreLeft)
            }
        }

        val sb1 = StringBuilder()
        val sb2 = StringBuilder()
        var i = n
        var j = m
        var matches = 0
        var totalAligned = 0

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + ScoringMatrices.score(s1[i - 1], s2[j - 1])) {
                val c1 = s1[i - 1]
                val c2 = s2[j - 1]
                sb1.append(c1)
                sb2.append(c2)
                if (c1 == c2) matches++
                totalAligned++
                i--
                j--
            } else if (i > 0 && dp[i][j] == dp[i - 1][j] + gapPenalty) {
                sb1.append(s1[i - 1])
                sb2.append('-')
                totalAligned++
                i--
            } else {
                sb1.append('-')
                sb2.append(s2[j - 1])
                totalAligned++
                j--
            }
        }

        val a1 = sb1.reverse().toString()
        val a2 = sb2.reverse().toString()
        val identity = if (totalAligned > 0) matches.toDouble() / totalAligned else 0.0

        return Alignment(
            score = dp[n][m].toDouble(),
            identity = identity,
            alignedSeq1 = a1,
            alignedSeq2 = a2
        )
    }
}

data class ProteinStats(
    val molecularWeight: Double,
    val pI: Double,
    val gravy: Double,
    val instabilityIndex: Double,
    val aromaticity: Double,
    val composition: Map<Char, Double>
)

object BioKtAdapter {

    fun analyze(sequence: String): ProteinStats {
        val protein = ProteinSequence(sequence)
        val weight = protein.molecularWeight()
        val pi = protein.isoelectricPoint()
        val gravy = ProteinDescriptors.gravyIndex(protein)
        val instability = protein.instabilityIndex()
        val aromaticity = protein.aromaticity()
        val composition = protein.aminoAcidComposition()

        return ProteinStats(
            molecularWeight = weight,
            pI = pi,
            gravy = gravy,
            instabilityIndex = instability,
            aromaticity = aromaticity,
            composition = composition
        )
    }

    data class AlignmentResult(
        val score: Double,
        val identityPercent: Double,
        val pretty: String
    )

    fun align(
        sequence1: String,
        sequence2: String
    ): AlignmentResult {
        val aligner = PairwiseAligner(
            mode = PairwiseAligner.Mode.GLOBAL,
            substitutionMatrix = ScoringMatrices.BLOSUM62,
            gapOpen = -11.0,
            gapExtend = -1.0
        )

        val result = aligner.align(
            sequence1,
            sequence2
        )

        return AlignmentResult(
            result.score,
            result.identity * 100,
            result.prettyPrint()
        )
    }
}