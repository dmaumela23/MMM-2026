package com.maumela.magnummanagement.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.maumela.magnummanagement.AppContainer
import com.maumela.magnummanagement.notifications.FcmRegistrar
import com.maumela.magnummanagement.ui.screens.EnergyDashboardScreen
import com.maumela.magnummanagement.ui.screens.EnergyReportsScreen
import com.maumela.magnummanagement.ui.screens.EnergyUsageScreen
import com.maumela.magnummanagement.ui.screens.HomeScreen
import com.maumela.magnummanagement.ui.screens.LoginScreen
import com.maumela.magnummanagement.ui.screens.MarketplaceScreen
import com.maumela.magnummanagement.ui.screens.OrderDetailScreen
import com.maumela.magnummanagement.ui.screens.OrdersScreen
import com.maumela.magnummanagement.ui.screens.ProductDetailScreen
import com.maumela.magnummanagement.ui.screens.ProfileScreen
import com.maumela.magnummanagement.ui.screens.RegisterScreen
import com.maumela.magnummanagement.ui.screens.ServiceDetailScreen
import com.maumela.magnummanagement.ui.screens.SettingsScreen
import com.maumela.magnummanagement.ui.screens.SplashScreen
import com.maumela.magnummanagement.viewmodel.AuthViewModel
import com.maumela.magnummanagement.viewmodel.EnergyViewModel
import com.maumela.magnummanagement.viewmodel.HomeViewModel
import com.maumela.magnummanagement.viewmodel.MarketplaceViewModel
import com.maumela.magnummanagement.viewmodel.OrderDetailViewModel
import com.maumela.magnummanagement.viewmodel.OrderDraftViewModel
import com.maumela.magnummanagement.viewmodel.OrdersViewModel
import com.maumela.magnummanagement.viewmodel.ProductDetailViewModel
import com.maumela.magnummanagement.viewmodel.ProfileOverviewViewModel
import com.maumela.magnummanagement.viewmodel.ProfileViewModel
import com.maumela.magnummanagement.viewmodel.ServiceDetailViewModel
import com.maumela.magnummanagement.viewmodel.SettingsViewModel
import com.maumela.magnummanagement.viewmodel.SplashViewModel
import com.maumela.magnummanagement.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The whole app: one NavHost, the bottom bar on the five top-level screens, the order draft shared by
 * every screen, and the session rules (logout, and "session expired" on any 401).
 */
@Composable
fun MmmApp(container: AppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()
    val fcm = remember { FcmRegistrar(context.applicationContext) }

    // Activity-scoped, so the draft survives moving between screens (but not the process being killed).
    val draftViewModel: OrderDraftViewModel =
        viewModel(viewModelStoreOwner = activity, factory = ViewModelFactory(container))
    val draft by draftViewModel.state.collectAsStateWithLifecycle()
    val user by container.authRepository.currentUser.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = Routes.showsBottomBar(currentRoute)
    var loginNotice by remember { mutableStateOf<String?>(null) }

    fun goToLogin() {
        navController.navigate(Routes.LOGIN) { popUpTo(navController.graph.id) { inclusive = true } }
    }

    fun enterApp() {
        loginNotice = null
        navController.navigate(Routes.HOME) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun logout() {
        scope.launch {
            // Best effort: stop sending this device's notifications to the account that is leaving.
            container.settingsRepository.registerFcmToken("")
            container.authRepository.logout() // clears the token, saved preferences and the Room cache
            draftViewModel.clear()
            goToLogin()
        }
    }

    // Any 401 on a normal request means the session expired: clear everything and return to Login.
    LaunchedEffect(Unit) {
        container.sessionEvents.expired.collect {
            val route = Routes.baseRoute(navController.currentDestination?.route)
            if (route != Routes.LOGIN && route != Routes.REGISTER && route != Routes.SPLASH) {
                container.authRepository.logout()
                draftViewModel.clear()
                loginNotice = "Your session expired. Please log in again."
                goToLogin()
            }
        }
    }

    // Register this device for push notifications whenever someone is logged in (needs Firebase set up).
    LaunchedEffect(user?.id) {
        if (user != null && container.preferencesStore.notificationsEnabled.first()) {
            val token = fcm.fetchToken()
            if (token != null) container.settingsRepository.registerFcmToken(token)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .then(if (showBottomBar) Modifier else Modifier.navigationBarsPadding()),
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(
                        viewModel = appViewModel<SplashViewModel>(container),
                        onGoHome = { enterApp() },
                        onGoLogin = { goToLogin() },
                    )
                }
                composable(Routes.LOGIN) {
                    LoginScreen(
                        viewModel = appViewModel<AuthViewModel>(container),
                        notice = loginNotice,
                        onAuthenticated = { enterApp() },
                        onRegister = { navController.navigate(Routes.REGISTER) },
                    )
                }
                composable(Routes.REGISTER) {
                    RegisterScreen(
                        viewModel = appViewModel<AuthViewModel>(container),
                        onAuthenticated = { enterApp() },
                        onBackToLogin = { navController.popBackStack() },
                    )
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        viewModel = appViewModel<HomeViewModel>(container),
                        onOpenMarketplace = { tab, query -> navController.navigate(Routes.marketplace(tab, query)) },
                        onOpenEnergy = { navController.navigateTopLevel(Routes.ENERGY) },
                        onOpenOrders = { navController.navigateTopLevel(Routes.ORDERS) },
                        onOpenProfile = { navController.navigateTopLevel(Routes.PROFILE) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onProduct = { navController.navigate(Routes.product(it)) },
                        onService = { navController.navigate(Routes.service(it)) },
                        onAddProduct = { draftViewModel.addProduct(it) },
                        onAddService = { draftViewModel.addService(it) },
                    )
                }
                composable(
                    route = Routes.MARKETPLACE,
                    arguments = listOf(
                        navArgument(Routes.ARG_TAB) { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument(Routes.ARG_QUERY) { type = NavType.StringType; nullable = true; defaultValue = null },
                    ),
                ) { entry ->
                    MarketplaceScreen(
                        viewModel = appViewModel<MarketplaceViewModel>(container),
                        tabArg = entry.arguments?.getString(Routes.ARG_TAB),
                        queryArg = entry.arguments?.getString(Routes.ARG_QUERY),
                        draftCount = draft.itemCount,
                        onProduct = { navController.navigate(Routes.product(it)) },
                        onService = { navController.navigate(Routes.service(it)) },
                        onAddProduct = { draftViewModel.addProduct(it) },
                        onAddService = { draftViewModel.addService(it) },
                        onOpenOrders = { navController.navigateTopLevel(Routes.ORDERS) },
                    )
                }
                composable(
                    route = Routes.PRODUCT,
                    arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.IntType }),
                ) { entry ->
                    val id = entry.arguments?.getInt(Routes.ARG_ID) ?: 0
                    ProductDetailScreen(
                        viewModel = appViewModel<ProductDetailViewModel>(container, productId = id),
                        user = user,
                        onBack = { navController.popBackStack() },
                        onAddToOrder = { product, quantity -> draftViewModel.addProduct(product, quantity) },
                        onGoToOrders = { navController.navigateTopLevel(Routes.ORDERS) },
                    )
                }
                composable(
                    route = Routes.SERVICE,
                    arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.IntType }),
                ) { entry ->
                    val id = entry.arguments?.getInt(Routes.ARG_ID) ?: 0
                    ServiceDetailScreen(
                        viewModel = appViewModel<ServiceDetailViewModel>(container, serviceId = id),
                        user = user,
                        onBack = { navController.popBackStack() },
                        onAddToOrder = { draftViewModel.addService(it) },
                        onGoToOrders = { navController.navigateTopLevel(Routes.ORDERS) },
                    )
                }
                composable(Routes.ENERGY) {
                    EnergyDashboardScreen(
                        viewModel = appViewModel<EnergyViewModel>(container),
                        onUsage = { navController.navigate(Routes.ENERGY_USAGE) },
                        onReports = { navController.navigate(Routes.ENERGY_REPORTS) },
                    )
                }
                composable(Routes.ENERGY_USAGE) {
                    EnergyUsageScreen(
                        viewModel = appViewModel<EnergyViewModel>(container),
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.ENERGY_REPORTS) {
                    EnergyReportsScreen(
                        viewModel = appViewModel<EnergyViewModel>(container),
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.ORDERS) {
                    OrdersScreen(
                        viewModel = appViewModel<OrdersViewModel>(container),
                        draftViewModel = draftViewModel,
                        onOrder = { navController.navigate(Routes.order(it)) },
                        onBrowse = { navController.navigateTopLevel(Routes.MARKETPLACE_BASE) },
                    )
                }
                composable(
                    route = Routes.ORDER,
                    arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.IntType }),
                ) { entry ->
                    val id = entry.arguments?.getInt(Routes.ARG_ID) ?: 0
                    OrderDetailScreen(
                        viewModel = appViewModel<OrderDetailViewModel>(container, orderId = id),
                        user = user,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.PROFILE) {
                    ProfileScreen(
                        viewModel = appViewModel<ProfileOverviewViewModel>(container),
                        onSettings = { navController.navigate(Routes.SETTINGS) },
                        onLogout = { logout() },
                        onProduct = { navController.navigate(Routes.product(it)) },
                        onService = { navController.navigate(Routes.service(it)) },
                        onOpenOrders = { navController.navigateTopLevel(Routes.ORDERS) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        settingsViewModel = appViewModel<SettingsViewModel>(container),
                        profileViewModel = appViewModel<ProfileViewModel>(container),
                        fcm = fcm,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        if (showBottomBar) {
            MmmBottomBar(
                currentRoute = currentRoute,
                draftCount = draft.itemCount,
                onNavigate = { navController.navigateTopLevel(it) },
            )
        }
    }
}
