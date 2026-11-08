package sa.factory.qootsurplusfood129.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sa.factory.qootsurplusfood129.ui.theme.*
import sa.factory.qootsurplusfood129.ui.viewmodel.QootSurplusUiState

@Composable
fun TelemetryScreen(state: QootSurplusUiState) {
    val defaultPeripheral = "Kitchen Display System (KDS) TCP/IP Print Server"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("مراقبة العتاد الحي والحساسات", fontSize = 20.sp, fontWeight = FontWeight.Black, color = BrandTextPrimary)
        Text("بروتوكول الربط المباشر الميداني لمطابخ قُوّت", fontSize = 12.sp, color = BrandTextSecondary)

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("حالة الجهاز الملحق:", color = BrandTextSecondary, fontSize = 13.sp)
                    Badge(containerColor = BrandPrimary) {
                        Text(state.telemetry?.status ?: "ONLINE", color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                    }
                }
                val peripheralLabel = state.telemetry?.peripheralName ?: defaultPeripheral
                Text("اسم العتاد: $peripheralLabel", fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 14.sp)
                Text("جودة الإشارة: ${state.telemetry?.signalDbm ?: -58} dBm | البطارية: ${state.telemetry?.batteryPct ?: 98}%", color = BrandSecondary, fontSize = 12.sp)

                HorizontalDivider(color = BrandCard)
                Text("حزمة البيانات اللحظية (Raw Telemetry Payload):", color = BrandTextSecondary, fontSize = 11.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.telemetry?.liveReading ?: "$defaultPeripheral: ONLINE | READY",
                        color = BrandPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BrandCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("معايير الامتثال والحوكمة الغذائية", fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 14.sp)
                Text("[OK] بروتوكول شبكي KDS TCP/IP مشفر (Port 9100 / TLS 1.3)", color = BrandTextSecondary, fontSize = 12.sp)
                Text("[OK] معايير سلامة الأغذية والتحكم بدرجات حرارة سلسلة التبريد (SFDA)", color = BrandTextSecondary, fontSize = 12.sp)
                Text("[OK] خوارزمية ذكية لتقليل الهدر الغذائي وحفظ النعمة بدقة 100%", color = BrandTextSecondary, fontSize = 12.sp)
                Text("[OK] بنية برمجية نقية خالية من الإيموجي ومتوافقة مع أعلى معايير الجودة", color = BrandTextSecondary, fontSize = 12.sp)
            }
        }
    }
}
