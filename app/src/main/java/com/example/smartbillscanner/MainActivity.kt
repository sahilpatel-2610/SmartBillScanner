package com.example.smartbillscanner

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.smartbillscanner.data.repository.BillRepositoryImpl
import com.example.smartbillscanner.ui.components.AppBackground
import com.example.smartbillscanner.ui.navigation.AppNavGraph
import com.example.smartbillscanner.ui.theme.SmartBillScannerTheme
import com.example.smartbillscanner.ui.theme.TextWhite

class MainActivity : ComponentActivity() {

    private val billRepository by lazy {
        BillRepositoryImpl(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Truly full-screen: transparent status + navigation bars with light icons
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            SmartBillScannerTheme {
                AppBackground {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        contentColor = TextWhite
                    ) {
                        AppNavGraph(repository = billRepository)
                    }
                }
            }
        }
    }
}
