package com.example.mfwosforeveryone

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeaponAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 💡 Intent から USER_ID を受け取る
        val currentUserId = intent.getStringExtra("USER_ID") ?: ""

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F8F8)
                ) {
                    WeaponAddScreen(
                        currentUserId = currentUserId,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeaponAddScreen(currentUserId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    // 入力状態の保持
    var weaponName by remember { mutableStateOf("") }
    var highestChoshiText by remember { mutableStateOf("0.0") }
    var categoryName by remember { mutableStateOf("シューター") }
    var bukiMemo by remember { mutableStateOf("") }
    var imageUriText by remember { mutableStateOf("manewber") }

    var asariMemo by remember { mutableStateOf("") }
    var hokoMemo by remember { mutableStateOf("") }
    var yaguraMemo by remember { mutableStateOf("") }
    var areaMemo by remember { mutableStateOf("") }

    // ドロップダウン用の状態
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf(
        "シューター", "ローラー", "チャージャー", "マニューバー", "ブラスター",
        "スロッシャー", "スピナー", "シェルター", "ストリンガー", "ワイパー"
    )

    // 画像ID安全取得
    val imageResId = remember(imageUriText) {
        val id = context.resources.getIdentifier(imageUriText, "drawable", context.packageName)
        if (id != 0) id else R.drawable.manewber
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        text = "武器情報登録",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // ② 画像プレビュー & 画像ファイル名入力フィールド
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
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
                                modifier = Modifier.size(90.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "画像名 (小文字英数)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = imageUriText,
                                onValueChange = { imageUriText = it.lowercase().trim() },
                                placeholder = { Text("例: wakaba", fontSize = 12.sp) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 右側：ボタン・タグエリア
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 武器種選択ドロップダウン
                            Box {
                                Button(
                                    onClick = { expanded = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0080FF)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "武器種選択 ⇩",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                categoryName = cat
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // #カテゴリ タグ
                            Surface(
                                color = Color(0xFFFFC0CB),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "#$categoryName",
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 武器名入力
                    OutlinedTextField(
                        value = weaponName,
                        onValueChange = { weaponName = it },
                        label = { Text("武器名を入力") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 最高チョーシ入力欄
                    OutlinedTextField(
                        value = highestChoshiText,
                        onValueChange = { highestChoshiText = it },
                        label = { Text("最高チョーシを入力 (例: 15.5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.Black, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // ③ ルール別のおすすめ立ち回り
                    Text(
                        text = "ルール別のおすすめ立ち回りを入力してください",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.Black, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ④ 各ルール用入力フォーム
                val ruleList = listOf(
                    Triple("asari", asariMemo) { text: String -> asariMemo = text },
                    Triple("area", areaMemo) { text: String -> areaMemo = text },
                    Triple("yagura", yaguraMemo) { text: String -> yaguraMemo = text },
                    Triple("hoko", hokoMemo) { text: String -> hokoMemo = text }
                )

                items(ruleList.size) { index ->
                    val (iconName, value, onValueChange) = ruleList[index]

                    val iconResId = remember(iconName) {
                        val id = context.resources.getIdentifier(iconName, "drawable", context.packageName)
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
                                    painter = painterResource(id = iconResId),
                                    contentDescription = iconName,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        TextField(
                            value = value,
                            onValueChange = onValueChange,
                            placeholder = { Text("ここに何か入力...", fontSize = 12.sp, color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFE0E0E0),
                                unfocusedContainerColor = Color(0xFFE0E0E0),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(4.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.Black, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // ⑤ 武器の説明
                    Text(
                        text = "武器の説明を入力してください",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.Black, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = bukiMemo,
                        onValueChange = { bukiMemo = it },
                        placeholder = { Text("ここに何か入力...", fontSize = 12.sp, color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE0E0E0),
                            unfocusedContainerColor = Color(0xFFE0E0E0),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }

        // ⑥ 下部固定ボタンエリア（戻る / 登録）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(130.dp)
                    .height(44.dp)
            ) {
                Text("戻る", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (weaponName.isEmpty()) {
                        Toast.makeText(context, "武器名を入力してください", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val choshiValue = highestChoshiText.toDoubleOrNull() ?: 0.0

                    coroutineScope.launch(Dispatchers.IO) {
                        // 💡 contributorId に currentUserId を渡して新規作成
                        val success = dbHelper.insertWeapon(
                            contributorId = currentUserId,
                            name = weaponName,
                            category = categoryName,
                            bukiMemo = bukiMemo,
                            imageUri = imageUriText,
                            highestChoshi = choshiValue,
                            asariMemo = asariMemo,
                            hokoMemo = hokoMemo,
                            yaguraMemo = yaguraMemo,
                            areaMemo = areaMemo
                        )

                        withContext(Dispatchers.Main) {
                            if (success) {
                                Toast.makeText(context, "武器を追加しました", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "登録に失敗しました", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(130.dp)
                    .height(44.dp)
            ) {
                Text("登録", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}