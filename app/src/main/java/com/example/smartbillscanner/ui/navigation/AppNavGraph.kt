package com.example.smartbillscanner.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.smartbillscanner.domain.model.BillReceipt
import com.example.smartbillscanner.domain.repository.BillRepository
import com.example.smartbillscanner.ui.components.AppBottomBar
import com.example.smartbillscanner.ui.screens.camera.ScannerScreen
import com.example.smartbillscanner.ui.screens.dashboard.DashboardScreen
import com.example.smartbillscanner.ui.screens.detail.BillDetailScreen
import com.example.smartbillscanner.ui.screens.split.SplitPayScreen
import com.example.smartbillscanner.ui.screens.verify.VerifyBillScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(
    repository: BillRepository,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bills by repository.getAllBills().collectAsState(initial = emptyList())

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    var activeBill by remember { mutableStateOf(BillReceipt()) }

    // No Scaffold here: every screen draws edge-to-edge and handles its own system-bar insets,
    // so the camera and backgrounds fill the whole display (no inset "box" around the app).
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            enterTransition = { fadeIn(tween(240)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = { fadeOut(tween(160)) }
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    bills = bills,
                    onBillClick = { bill ->
                        activeBill = bill
                        navController.navigate(Screen.Detail.route)
                    },
                    onDeleteBill = { id ->
                        coroutineScope.launch {
                            repository.deleteBill(id)
                        }
                    }
                )
            }

            composable(Screen.Detail.route) {
                // always show the latest saved copy of this bill
                val live = bills.firstOrNull { it.id == activeBill.id } ?: activeBill
                BillDetailScreen(
                    bill = live,
                    onEdit = {
                        activeBill = live
                        navController.navigate(Screen.Verify.route)
                    },
                    onSplit = {
                        activeBill = live
                        navController.navigate(Screen.SplitPay.route)
                    },
                    onDelete = {
                        coroutineScope.launch { repository.deleteBill(live.id) }
                        navController.popBackStack()
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Scanner.route) {
                ScannerScreen(
                    onBillScanned = { scannedBill ->
                        activeBill = scannedBill
                        navController.navigate(Screen.Verify.route) {
                            popUpTo(Screen.Scanner.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Verify.route) {
                VerifyBillScreen(
                    bill = activeBill,
                    onSaveAndSplit = { updatedBill ->
                        coroutineScope.launch {
                            // keep the saved id, otherwise the next save would create a duplicate bill
                            val savedId = repository.saveBill(updatedBill)
                            activeBill = updatedBill.copy(id = savedId)
                            navController.navigate(Screen.SplitPay.route)
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.SplitPay.route) {
                SplitPayScreen(
                    bill = activeBill,
                    onComplete = { completedBill ->
                        coroutineScope.launch {
                            repository.saveBill(completedBill)
                        }
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Dashboard.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Floating bar only on the home screen; Verify / Split have their own action bar.
        if (currentRoute == Screen.Dashboard.route) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        when (route) {
                            Screen.Scanner.route -> {
                                navController.navigate(Screen.Scanner.route) { launchSingleTop = true }
                            }
                            Screen.SplitPay.route -> {
                                // Split & Pay works on a real bill: latest unsettled one, else latest
                                val target = bills.firstOrNull { !it.isPaid } ?: bills.firstOrNull()
                                if (target == null) {
                                    Toast.makeText(context, "Scan a bill first", Toast.LENGTH_SHORT).show()
                                } else {
                                    activeBill = target
                                    navController.navigate(Screen.SplitPay.route) { launchSingleTop = true }
                                }
                            }
                            else -> {
                                navController.navigate(route) {
                                    popUpTo(Screen.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
