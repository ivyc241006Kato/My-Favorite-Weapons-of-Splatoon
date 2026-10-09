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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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

class WeaponEditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val weaponId = intent.getStringExtra("WEAPON_ID") ?: "1"

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F8F8)
                ) {
                    WeaponEditScreen(weaponId = weaponId, onBack = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeaponEditScreen(weaponId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    var weaponName by remember { mutableStateOf("") }
    var highestChoshiText by remember { mutableStateOf("0.0") } // 💡 最高チョーシの状態を追加
    var categoryName by remember { mutableStateOf("シューター") }
    var bukiMemo by remember { mutableStateOf("") }
    var imageUriText by remember { mutableStateOf("manewber") }

    var asariMemo by remember { mutableStateOf("") }
    var hokoMemo by remember { mutableStateOf("") }
    var yaguraMemo by remember { mutableStateOf("") }
    var areaMemo by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("シューター", "ブラスター", "シェルター", "フデ", "ローラー", "チャージャー", "スロッシャー", "スピナー", "マニューバー", "ストリンガー", "ワイパー")

    // 初期データのロード
    LaunchedEffect(weaponId) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val weaponInfo = try { dbHelper.getWeaponDetail(weaponId) } catch (e: Exception) { emptyMap() }
            val memoMap = try { dbHelper.getBukiMemosList(weaponId) } catch (e: Exception) { emptyMap() }

            withContext(Dispatchers.Main) {
                weaponName = weaponInfo["name"] ?: ""
                highestChoshiText = weaponInfo["highest_choshi"] ?: "0.0" // 💡 最高チョーシの初期値をロード
                categoryName = weaponInfo["category"] ?: "シューター"
                bukiMemo = weaponInfo["buki_memo"] ?: ""
                imageUriText = weaponInfo["image_uri"]?.ifEmpty { "manewber" } ?: "manewber"

                asariMemo = memoMap["1"] ?: ""
                areaMemo = memoMap["2"] ?: ""
                yaguraMemo = memoMap["3"] ?: ""
                hokoMemo = memoMap["4"] ?: ""
                isLoading = false
            }
        }
    }

    val imageResId = remember(imageUriText) {
        if (imageUriText.isEmpty()) {
            R.drawable.manewber
        } else {
            val resId = context.resources.getIdentifier(imageUriText, "drawable", context.packageName)
            if (resId != 0) resId else R.drawable.manewber
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        // 武器情報編集 & 武器種選択
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "武器情報編集",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )

                            Box {
                                Button(
                                    onClick = { dropdownExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "武器種選択",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(Color.White, CircleShape)
                                                .border(1.dp, Color.LightGray, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Dropdown",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                categoryName = cat
                                                dropdownExpanded = false
                                                Toast.makeText(context, "カテゴリを「$cat」に変更しました", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 武器名入力フィールド
                        OutlinedTextField(
                            value = weaponName,
                            onValueChange = { weaponName = it },
                            label = { Text("武器名") },
                            placeholder = { Text("例: スプラシューター") },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        // 💡 最高チョーシ入力フィールドを追加
                        OutlinedTextField(
                            value = highestChoshiText,
                            onValueChange = { highestChoshiText = it },
                            label = { Text("最高チョーシ") },
                            placeholder = { Text("例: 15.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

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
                        }

                        // セパレーター：ルール別タイトル
                        HorizontalDivider(color = Color.Black, thickness = 1.dp)
                        Text(
                            text = "ルール別のおすすめ立ち回りを入れてください",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                        HorizontalDivider(color = Color.Black, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // ③ ルール別入力フィールド群
                    val editRules = listOf(
                        EditRuleItem("1", "asari", asariMemo) { asariMemo = it },
                        EditRuleItem("2", "area", areaMemo) { areaMemo = it },
                        EditRuleItem("3", "yagura", yaguraMemo) { yaguraMemo = it },
                        EditRuleItem("4", "hoko", hokoMemo) { hokoMemo = it }
                    )

                    items(editRules.size) { index ->
                        val item = editRules[index]

                        val ruleIconResId = remember(item.drawableName) {
                            val id = context.resources.getIdentifier(item.drawableName, "drawable", context.packageName)
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

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = item.textValue,
                                        onValueChange = item.onValueChange,
                                        textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { innerTextField ->
                                            if (item.textValue.isEmpty()) {
                                                Text(
                                                    text = "ここに何か入力...",
                                                    fontSize = 14.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // ④ 武器の説明入力欄
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.Black, thickness = 1.dp)
                        Text(
                            text = "武器の説明を入力してください",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                        HorizontalDivider(color = Color.Black, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                BasicTextField(
                                    value = bukiMemo,
                                    onValueChange = { bukiMemo = it },
                                    textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                                    modifier = Modifier.fillMaxSize(),
                                    decorationBox = { innerTextField ->
                                        if (bukiMemo.isEmpty()) {
                                            Text(
                                                text = "ここに何か入力...",
                                                fontSize = 14.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ⑤ 戻る & 更新ボタン
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(120.dp)
                    .height(44.dp)
            ) {
                Text(
                    text = "戻る",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    val choshiValue = highestChoshiText.toDoubleOrNull() ?: 0.0 // 💡 文字列から Double へ変換

                    coroutineScope.launch {
                        val success = withContext(Dispatchers.IO) {
                            val d1 = dbHelper.updateWeaponDetail(
                                bukiId = weaponId,
                                name = weaponName,
                                category = categoryName,
                                bukiMemo = bukiMemo,
                                imageUri = imageUriText,
                                highestChoshi = choshiValue // 💡 変換した数値を渡す
                            )
                            val m1 = dbHelper.updateBukiMemo(weaponId, "1", asariMemo)
                            val m2 = dbHelper.updateBukiMemo(weaponId, "2", areaMemo)
                            val m3 = dbHelper.updateBukiMemo(weaponId, "3", yaguraMemo)
                            val m4 = dbHelper.updateBukiMemo(weaponId, "4", hokoMemo)
                            d1 && m1 && m2 && m3 && m4
                        }
                        if (success) {
                            Toast.makeText(context, "更新が完了しました！", Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(context, "更新に失敗しました", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(120.dp)
                    .height(44.dp)
            ) {
                Text(
                    text = "更新",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class EditRuleItem(
    val ruleId: String,
    val drawableName: String,
    val textValue: String,
    val onValueChange: (String) -> Unit
)