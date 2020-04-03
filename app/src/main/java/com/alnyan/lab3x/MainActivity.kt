package com.alnyan.lab3x

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.NumberFormatException
import kotlin.math.abs

typealias TrainPoint = Pair<Array<Double>, Boolean>

class MainActivity : AppCompatActivity() {
    private val dataSet = ArrayList<TrainPoint>()
    private var ws: Array<Double> = arrayOf(0.0, 0.0)

    private fun eval(xs: Array<Double>, ws: Array<Double>) =
        (xs.zip(ws) { x, w -> x * w }).sum()

    private fun updatePointList() {
        point_list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            dataSet.map { "(${it.first.joinToString("; ")}) ${if (it.second) ">" else "<"} P" })
    }

    private fun updateEquation() {
        val wstr = ws.map { String.format("%.4f", abs(it)) }
        function_text.text = "y = ${wstr[0]}x0 ${if (ws[1] > 0) '+' else '-'} ${wstr[1]}x1"
    }

    private fun train() {
        // Pre-flight checks
        if (dataSet.isEmpty()) {
            Toast.makeText(this, "Dataset is empty!", Toast.LENGTH_SHORT).show()
            return
        }
        val delta = delta_edit.text.toString().toDoubleOrNull()
        if (delta == null) {
            Toast.makeText(this, "Learning speed value is invalid", Toast.LENGTH_SHORT).show()
            return
        }
        val thr = p_edit.text.toString().toDoubleOrNull()
        if (thr == null) {
            Toast.makeText(this, "Threshold (P) is invalid", Toast.LENGTH_SHORT).show()
            return
        }
        val deadlineValue = limit_edit.text.toString().toIntOrNull()
        if (deadlineValue == null) {
            Toast.makeText(this, "Deadline value is invalid", Toast.LENGTH_SHORT).show()
            return
        }

        // Reset weights
        ws = arrayOf(0.0, 0.0)

        val isIterationBased = limit_type_spinner.selectedItemPosition == 0
        var round = 0
        val startTime = System.currentTimeMillis()
        var failure = true

        while (true) {
            val currTime = System.currentTimeMillis()
            if (!isIterationBased && (currTime - startTime) >= deadlineValue * 1000) {
                break
            }
            if (isIterationBased && round >= deadlineValue) {
                break
            }
            println("Round $round")
            // Train on input set
            failure = false

            for (test in dataSet) {
                val xs = test.first
                val big = test.second
                val y = eval(xs, ws)
                if (big != (y > thr)) {
                    failure = true
                    // Apply correction
                    ws = ws.zip(xs) { w, x -> w + x * delta * (thr - y) }.toTypedArray()
                }
            }

            if (!failure) {
                break
            }

            ++round
        }

        // Which basically means we failed to pick a good weight combo
        if (failure) {
            ws = arrayOf(0.0, 0.0)
            Toast.makeText(this, "Training failed", Toast.LENGTH_SHORT).show()
        }

        updateEquation()
    }

    private fun test() {
        if (dataSet.isEmpty()) {
            Toast.makeText(this, "Dataset is empty!", Toast.LENGTH_SHORT).show()
            return
        }
        val thr = p_edit.text.toString().toDoubleOrNull()
        if (thr == null) {
            Toast.makeText(this, "Threshold (P) is invalid", Toast.LENGTH_SHORT).show()
            return
        }

        val failedTests = ArrayList<Pair<TrainPoint, Double>>()

        for (test in dataSet) {
            val xs = test.first
            val big = test.second

            val y = eval(xs, ws)

            if (big != (y > thr)) {
                failedTests.add(Pair(test, y))
            }
        }

        if (failedTests.isNotEmpty()) {
            var failedTestsString = "${failedTests.size} test(s) failed:"
            for (failedTest in failedTests) {
                failedTestsString += "\nf(${failedTest.first.first.joinToString("; ")}) = " +
                                     "${failedTest.second} ${if (failedTest.second > thr) '>' else '<'} P"
            }
            Toast.makeText(this, failedTestsString, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "All tests good!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        delta_edit.setText("0.06")
        limit_edit.setText("10")

        dataSet.add(Pair(arrayOf(0.0, 6.0), false))
        dataSet.add(Pair(arrayOf(1.0, 5.0), true))
        dataSet.add(Pair(arrayOf(3.0, 3.0), true))
        dataSet.add(Pair(arrayOf(2.0, 4.0), true))
        updatePointList()

        p_edit.setRawInputType(InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_NUMBER)
        x0_edit.setRawInputType(InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_NUMBER)
        x1_edit.setRawInputType(InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_NUMBER)

        clear_button.setOnClickListener {
            dataSet.clear()
            updatePointList()
        }
        add_point_button.setOnClickListener {
            try {
                val x0 = x0_edit.text.toString().toDouble()
                val x1 = x1_edit.text.toString().toDouble()
                val c = operation_spinner.selectedItemPosition == 0
                dataSet.add(Pair(arrayOf(x0, x1), c))
                updatePointList()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid point entered", Toast.LENGTH_SHORT).show()
            }
        }
        train_button.setOnClickListener { train() }
        test_button.setOnClickListener { test() }
    }
}
