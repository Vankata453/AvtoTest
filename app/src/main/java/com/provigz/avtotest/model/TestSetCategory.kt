package com.provigz.avtotest.model

import com.google.gson.annotations.SerializedName

enum class TestSetCategory(val id: Int) {
    @SerializedName("2")  A(id = 2),
    @SerializedName("7")  A1(id = 7),
    @SerializedName("18") A2(id = 18),
    @SerializedName("1")  AM(id = 1),

    @SerializedName("3")  B(id = 3),
    @SerializedName("8")  B1(id = 8),

    @SerializedName("9")  C(id = 9),
    @SerializedName("10") C1(id = 10),
    @SerializedName("13") CE(id = 13),

    @SerializedName("11") D(id = 11),
    @SerializedName("12") D1(id = 12),
    @SerializedName("14") DE(id = 14),

    @SerializedName("4")  TKT(id = 4),
    @SerializedName("5")  TTB(id = 5),
    @SerializedName("6")  TTM(id = 6),

    @SerializedName("25") OGain(id = 25),
    @SerializedName("26") CGain(id = 26),
    @SerializedName("27") ADRGain1(id = 27),
    @SerializedName("28") ADRGain7(id = 28),

    @SerializedName("29") OExtend(id = 29),
    @SerializedName("30") CExtend(id = 30),
    @SerializedName("31") ADRExtend1(id = 31),
    @SerializedName("32") ADRExtend7(id = 32),

    @SerializedName("49") BTA(id = 49);
    //@SerializedName("48") BTABoss(id = 48);

    companion object {
        fun fromInt(value: Int): TestSetCategory? = entries.find { it.id == value }
    }
    fun toInt(): Int = this.id

    override fun toString(): String {
        return when (this) {
            TKT -> "Ткт"
            TTB -> "Ттб"
            TTM -> "Ттм"

            OGain, OExtend -> "О"
            CGain, CExtend -> "Ц"
            ADRGain1, ADRExtend1 -> "Клас 1"
            ADRGain7, ADRExtend7 -> "Клас 7"

            else -> name
        }
    }
}