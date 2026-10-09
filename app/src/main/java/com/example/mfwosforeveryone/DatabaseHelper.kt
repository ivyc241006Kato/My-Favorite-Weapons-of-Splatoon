package com.example.mfwosforeveryone

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "splatoon_weapon.db"
        private const val DATABASE_VERSION = 7 // 💡 お気に入り（is_favorite）カラム追加に対応
    }

    // テーブルの作成
    override fun onCreate(db: SQLiteDatabase) {
        // ① ユーザーテーブル
        db.execSQL("""
            CREATE TABLE user_table (
                user_id TEXT PRIMARY KEY,
                user_name TEXT,
                password TEXT,
                email TEXT
            )
        """.trimIndent())

        // ② 武器テーブル (contributor_id で所有ユーザーを管理)
        db.execSQL("""
            CREATE TABLE buki_table (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                contributor_id TEXT,
                name TEXT,
                category TEXT,
                highest_choshi REAL,
                buki_memo TEXT,
                image_uri TEXT,
                is_favorite INTEGER DEFAULT 0
            )
        """.trimIndent())

        // ③ ルールテーブル
        db.execSQL("""
            CREATE TABLE rule_table (
                rule_id TEXT PRIMARY KEY,
                rule_name TEXT
            )
        """.trimIndent())

        // ④ 中間テーブル
        db.execSQL("""
            CREATE TABLE buki_junction_table (
                junction_id INTEGER PRIMARY KEY AUTOINCREMENT,
                buki_id TEXT,
                rule_id TEXT,
                memo_text TEXT
            )
        """.trimIndent())

        // 初期データ（4つのルール）を自動登録
        val rules = listOf("1" to "ナワバリバトル", "2" to "ガチエリア", "3" to "ガチヤグラ", "4" to "ガチホコバトル")
        for (rule in rules) {
            val values = ContentValues().apply {
                put("rule_id", rule.first)
                put("rule_name", rule.second)
            }
            db.insert("rule_table", null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE buki_table ADD COLUMN is_favorite INTEGER DEFAULT 0")
            } catch (e: Exception) {
                db.execSQL("DROP TABLE IF EXISTS user_table")
                db.execSQL("DROP TABLE IF EXISTS buki_table")
                db.execSQL("DROP TABLE IF EXISTS rule_table")
                db.execSQL("DROP TABLE IF EXISTS buki_junction_table")
                onCreate(db)
            }
        }
    }

    // 💡 ユーザー登録用の関数
    fun insertUser(id: String, name: String, pass: String, email: String): Boolean {
        return try {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put("user_id", id)
                put("user_name", name)
                put("password", pass)
                put("email", email)
            }

            val result = db.insert("user_table", null, values)
            result != -1L
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "insertUserエラー: ${e.message}", e)
            false
        }
    }

    // 💡 ユーザー存在チェック関数
    fun checkUserExists(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM user_table WHERE (user_id = ? OR user_name = ?) AND password = ?"
        val cursor = db.rawQuery(query, arrayOf(username, username, password))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // 💡 ログイン認証時に該当するユーザーの正確な user_id を取得する関数
    fun getUserIdByCredentials(username: String, password: String): String? {
        val db = this.readableDatabase
        val query = "SELECT user_id FROM user_table WHERE (user_id = ? OR user_name = ?) AND password = ?"
        val cursor = db.rawQuery(query, arrayOf(username, username, password))
        var userId: String? = null
        if (cursor.moveToFirst()) {
            userId = cursor.getString(0)
        }
        cursor.close()
        return userId
    }

    // ⭕ 武器の基本情報を1件取得する関数
    fun getWeaponDetail(bukiId: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var db: SQLiteDatabase? = null
        try {
            db = this.readableDatabase
            val cursor = db.rawQuery("SELECT * FROM buki_table WHERE id = ?", arrayOf(bukiId))
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex("name")
                val categoryIndex = cursor.getColumnIndex("category")
                val bukiMemoIndex = cursor.getColumnIndex("buki_memo")
                val highestChoshiIndex = cursor.getColumnIndex("highest_choshi")
                val imageUriIndex = cursor.getColumnIndex("image_uri")
                val isFavoriteIndex = cursor.getColumnIndex("is_favorite")

                if (nameIndex != -1) result["name"] = cursor.getString(nameIndex) ?: ""
                if (categoryIndex != -1) result["category"] = cursor.getString(categoryIndex) ?: ""
                if (bukiMemoIndex != -1) result["buki_memo"] = cursor.getString(bukiMemoIndex) ?: ""
                if (highestChoshiIndex != -1) result["highest_choshi"] = cursor.getDouble(highestChoshiIndex).toString()
                if (imageUriIndex != -1) result["image_uri"] = cursor.getString(imageUriIndex) ?: ""
                if (isFavoriteIndex != -1) result["is_favorite"] = cursor.getInt(isFavoriteIndex).toString()

                cursor.close()
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "getWeaponDetailエラー: ${e.message}", e)
        }
        return result
    }

    // ⭕ 中間テーブルからルールのメモをまとめて取得する関数
    fun getBukiMemosList(bukiId: String): Map<String, String> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("""
            SELECT j.rule_id, j.memo_text 
            FROM buki_junction_table j 
            WHERE j.buki_id = ?
        """.trimIndent(), arrayOf(bukiId))

        val memoMap = mutableMapOf("1" to "", "2" to "", "3" to "", "4" to "")
        while (cursor.moveToNext()) {
            val ruleId = cursor.getString(0)
            val memoText = cursor.getString(1)
            memoMap[ruleId] = memoText
        }
        cursor.close()
        return memoMap
    }

    // 💡 ユーザーごとのランダムおすすめ武器を取得する関数
    fun getRandomWeaponByUserId(userId: String): Map<String, String>? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, name, category, buki_memo, image_uri, highest_choshi, is_favorite FROM buki_table WHERE contributor_id = ? ORDER BY RANDOM() LIMIT 1",
            arrayOf(userId)
        )
        var result: Map<String, String>? = null

        if (cursor.moveToFirst()) {
            result = mapOf(
                "id" to (cursor.getString(0) ?: ""),
                "name" to (cursor.getString(1) ?: ""),
                "category" to (cursor.getString(2) ?: ""),
                "buki_memo" to (cursor.getString(3) ?: ""),
                "image_uri" to (cursor.getString(4) ?: ""),
                "highest_choshi" to cursor.getDouble(5).toString(),
                "is_favorite" to cursor.getInt(6).toString()
            )
        }
        cursor.close()
        return result
    }

    // 💡 武器テーブルからランダムに1件取得する（従来版）
    fun getRandomWeapon(): Map<String, String>? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT id, name, category, buki_memo, image_uri, highest_choshi, is_favorite FROM buki_table ORDER BY RANDOM() LIMIT 1", null)
        var result: Map<String, String>? = null

        if (cursor.moveToFirst()) {
            result = mapOf(
                "id" to (cursor.getString(0) ?: ""),
                "name" to (cursor.getString(1) ?: ""),
                "category" to (cursor.getString(2) ?: ""),
                "buki_memo" to (cursor.getString(3) ?: ""),
                "image_uri" to (cursor.getString(4) ?: ""),
                "highest_choshi" to cursor.getDouble(5).toString(),
                "is_favorite" to cursor.getInt(6).toString()
            )
        }
        cursor.close()
        return result
    }

    // 💡 武器の基本情報を更新（データが無ければ新規追加）
    fun updateWeaponDetail(
        bukiId: String,
        name: String,
        category: String,
        bukiMemo: String,
        imageUri: String,
        highestChoshi: Double
    ): Boolean {
        return try {
            val db = this.writableDatabase

            val cursor = db.rawQuery("SELECT id FROM buki_table WHERE id = ?", arrayOf(bukiId))
            val exists = cursor.moveToFirst()
            cursor.close()

            val values = ContentValues().apply {
                put("id", bukiId)
                put("name", name)
                put("category", category)
                put("buki_memo", bukiMemo)
                put("image_uri", imageUri)
                put("highest_choshi", highestChoshi)
            }

            val result = if (exists) {
                db.update("buki_table", values, "id = ?", arrayOf(bukiId)).toLong()
            } else {
                db.insert("buki_table", null, values)
            }

            result > 0 || result != -1L
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "updateWeaponDetailエラー: ${e.message}", e)
            false
        }
    }

    // 💡 ルール別の立ち回りメモを中間テーブルに保存・更新する
    fun updateBukiMemo(bukiId: String, ruleId: String, memoText: String): Boolean {
        return try {
            val db = this.writableDatabase
            val cursor = db.rawQuery(
                "SELECT junction_id FROM buki_junction_table WHERE buki_id = ? AND rule_id = ?",
                arrayOf(bukiId, ruleId)
            )
            val exists = cursor.moveToFirst()
            cursor.close()

            val values = ContentValues().apply {
                put("buki_id", bukiId)
                put("rule_id", ruleId)
                put("memo_text", memoText)
            }

            val result = if (exists) {
                db.update("buki_junction_table", values, "buki_id = ? AND rule_id = ?", arrayOf(bukiId, ruleId)).toLong()
            } else {
                db.insert("buki_junction_table", null, values)
            }
            result != -1L
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "updateBukiMemoエラー: ${e.message}", e)
            false
        }
    }

    // 💡 お気に入り状態の更新 (1: お気に入り, 0: 未登録)
    fun updateFavoriteStatus(bukiId: String, isFavorite: Boolean): Boolean {
        return try {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put("is_favorite", if (isFavorite) 1 else 0)
            }
            val result = db.update("buki_table", values, "id = ?", arrayOf(bukiId))
            result > 0
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "updateFavoriteStatusエラー: ${e.message}", e)
            false
        }
    }

    // 💡 ログインユーザーごとの武器一覧を取得する関数（★新設）
    fun getAllWeaponsByUserId(userId: String): List<Map<String, Any>> {
        val weaponList = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        var cursor: android.database.Cursor? = null

        try {
            cursor = db.rawQuery(
                "SELECT id, name, category, highest_choshi, image_uri, is_favorite FROM buki_table WHERE contributor_id = ?",
                arrayOf(userId)
            )
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val item = mapOf(
                        "id" to (cursor.getString(0) ?: ""),
                        "name" to (cursor.getString(1) ?: ""),
                        "category" to (cursor.getString(2) ?: ""),
                        "highestChoshi" to cursor.getDouble(3),
                        "imageUri" to (cursor.getString(4) ?: ""),
                        "isFavorite" to (cursor.getInt(5) == 1)
                    )
                    weaponList.add(item)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "getAllWeaponsByUserIdエラー: ${e.message}", e)
        } finally {
            cursor?.close()
        }
        return weaponList
    }

    // 💡 全ての武器一覧を取得する関数（従来互換用）
    fun getAllWeapons(): List<Map<String, Any>> {
        val weaponList = mutableListOf<Map<String, Any>>()
        val db = this.readableDatabase
        var cursor: android.database.Cursor? = null

        try {
            cursor = db.rawQuery("SELECT id, name, category, highest_choshi, image_uri, is_favorite FROM buki_table", null)
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val item = mapOf(
                        "id" to (cursor.getString(0) ?: ""),
                        "name" to (cursor.getString(1) ?: ""),
                        "category" to (cursor.getString(2) ?: ""),
                        "highestChoshi" to cursor.getDouble(3),
                        "imageUri" to (cursor.getString(4) ?: ""),
                        "isFavorite" to (cursor.getInt(5) == 1)
                    )
                    weaponList.add(item)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "getAllWeaponsエラー: ${e.message}", e)
        } finally {
            cursor?.close()
        }
        return weaponList
    }

    // 💡 新規武器を追加するメソッド（contributorId 対応版★）
    fun insertWeapon(
        contributorId: String = "",
        name: String,
        category: String,
        bukiMemo: String,
        imageUri: String,
        highestChoshi: Double,
        asariMemo: String,
        hokoMemo: String,
        yaguraMemo: String,
        areaMemo: String
    ): Boolean {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            val bukiValues = ContentValues().apply {
                put("contributor_id", contributorId) // 💡 ログインユーザーIDを設定
                put("name", name.ifEmpty { "新規ブキ" })
                put("category", category.ifEmpty { "シューター" })
                put("buki_memo", bukiMemo)
                put("highest_choshi", highestChoshi)
                put("image_uri", imageUri.ifEmpty { "manewber" })
                put("is_favorite", 0)
            }
            val newBukiId = db.insert("buki_table", null, bukiValues)

            if (newBukiId == -1L) return false

            val memos = listOf(
                Pair("1", asariMemo),
                Pair("2", areaMemo),
                Pair("3", yaguraMemo),
                Pair("4", hokoMemo)
            )

            for ((ruleId, memoText) in memos) {
                val memoValues = ContentValues().apply {
                    put("buki_id", newBukiId.toString())
                    put("rule_id", ruleId)
                    put("memo_text", memoText)
                }
                db.insert("buki_junction_table", null, memoValues)
            }

            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            db.endTransaction()
        }
    }

    // 💡 ルールIDやルール名からdrawableのリソースIDを安全に取得する関数
    fun getRuleIconResId(context: Context, ruleKey: String): Int {
        val drawableName = when (ruleKey) {
            "1", "asari", "アサリ" -> "asari"
            "2", "area", "ガチエリア" -> "area"
            "3", "yagura", "ガチヤグラ" -> "yagura"
            "4", "hoko", "ガチホコ" -> "hoko"
            else -> "manewber"
        }
        val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
        return if (resId != 0) resId else R.drawable.manewber
    }

    // 💡 ユーザー情報を更新する関数
    fun updateUser(currentUserId: String, newName: String, newPass: String): Boolean {
        return try {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put("user_name", newName)
                put("password", newPass)
            }
            val result = db.update("user_table", values, "user_id = ?", arrayOf(currentUserId))
            result > 0
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "updateUserエラー: ${e.message}", e)
            false
        }
    }

    // 💡 指定されたユーザーIDの情報を取得する関数
    fun getUser(userId: String): Map<String, String>? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT user_id, user_name, password FROM user_table WHERE user_id = ? OR user_name = ?", arrayOf(userId, userId))
        var userMap: Map<String, String>? = null

        if (cursor.moveToFirst()) {
            userMap = mapOf(
                "id" to (cursor.getString(0) ?: ""),
                "name" to (cursor.getString(1) ?: ""),
                "pass" to (cursor.getString(2) ?: "")
            )
        }
        cursor.close()
        return userMap
    }

    // 💡 指定した武器IDのデータを削除する関数
    fun deleteWeapon(bukiId: String): Boolean {
        val db = this.writableDatabase
        db.beginTransaction()
        return try {
            db.delete("buki_junction_table", "buki_id = ?", arrayOf(bukiId))
            val deletedRows = db.delete("buki_table", "id = ?", arrayOf(bukiId))
            db.setTransactionSuccessful()
            deletedRows > 0
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "deleteWeaponエラー: ${e.message}", e)
            false
        } finally {
            db.endTransaction()
        }
    }

    // 💡 ユーザーアカウント削除時に、該当ユーザーの所有ブキもまとめて削除（連動削除）
    fun deleteUser(userId: String): Boolean {
        val db = this.writableDatabase
        db.beginTransaction()
        return try {
            // 対象ユーザーが作成したブキID一覧を取得して中間テーブルのメモを削除
            val cursor = db.rawQuery("SELECT id FROM buki_table WHERE contributor_id = ?", arrayOf(userId))
            while (cursor.moveToNext()) {
                val bukiId = cursor.getString(0)
                db.delete("buki_junction_table", "buki_id = ?", arrayOf(bukiId))
            }
            cursor.close()

            // 該当ユーザーのブキデータ＆アカウント本体を削除
            db.delete("buki_table", "contributor_id = ?", arrayOf(userId))
            val deletedRows = db.delete("user_table", "user_id = ?", arrayOf(userId))
            db.setTransactionSuccessful()
            deletedRows > 0
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "deleteUserエラー: ${e.message}", e)
            false
        } finally {
            db.endTransaction()
        }
    }
}