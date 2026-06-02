package ru.starline.bluz.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность Room — детектор (NaI, CsI, ...) с χ-вектором для расчёта эквивалентной дозы.
 *
 * Активный детектор используется [ru.starline.bluz.DoseCalculator] для MLEM-расчёта
 * мощности дозы (`GO.compMED`). Активным может быть только один — управляется
 * [ru.starline.bluz.data.dao.DosimeterDao.activateDetector] (атомарный CASE-update).
 *
 * **Кастомные `equals` / `hashCode`** — `DoubleArray` сравнивается через `contentEquals`,
 * а не reference-equality. Дефолтная реализация Kotlin для `data class` не подошла бы.
 *
 * @property id Сгенерированный Room ID.
 * @property name Имя детектора (например «10×40 NaI(Tl) Sensl»).
 * @property changeAt Unix-timestamp последнего изменения (`System.currentTimeMillis()` —
 *  миллисекунды, а не секунды как в [Track.createdAt]).
 * @property chiVector χ-вектор из 1024 значений типа Double — энергозависимая
 *  чувствительность в `[μR/h] / [импульс]`. Сериализуется через [ru.starline.bluz.data.DatabaseConverters].
 * @property curActive Флаг текущей активности.
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
