package ru.starline.bluz.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Created by ed on 06,май,2026
 */
@Entity(tableName = "detectors")

data class DetectorType (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "changeAt") val changeAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "chiVector") val chiVector: DoubleArray,
    @ColumnInfo(name = "curActive") val curActive: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DetectorType

        if (id != other.id) return false
        if (changeAt != other.changeAt) return false
        if (name != other.name) return false
        if (!chiVector.contentEquals(other.chiVector)) return false
        if (curActive != other.curActive) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + changeAt.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + chiVector.contentHashCode()
        result = 31 * result + curActive.hashCode()
        return result
    }
}
