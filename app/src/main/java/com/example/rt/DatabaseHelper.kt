package com.example.rt

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "RTDatabase.db"  // Nama database baru
        private const val DATABASE_VERSION = 1  // Versi pertama dari database

        private const val TABLE_USERS = "users"
        private const val COLUMN_USER_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_ROLE = "role"  // Kolom tambahan untuk peran

        private const val TABLE_TEMPLATES = "templates"
        private const val COLUMN_TEMPLATE_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_FILE = "file"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Buat tabel users dengan kolom role
        val createUserTable = ("CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_USERNAME TEXT, " +
                "$COLUMN_EMAIL TEXT, " +
                "$COLUMN_PASSWORD TEXT, " +
                "$COLUMN_ROLE TEXT)")  // Kolom role sudah termasuk dalam versi awal
        db.execSQL(createUserTable)

        // Buat tabel templates untuk menyimpan template surat
        val createTemplateTable = ("CREATE TABLE $TABLE_TEMPLATES (" +
                "$COLUMN_TEMPLATE_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_TITLE TEXT, " +
                "$COLUMN_FILE BLOB)")
        db.execSQL(createTemplateTable)
    }

    // Implementasi metode onUpgrade yang kosong
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Tidak ada update schema yang diperlukan untuk versi 1
        // Implementasi kosong karena tidak ada upgrade yang perlu dilakukan
    }

    // Fungsi untuk menambahkan user baru dengan validasi role
    fun addUser(username: String, email: String, password: String, role: String): Boolean {
        // Validasi role sebelum memasukkan ke database
        if (role != "Warga" && role != "Ketua RT") {
            Log.e("DatabaseHelper", "Invalid role: $role")
            return false
        }

        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_ROLE, role)  // Simpan role yang dipilih
        }
        val result = db.insert(TABLE_USERS, null, values)
        db.close()
        return result != -1L
    }

    // Fungsi untuk memeriksa apakah user ada saat login
    fun getUser(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(username, password))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    // Fungsi untuk mendapatkan role user berdasarkan username dan password
    fun getUserRole(username: String, password: String): String? {
        val db = this.readableDatabase
        val query = "SELECT $COLUMN_ROLE FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(username, password))
        var role: String? = null
        if (cursor.moveToFirst()) {
            role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE))
        }
        cursor.close()
        db.close()
        return role
    }

    // Fungsi untuk mendapatkan role user hanya berdasarkan username
    fun getUserRoleByUsername(username: String): String? {
        val db = this.readableDatabase
        val query = "SELECT $COLUMN_ROLE FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?"
        val cursor = db.rawQuery(query, arrayOf(username))
        var role: String? = null
        if (cursor.moveToFirst()) {
            role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE))
        }
        cursor.close()
        db.close()
        return role
    }

    // Fungsi untuk mengecek apakah user adalah warga
    fun isUserWarga(username: String): Boolean {
        return getUserRoleByUsername(username) == "Warga"
    }

    // Fungsi untuk mengecek apakah user adalah ketua RT
    fun isUserKetuaRT(username: String): Boolean {
        return getUserRoleByUsername(username) == "Ketua RT"
    }

    // Fungsi untuk menambahkan template surat ke database
    fun addTemplate(title: String, file: ByteArray): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_FILE, file)
        }
        val result = db.insert(TABLE_TEMPLATES, null, values)
        db.close()
        return result != -1L
    }

    // Fungsi untuk mendapatkan semua template
    fun getAllTemplates(): List<Pair<String, ByteArray>> {
        val templates = mutableListOf<Pair<String, ByteArray>>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_TEMPLATES", null)

        val titleIndex = cursor.getColumnIndex(COLUMN_TITLE)
        val fileIndex = cursor.getColumnIndex(COLUMN_FILE)

        if (titleIndex == -1 || fileIndex == -1) {
            Log.e("DatabaseHelper", "Column index not found. Make sure the column names are correct.")
            cursor.close()
            db.close()
            return templates
        }

        if (cursor.moveToFirst()) {
            do {
                val title = cursor.getString(titleIndex)
                val file = cursor.getBlob(fileIndex)
                templates.add(Pair(title, file))
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return templates
    }

    // Fungsi untuk menghapus template berdasarkan title
    fun deleteTemplate(title: String): Boolean {
        val db = this.writableDatabase
        val result = db.delete(TABLE_TEMPLATES, "$COLUMN_TITLE = ?", arrayOf(title))
        db.close()
        return result > 0
    }

    // Fungsi untuk memeriksa apakah username sudah ada di database
    fun isUsernameExists(username: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?"
        val cursor = db.rawQuery(query, arrayOf(username))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    // Fungsi untuk memeriksa apakah email sudah ada di database
    fun isEmailExists(email: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(email))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

}