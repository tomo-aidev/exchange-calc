package com.exchangecalc.app.ui.paywall

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exchangecalc.app.R
import com.exchangecalc.app.ui.theme.Primary
import com.exchangecalc.app.util.PurchaseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    onDismiss: () -> Unit,
    onPurchased: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.paywall_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.paywall_feature_unlimited),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = stringResource(R.string.paywall_one_time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            // Purchase button
            Button(
                onClick = {
                    // TODO: Implement Google Play Billing
                    PurchaseManager.getInstance().unlockPro()
                    onPurchased()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(stringResource(R.string.paywall_purchase), fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Restore
            TextButton(onClick = {
                PurchaseManager.getInstance().restorePurchase()
                if (PurchaseManager.getInstance().isProUnlocked) onPurchased()
            }) {
                Text(stringResource(R.string.restore_purchase))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
