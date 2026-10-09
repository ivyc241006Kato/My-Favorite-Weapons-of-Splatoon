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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F8F8)
                ) {
                    RegisterScreen(
                        onRegisterSuccess = { username ->
                            Toast.makeText(this, "${username}さん、登録が完了しました！", Toast.LENGTH_SHORT).show()
                            val generatedUserId = UUID.randomUUID().toString()
                            val intent = Intent(this, MainActivity::class.java).apply {
                                putExtra("USER_ID", generatedUserId)
                                putExtra("USER_NAME", username)
                            }
                            startActivity(intent)
                            finish()
                        },
                        onEmptyFields = {
                            Toast.makeText(this, "ユーザー名とパスワードを入力してください", Toast.LENGTH_SHORT).show()
                        },
                        onPasswordTooShort = {
                            Toast.makeText(this, "パスワードは6文字以上で入力してください", Toast.LENGTH_SHORT).show()
                        },
                        onRegisterFailed = {
                            Toast.makeText(this, "データベースへの保存に失敗しました", Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToLogin = {
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onRegisterSuccess: (String) -> Unit,
    onEmptyFields: () -> Unit,
    onPasswordTooShort: () -> Unit,
    onRegisterFailed: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val coroutineScope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFFFF9800)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Splatoon Weapons list", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "アカウント登録", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("ユーザー名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("パスワード") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val trimmedUser = username.trim()
                    val trimmedPass = password.trim()

                    if (trimmedUser.isEmpty() || trimmedPass.isEmpty()) {
                        onEmptyFields()
                    } else if (trimmedPass.length < 6) {
                        onPasswordTooShort()
                    } else {
                        isLoading = true
                        // 💡 データベースへ登録（ID, 名前, パスワード, メールアドレス）を書き込む
                        coroutineScope.launch {
                            val isSaved = withContext(Dispatchers.IO) {
                                dbHelper.insertUser(
                                    id = trimmedUser,
                                    name = trimmedUser,
                                    pass = trimmedPass,
                                    email = ""
                                )
                            }
                            isLoading = false

                            if (isSaved) {
                                onRegisterSuccess(trimmedUser)
                            } else {
                                onRegisterFailed()
                            }
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0080FF)),
                modifier = Modifier.align(Alignment.End)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("登録", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "すでにアカウントをお持ちの方（ログインはこちら）",
                    color = Color(0xFF0080FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}