package ru.starline.bluz

import android.util.Log

/*
*   Решение системы урарвнений
*/
class Mtrx {
    public var cA: Float = 0.0f
    public var cB: Float = 1.0f
    public var cC: Float = 0.0f


    public var sysArray:Array<DoubleArray> = Array(3) {DoubleArray(2)}

    private fun determinant(matr: Array<DoubleArray>): Double {
        val det =
            matr[0][0] * matr[1][1] * matr[2][2] +
            matr[2][0] * matr[0][1] * matr[1][2] +
            matr[1][0] * matr[2][1] * matr[0][2] -
            matr[2][0] * matr[1][1] * matr[0][2] -
            matr[0][0] * matr[2][1] * matr[1][2] -
            matr[1][0] * matr[0][1] * matr[2][2]
        return det
    }

    public fun sysEq() {
        val detArray: Array<DoubleArray> = Array(3) {DoubleArray(3)}
        detArray[0][0] = sysArray[0][0] * sysArray[0][0]
        detArray[1][0] = sysArray[1][0] * sysArray[1][0]
        detArray[2][0] = sysArray[2][0] * sysArray[2][0]
        detArray[0][1] = sysArray[0][0]
        detArray[1][1] = sysArray[1][0]
        detArray[2][1] = sysArray[2][0]
        detArray[0][2] = 1.0
        detArray[1][2] = 1.0
        detArray[2][2] = 1.0
        for (i in 0..2) {
            for(j in 0..1) {
                Log.i("BluZ-BT", "Arr: $i " + sysArray[i][j])
            }
        }
        val mainDet = determinant(detArray)
        if (mainDet == 0.0) {  // Система не имеет решения в области действительных чисел
            GO.propCoefA = 0.0f
            GO.propCoefB = 1.0f
            GO.propCoefC = 0.0f
        } else {
            detArray[0][0] = sysArray[0][1]
            detArray[1][0] = sysArray[1][1]
            detArray[2][0] = sysArray[2][1]
            cA = (determinant(detArray) / mainDet).toFloat()
            detArray[0][0] = sysArray[0][0] * sysArray[0][0]
            detArray[1][0] = sysArray[1][0] * sysArray[1][0]
            detArray[2][0] = sysArray[2][0] * sysArray[2][0]
            detArray[0][1] = sysArray[0][1]
            detArray[1][1] = sysArray[1][1]
            detArray[2][1] = sysArray[2][1]
            cB = (determinant(detArray) / mainDet).toFloat()
            detArray[0][1] = sysArray[0][0]
            detArray[1][1] = sysArray[1][0]
            detArray[2][1] = sysArray[2][0]
            detArray[0][2] = sysArray[0][1]
            detArray[1][2] = sysArray[1][1]
            detArray[2][2] = sysArray[2][1]
            cC = (determinant(detArray) / mainDet).toFloat()
        }
        Log.i("BluZ-BT", "det: $mainDet A: " + GO.propCoefA + " B: " + GO.propCoefB + " C: " + GO.propCoefC)
    }
}