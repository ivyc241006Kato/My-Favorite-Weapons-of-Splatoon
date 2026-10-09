package com.example.mfwosforeveryone

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecommendationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F8F8)
                ) {
                    RecommendationScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
fun RecommendationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    // 画面状態の保持
    var recommendedWeaponName by remember { mutableStateOf("？？？") }
    var imageResId by remember { mutableStateOf<Int?>(null) } // 最初は null（テキスト表示）
    var isSpinning by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ① オレンジのヘッダー
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

            Spacer(modifier = Modifier.height(16.dp))

            // ② メインタイトル
            Text(
                text = "今日のあなたのオススメ武器は？",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ③ 丸いブキ画像スペース
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(Color(0xFFEFEFEF), CircleShape)
                    .border(1.dp, Color.LightGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (imageResId != null) {
                    // 💡 画像がある場合は Image コンポーザブルを表示
                    Image(
                        painter = painterResource(id = imageResId!!),
                        contentDescription = "おすすめブキ画像",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(150.dp)
                    )
                } else {
                    // 初期状態テキスト
                    Text(
                        text = "ボタンを押してね",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ④ ナレーションテキスト
            Text(
                text = "あなたの今日のおすすめは…",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ⑤ ランダムで決定された武器の名前
            Text(
                text = recommendedWeaponName,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ⑥ スタートボタン
            Button(
                onClick = {
                    if (!isSpinning) {
                        isSpinning = true
                        coroutineScope.launch {
                            var finalResult: Map<String, String>? = null

                            // 演出：パラパラとランダム取得して切り替える
                            withContext(Dispatchers.IO) {
                                repeat(12) {
                                    val randomWeapon = dbHelper.getRandomWeapon()
                                    if (randomWeapon != null) {
                                        finalResult = randomWeapon
                                        val name = randomWeapon["name"] ?: "不明なブキ"
                                        val imageName = randomWeapon["image_uri"]?.ifEmpty { "manewber" } ?: "manewber"
                                        val resId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
                                        val safeResId = if (resId != 0) resId else R.drawable.manewber

                                        withContext(Dispatchers.Main) {
                                            recommendedWeaponName = name
                                            imageResId = safeResId
                                        }
                                    }
                                    delay(80L)
                                }
                            }

                            if (finalResult == null) {
                                Toast.makeText(context, "武器データが登録されていません！", Toast.LENGTH_SHORT).show()
                                recommendedWeaponName = "わかばシューター"
                                imageResId = R.drawable.manewber
                            }
                            isSpinning = false
                        }
                    }
                },
                enabled = !isSpinning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(200.dp)
                    .height(50.dp)
            ) {
                Text(
                    text = if (isSpinning) "選出中..." else "スタート",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ⑦ 戻るボタン
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .width(100.dp)
                .height(40.dp)
        ) {
            Text(
                text = "戻る",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}