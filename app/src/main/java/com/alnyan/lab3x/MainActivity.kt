package com.alnyan.lab3x

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.absoluteValue
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    fun solve() {
        val params = let {
            try {
                Pair(
                    arrayOf(
                        input_k1.text.toString().toInt(),
                        input_k2.text.toString().toInt(),
                        input_k3.text.toString().toInt(),
                        input_k4.text.toString().toInt(),
                        input_y.text.toString().toInt()
                    ),
                    input_p.text.toString().toDouble() / 100.0
                )
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "(Some of) inputs are missing or invalid", Toast.LENGTH_SHORT)
                    .show()
                return@solve
            }
        }

        if (params.second <= 0 || params.second >= 1) {
            Toast.makeText(this, "Mutation probability value is invalid", Toast.LENGTH_SHORT).show()
            return
        }

        if (params.first.find { it <= 0 } != null) {
            Toast.makeText(this, "Inputs should be >0", Toast.LENGTH_SHORT).show()
            return
        }

        if (params.first[4] < params.first.take(4).sum()) {
            Toast.makeText(this, "Given input cannot yield an integer solution", Toast.LENGTH_SHORT).show()
            return
        }

        output.text = ""

        val solution = solveDiophantineEquation(
            params.first[0],
            params.first[1],
            params.first[2],
            params.first[3],
            params.first[4],
            params.second
        )

        output.text = solution.mapIndexed { i, x ->
            "x$i = $x"
        }.joinToString("\n")
    }

    fun initialState(rng: Random, y: Int) = (0 until 5 + rng.nextInt(y - 4)).map {
        Array(4) {
            rng.nextInt(y / 2)
        }
    }

    fun solutionFitness(xs: Array<Int>, k1: Int, k2: Int, k3: Int, k4: Int, y: Int) =
        (k1 * xs[0] + k2 * xs[1] + k3 * xs[2] + k4 * xs[3] - y).absoluteValue

    fun couple(rng: Random, surv: ArrayList<Double>, population: List<Array<Int>>): Array<Pair<Int, Int>> {
        val candidateCount = surv.size / 2
        val parents = Array(candidateCount) {0}
        var maxIndex = -1
        var max = Double.MIN_VALUE
        for (i in 0 until candidateCount) {
            max = surv[0]
            maxIndex = 0
            for (j in 1 until surv.size) {
                if (surv[j] > max) {
                    max = surv[j]
                    maxIndex = j
                }
            }
            surv[maxIndex] = -1.0
            parents[i] = maxIndex
        }

        val result = Array(population.size) {Pair(0, 0)}
        var i = 0
        while (i < population.size) {
            result[i] = Pair(rng.nextInt(parents.size), rng.nextInt(parents.size))
            if (result[i].first != result[i].second) {
                ++i
            }
        }
        return result
    }

    fun produceChildren(rng: Random, state1: Array<Int>, state2: Array<Int>) = let {
        val limit = 1 + rng.nextInt(3)
        state1.take(limit) + state2.take(state2.size - limit)
    }

    fun adapt(rng: Random, errs: List<Pair<Array<Int>, Int>>, surv: List<Double>): List<Array<Int>> {
        val parents = errs.map { it.first }
        return couple(rng, ArrayList(surv), parents).map {
            produceChildren(rng, parents[it.first], parents[it.second]).toTypedArray()
        }
    }

    fun survivability(errs: Array<Int>) = let {
        val overallSurvivability = errs.fold(0.0) { acc, p -> acc + 1.0 / p.toDouble() }
        errs.map {
            (1.0 / it.toDouble()) / overallSurvivability
        }
    }
    /*

    private void randomMutation(int[][] population, int y, double mutationPercent) {
        if (RANDOM.nextDouble() < mutationPercent) {
            for (int i = 0; i < population.length; i++) {
                int randIndex = RANDOM.nextInt(population[0].length);
                population[i][randIndex] = RANDOM.nextInt(y);
            }
        }
    }
     */

    fun mutate(rng: Random, population: List<Array<Int>>, y: Int, p: Double) =
        if (p >= rng.nextDouble()) {
            population.map {
                val randIndex = rng.nextInt(4)
                val r = it.copyOf()
                r[randIndex] = rng.nextInt(y)
                r
            }
        } else {
            population
        }

    fun solveDiophantineEquation(k1: Int, k2: Int, k3: Int, k4: Int, y: Int, p: Double): Array<Int> {
        val rng = Random(System.nanoTime())
        var systemState = initialState(rng, y)

        while (true) {
            // Find out how far current state deviates from "desired" solution
            val errors = systemState.map { Pair(it, solutionFitness(it, k1, k2, k3, k4, y)) }
            val maybeSolution = errors.find { it.second == 0 }

            if (maybeSolution != null) {
                return maybeSolution.first
            }

            val survivability = survivability(errors.map { it.second }.toTypedArray())
            val mx = survivability.average()

            // Check if offspring generation produced randomly survives
            val candidate = adapt(rng, errors, survivability)
            val newSurv = survivability(candidate.map { solutionFitness(it, k1, k2, k3, k4, y) }.toTypedArray())
            val mxNew = newSurv.average()

            systemState = if (mx < mxNew) {
                // Candidate fits
                candidate
            } else {
                // Need to fix survivability then
                mutate(rng, systemState, y, p)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button_solve.setOnClickListener {
            solve()
        }
    }
}
