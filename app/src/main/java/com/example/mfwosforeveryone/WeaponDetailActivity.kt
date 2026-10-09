package com.example.mfwosforeveryone

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeaponDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val weaponId = intent.getStringExtra("WEAPON_ID") ?: "1"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F8F8)
                ) {
                    WeaponDetailScreen(weaponId = weaponId, onBack = { finish() })
                }
            }
        }
    }
}

@Composable
fun WeaponDetailScreen(weaponId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    var weaponInfo by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var memoMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var imageResId by remember { mutableStateOf(R.drawable.manewber) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 💡 お気に入り状態の保持
    var isFavorite by remember { mutableStateOf(false) }

    var refreshTrigger by remember { mutableStateOf(0) }

    // 全てのデータ処理・リソース検索を IO スレッドで行う
    LaunchedEffect(weaponId, refreshTrigger) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val infoResult = try { dbHelper.getWeaponDetail(weaponId) } catch (e: Exception) { emptyMap() }
            val memoResult = try { dbHelper.getBukiMemosList(weaponId) } catch (e: Exception) { emptyMap() }

            val imageName = infoResult["image_uri"]?.ifEmpty { "squid" } ?: "squid"
            val resId = try {
                val id = context.resources.getIdentifier(imageName, "drawable", context.packageName)
                if (id != 0) id else R.drawable.manewber
            } catch (e: Exception) {
                R.drawable.manewber
            }

            withContext(Dispatchers.Main) {
                weaponInfo = infoResult
                memoMap = memoResult
                imageResId = resId
                // 💡 DBから取得した is_favorite の値 ("1" なら true) を反映
                isFavorite = (infoResult["is_favorite"] ?: "0") == "1"
                isLoading = false
            }
        }
    }

    val editLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        refreshTrigger++
    }

    val bukiName = if (weaponInfo.isEmpty() || !weaponInfo.containsKey("name")) {
        if (weaponId == "1") "わかばシューター" else "スプラシューター"
    } else {
        weaponInfo["name"] ?: "不明なブキ"
    }
    val categoryName = weaponInfo["category"] ?: "シューター"
    val highestChoshi = weaponInfo["highest_choshi"] ?: "0.0"
    val bukiMemo = weaponInfo["buki_memo"] ?: "説明はありません。"

    val asariMemo = memoMap["1"]?.ifEmpty { "メモなし" } ?: "メモなし"
    val areaMemo = memoMap["2"]?.ifEmpty { "メモなし" } ?: "メモなし"
    val yaguraMemo = memoMap["3"]?.ifEmpty { "メモなし" } ?: "メモなし"
    val hokoMemo = memoMap["4"]?.ifEmpty { "メモなし" } ?: "メモなし"

    data class RuleItem(val drawableName: String, val memo: String)

    val ruleList = listOf(
        RuleItem("asari", asariMemo),
        RuleItem("area", areaMemo),
        RuleItem("yagura", yaguraMemo),
        RuleItem("hoko", hokoMemo)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF9800))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {

                // ① ヘッダー
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
                ) {
                    item {
                        Text(
                            text = "武器詳細",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // ② 画像・カテゴリ・最高チョーシ・ボタン
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .background(Color(0xFFEFEFEF), CircleShape)
                                    .border(1.dp, Color.LightGray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = imageResId),
                                    contentDescription = "ブキ画像",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(100.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                // 💡 最高チョーシ表示
                                Surface(
                                    color = Color(0xFFFFF3CD),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = "★ 最高: $highestChoshi",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF856404)
                                    )
                                }

                                Surface(
                                    color = Color(0xFFFFC0CB),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Text(
                                        text = "#$categoryName",
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }

                                // 削除ボタン
                                Button(
                                    onClick = { showDeleteDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.width(100.dp).height(36.dp)
                                ) {
                                    Text("削除", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // 編集ボタン
                                Button(
                                    onClick = {
                                        val intent = Intent(context, WeaponEditActivity::class.java).apply {
                                            putExtra("WEAPON_ID", weaponId)
                                        }
                                        editLauncher.launch(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.width(100.dp).height(36.dp)
                                ) {
                                    Text("編集", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.Black, thickness = 1.dp)

                        // ③ ブキ名とお気に入りボタン
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bukiName,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // 💡 お気に入り★切り替えボタン
                            IconButton(
                                onClick = {
                                    val nextState = !isFavorite
                                    isFavorite = nextState
                                    coroutineScope.launch(Dispatchers.IO) {
                                        dbHelper.updateFavoriteStatus(weaponId, nextState)
                                    }
                                    val msg = if (nextState) "お気に入りに登録しました" else "お気に入りを解除しました"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = "お気に入りボタン",
                                    tint = if (isFavorite) Color(0xFFFFC107) else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.Black, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // ④ 武器の説明
                        Text(
                            text = "武器の説明",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8FCE8)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = bukiMemo,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "ルール別おすすめ立ち回り",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ⑤ ルール一覧
                    items(ruleList) { rule ->
                        val ruleIconResId = remember(rule.drawableName) {
                            val id = context.resources.getIdentifier(rule.drawableName, "drawable", context.packageName)
                            if (id != 0) id else R.drawable.manewber
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Image(
                                        painter = painterResource(id = ruleIconResId),
                                        contentDescription = "ルール画像",
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                color = Color(0xFFE8FCE8),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = rule.memo,
                                        fontSize = 12.sp,
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

        // ⑥ 戻るボタン
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .width(120.dp)
                .height(44.dp)
        ) {
            Text("戻る", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // 削除確認ダイアログ
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("武器の削除") },
                text = { Text("この武器を削除してもよろしいですか？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            coroutineScope.launch {
                                val isSuccess = withContext(Dispatchers.IO) {
                                    dbHelper.deleteWeapon(weaponId)
                                }
                                if (isSuccess) {
                                    Toast.makeText(context, "削除しました", Toast.LENGTH_SHORT).show()
                                    (context as? Activity)?.finish()
                                } else {
                                    Toast.makeText(context, "削除に失敗しました", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("削除", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("キャンセル")
                    }
                }
            )
        }
    }
}