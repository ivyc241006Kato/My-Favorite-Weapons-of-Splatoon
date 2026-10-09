package com.example.mfwosforeveryone

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class EditAccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 前画面から渡されたユーザーIDを取得（デフォルトは仮のID）
        val currentUserId = intent.getStringExtra("USER_ID") ?: ""

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    EditAccountScreen(
                        currentUserId = currentUserId,
                        onUpdateSuccess = {
                            Toast.makeText(this, "アカウント情報を更新しました", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onUpdateFailed = {
                            Toast.makeText(this, "更新に失敗しました", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EditAccountScreen(
    currentUserId: String,
    onUpdateSuccess: () -> Unit,
    onUpdateFailed: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // 💡 アカウント削除確認ダイアログの状態管理
    var showDeleteUserDialog by remember { mutableStateOf(false) }

    // 画面表示時に初期データ（現在のユーザー情報）を取得してフィールドにセット
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            val user = withContext(Dispatchers.IO) { dbHelper.getUser(currentUserId) }
            user?.let {
                username = it["name"] ?: ""
                password = it["pass"] ?: ""
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ヘッダーバー（オレンジ）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color(0xFFF57C00)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Splatoon Weapons list",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // メイン入力フォーム部分
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // ユーザー名ラベル & 入力欄
                Text(
                    text = "ユーザー名",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFE0E0E0),
                        unfocusedContainerColor = Color(0xFFE0E0E0),
                        disabledContainerColor = Color(0xFFE0E0E0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(2.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(28.dp))

                // パスワードラベル & 入力欄
                Text(
                    text = "パスワード",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFE0E0E0),
                        unfocusedContainerColor = Color(0xFFE0E0E0),
                        disabledContainerColor = Color(0xFFE0E0E0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(2.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(40.dp))

                // 更新ボタン（右寄せ）
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(
                        onClick = {
                            val trimmedUser = username.trim()
                            val trimmedPass = password.trim()

                            if (trimmedUser.isEmpty() || trimmedPass.isEmpty()) {
                                Toast.makeText(context, "入力項目をすべて入力してください", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true
                            coroutineScope.launch {
                                val isSuccess = withContext(Dispatchers.IO) {
                                    dbHelper.updateUser(currentUserId, trimmedUser, trimmedPass)
                                }
                                isLoading = false
                                if (isSuccess) {
                                    onUpdateSuccess()
                                } else {
                                    onUpdateFailed()
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
                        modifier = Modifier
                            .width(100.dp)
                            .height(44.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "更新",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 💡 アカウント削除（退会）ボタン
                Button(
                    onClick = { showDeleteUserDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        text = "アカウントを削除",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 右上のユーザーアイコン
        Box(
            modifier = Modifier
                .padding(top = 42.dp, end = 16.dp)
                .align(Alignment.TopEnd)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF7E57C2)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (username.isNotEmpty()) username.take(1) else "潤",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 💡 アカウント削除確認ダイアログ
        if (showDeleteUserDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteUserDialog = false },
                title = { Text("アカウントの削除") },
                text = { Text("本当にアカウントを削除しますか？この操作は取り消せません。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteUserDialog = false
                            coroutineScope.launch {
                                val isSuccess = withContext(Dispatchers.IO) {
                                    dbHelper.deleteUser(currentUserId)
                                }
                                if (isSuccess) {
                                    Toast.makeText(context, "アカウントを削除しました", Toast.LENGTH_SHORT).show()

                                    // 💡 ログイン画面へ遷移＆画面履歴を消去
                                    val intent = Intent(context, LoginActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                    context.startActivity(intent)
                                    (context as? Activity)?.finish()
                                } else {
                                    Toast.makeText(context, "削除に失敗しました", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text("削除する", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteUserDialog = false }) {
                        Text("キャンセル")
                    }
                }
            )
        }
    }
}