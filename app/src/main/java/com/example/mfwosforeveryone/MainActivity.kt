package com.example.mfwosforeveryone

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    // 💡 画面復帰時の再読み込み用トリガー
    private val refreshTrigger = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 💡 LoginActivityから渡された USER_ID と USER_NAME を受け取る
        val userId = intent.getStringExtra("USER_ID") ?: ""
        val userName = intent.getStringExtra("USER_NAME") ?: "ゲスト"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F8F8)
                ) {
                    MainScreen(
                        userId = userId,
                        userName = userName,
                        refreshKey = refreshTrigger.value
                    )
                }
            }
        }
    }

    // 💡 別画面（詳細画面・編集画面）から戻ってきたときに最新DB状態へ更新
    override fun onResume() {
        super.onResume()
        refreshTrigger.value += 1
    }
}

data class Weapon(
    val id: String,
    val name: String,
    val category: String,
    val highestChoshi: Double,
    val imageUri: String,
    val color: Color = Color(0xFF90EE90) // デフォルト緑色
)

@Composable
fun MainScreen(userId: String, userName: String, refreshKey: Int) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }

    var weaponList by remember { mutableStateOf<List<Weapon>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) } // 💡 フィルター選択用
    var isLoading by remember { mutableStateOf(true) }

    // フィルター用の武器種別リスト
    val filters = listOf(
        "すべて", "シューター", "ローラー", "チャージャー", "マニューバー", "ブラスター",
        "スロッシャー", "スピナー", "シェルター", "ストリンガー", "ワイパー"
    )

    // 💡 ログインユーザー固有のブキリストをDBから動的に取得
    LaunchedEffect(refreshKey, userId) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val rawList = if (userId.isNotEmpty()) {
                dbHelper.getAllWeaponsByUserId(userId)
            } else {
                dbHelper.getAllWeapons() // IDが無い場合は全体表示（バックアップ動作）
            }
            val list = rawList.map { map ->
                Weapon(
                    id = map["id"] as? String ?: "",
                    name = map["name"] as? String ?: "名称未設定",
                    category = map["category"] as? String ?: "未設定",
                    highestChoshi = map["highestChoshi"] as? Double ?: 0.0,
                    imageUri = map["imageUri"] as? String ?: "manewber"
                )
            }
            withContext(Dispatchers.Main) {
                weaponList = list
                isLoading = false
            }
        }
    }

    // 💡 選択されたカテゴリで絞り込み処理
    val displayList = remember(weaponList, selectedCategory) {
        if (selectedCategory == null || selectedCategory == "すべて") {
            weaponList
        } else {
            weaponList.filter { it.category == selectedCategory }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ① ヘッダーバー
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFFFF9800)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Splatoon Weapons list",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // メインコンテンツ
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // ② 武器種別フィルター
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "武器種別フィルター", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filters.forEach { filter ->
                            val isSelected = (selectedCategory == filter) || (selectedCategory == null && filter == "すべて")
                            Card(
                                onClick = {
                                    selectedCategory = if (filter == "すべて") null else filter
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFFF80AB) else Color(0xFFFFC0CB)
                                ),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFD9D9D9), thickness = 2.dp)
                }

                // ③ 今日のオススメ武器
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "今日のオススメ武器", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val intent = Intent(context, RecommendationActivity::class.java).apply {
                                putExtra("USER_ID", userId)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("今日のオススメを見てみる", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.Black, thickness = 2.dp)
                }

                // ④ 武器一覧タイトル（アカウント編集ボタン付き）
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "武器一覧 (ログイン中: $userName)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )


                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ⑤ ブキのリスト表示（DBから動的に生成）
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFF9800))
                        }
                    }
                } else if (displayList.isEmpty()) {
                    item {
                        Text(
                            text = "登録された武器がありません。「＋」ボタンから追加してください。",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                } else {
                    items(displayList) { weapon ->
                        val imageResId = remember(weapon.imageUri) {
                            val id = context.resources.getIdentifier(weapon.imageUri, "drawable", context.packageName)
                            if (id != 0) id else R.drawable.manewber
                        }

                        Card(
                            onClick = {
                                val intent = Intent(context, WeaponDetailActivity::class.java).apply {
                                    putExtra("WEAPON_ID", weapon.id)
                                    putExtra("WEAPON_NAME", weapon.name)
                                    putExtra("USER_ID", userId)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(containerColor = weapon.color),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左側：画像表示エリア
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = imageResId),
                                        contentDescription = weapon.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(60.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // 中央の区切り線
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(80.dp)
                                        .background(Color.White)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // 右側：ブキ詳細テキスト
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = weapon.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("ブキ種：", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Surface(color = Color.White, shape = RoundedCornerShape(2.dp)) {
                                            Text(
                                                text = " ${weapon.category} ",
                                                fontSize = 14.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("最高チョーシ：", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Surface(color = Color.White, shape = RoundedCornerShape(2.dp)) {
                                            Text(
                                                text = "  ${weapon.highestChoshi}  ",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 💡 ログアウトボタン（クラッシュ対策済み）
        Button(
            onClick = {
                val activity = context as? Activity
                val intent = Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                activity?.finish()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("ログアウト", color = Color.White, fontWeight = FontWeight.Bold)
        }

        // 右下の「＋」ボタン（新規武器追加）
        Button(
            onClick = {
                val intent = Intent(context, WeaponAddActivity::class.java).apply {
                    putExtra("USER_ID", userId)
                }
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp)
        ) {
            Text(
                text = "+",
                color = Color.White,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = (-2).dp)
            )
        }
    }
}