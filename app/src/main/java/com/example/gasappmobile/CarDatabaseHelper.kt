package com.example.gasappmobile

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CarDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "cars.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE cars (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT,
                gasAutonomy REAL,
                ethAutonomy REAL
            )
            """
        )

        val cars = listOf(
            Triple("Chevrolet Onix 1.0", 10.5, 8.7),
            Triple("Hyundai HB20", 11.5, 8.2),
            Triple("Volkswagen Polo", 11.8, 8.4),
            Triple("Toyota Corolla", 11.9, 8.0),
            Triple("BMW 320i", 9.4, 6.5),
            Triple("Volkswagen Tcross 1.4 TSI", 12.0, 8.5)
        )

        cars.forEach { (name, gas, eth) ->
            val values = ContentValues().apply {
                put("name", name)
                put("gasAutonomy", gas)
                put("ethAutonomy", eth)
            }
            db.insert("cars", null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS cars")
        onCreate(db)
    }

    fun getAllCars(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT name FROM cars", null)
        val cars = mutableListOf<String>()
        while (cursor.moveToNext()) {
            cars.add(cursor.getString(0))
        }
        cursor.close()
        return cars
    }

    fun getAutonomyByCar(name: String): Pair<Float, Float>? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT gasAutonomy, ethAutonomy FROM cars WHERE name = ?",
            arrayOf(name)
        )
        val result = if (cursor.moveToFirst()) {
            Pair(cursor.getFloat(0), cursor.getFloat(1))
        } else null
        cursor.close()
        return result
    }
}
