package com.djs66256.short_drama.feature.mall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MallLoginScreen(
    productId: String,
    onClose: () -> Unit,
    onCancel: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "商城登录承接",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (productId.isBlank()) {
                "当前商品信息缺失，请关闭后返回商城。"
            } else {
                "请先登录后继续查看商品。当前商品 ID：$productId"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSuccess) {
            Text("模拟登录成功")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onCancel) {
            Text("取消")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onClose) {
            Text("关闭")
        }
    }
}
