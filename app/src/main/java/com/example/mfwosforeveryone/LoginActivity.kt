package com.example.mfwosforeveryone

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F8F8)
                ) {
                    LoginScreen(
                        // 💡userId と username の両方を受け取るよう変更
                        onLoginSuccess = { userId, username ->
                            Toast.makeText(this, "ログインしました！", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java).apply {
                                putExtra("USER_ID", userId)       // 💡 正確な ID を渡す
                                putExtra("USER_NAME", username)   // 💡 表示用ユーザー名を渡す
                            }
                            startActivity(intent)
                            finish()
                        },
                        onEmptyFields = {
                            Toast.makeText(this, "ユーザー名とパスワードを入力してください", Toast.LENGTH_SHORT).show()
                        },
                        onLoginFailed = {
                            Toast.makeText(this, "ユーザー名またはパスワードが違います", Toast.LENGTH_SHORT).show()
                        },
                        // 💡 情報編集画面へ遷移する処理（userId を渡す）
                        onNavigateToEdit = { userId ->
                            val intent = Intent(this, EditAccountActivity::class.java).apply {
                                putExtra("USER_ID", userId)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (userId: String, username: String) -> Unit,
    onEmptyFields: () -> Unit,
    onLoginFailed: () -> Unit,
    onNavigateToEdit: (userId: String) -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ヘッダーバー
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ユーザー名 / ID 入力欄
            Text(
                text = "ユーザー名",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFD9D9D9),
                    unfocusedContainerColor = Color(0xFFD9D9D9),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // パスワード
            Text(
                text = "パスワード",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFD9D9D9),
                    unfocusedContainerColor = Color(0xFFD9D9D9),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // 下部ボタン
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 情報編集ボタン
                Button(
                    onClick = {
                        val trimmedUser = username.trim()
                        val trimmedPass = password.trim()

                        if (trimmedUser.isEmpty()) {
                            Toast.makeText(context, "ユーザー名を入力してください", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        coroutineScope.launch {
                            // 💡 入力されたユーザー情報から実際の user_id を照会
                            val userId = withContext(Dispatchers.IO) {
                                dbHelper.getUserIdByCredentials(trimmedUser, trimmedPass)
                            }
                            if (userId != null) {
                                onNavigateToEdit(userId)
                            } else {
                                // IDが一致しない場合は入力された文字列をそのまま渡す（新規等）
                                onNavigateToEdit(trimmedUser)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF))
                ) {
                    Text("情報編集", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // ログインボタン
                Button(
                    onClick = {
                        val trimmedUser = username.trim()
                        val trimmedPass = password.trim()

                        if (trimmedUser.isEmpty() || trimmedPass.isEmpty()) {
                            onEmptyFields()
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            // 💡 認証を行い、一致したユーザーの実際の user_id を取得
                            val loggedInUserId = withContext(Dispatchers.IO) {
                                dbHelper.getUserIdByCredentials(trimmedUser, trimmedPass)
                            }
                            isLoading = false

                            if (loggedInUserId != null) {
                                // 💡 正しい userId と username（入力値またはDB値）を渡す
                                onLoginSuccess(loggedInUserId, trimmedUser)
                            } else {
                                onLoginFailed()
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("ログイン", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}