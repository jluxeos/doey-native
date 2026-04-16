package com.doey.ui.core

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.doey.MainViewModel
import com.doey.agente.ProfileStore
import com.doey.agente.SettingsStore
import com.doey.ui.avanzado.LogScreen
import com.doey.ui.avanzado.PermissionsScreen
import com.doey.ui.basico.FriendlySettingsScreen
import com.doey.ui.basico.JournalScreen
import com.doey.ui.comun.*
import kotlinx.coroutines.launch

// ── Rutas de navegación ──────────────────────────────────────────
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Inicio        : Screen("inicio",         "Inicio",     CustomIcons.Home)
    object Memorias      : Screen("memorias",       "Memorias",   CustomIcons.Message)
    object Agendas       : Screen("agendas",        "Agendas",    CustomIcons.Clock)
    object Diario        : Screen("diario",         "Diario",     CustomIcons.LibraryBooks)
    object Permisos      : Screen("permisos",       "Permisos",   CustomIcons.Lock)
    object Configuracion : Screen("configuracion",  "Ajustes",    CustomIcons.Settings)
    object Registros     : Screen("registros",      "Registros",  CustomIcons.BugReport)
    object Perfil        : Screen("perfil",         "Mi Perfil",  CustomIcons.Person)
    object AjustesBasicos: Screen("ajustes_basicos","Modo Básico",CustomIcons.Spa)
}

// ── Punto de entrada ─────────────────────────────────────────────
@Composable
fun DoeyApp() {
    val vm           = viewModel<MainViewModel>()
    val ctx          = LocalContext.current
    val profileStore = remember { ProfileStore(ctx) }
    val settingsStore= remember { SettingsStore(ctx) }

    var onboardingDone by remember { mutableStateOf(profileStore.isOnboardingDone()) }
    var userProfile    by remember { mutableStateOf(profileStore.getUserProfile()) }
    var isLowPower     by remember { mutableStateOf(profileStore.isLowPowerMode()) }
    var activeTheme    by remember { mutableStateOf("NebulaPurple") }

    LaunchedEffect(Unit) {
        activeTheme = settingsStore.getTheme()
        updateGlassTheme(activeTheme)
    }

    val scope2 = rememberCoroutineScope()
    val onProfileChanged: () -> Unit = {
        userProfile = profileStore.getUserProfile()
        isLowPower  = profileStore.isLowPowerMode()
        scope2.launch {
            activeTheme = settingsStore.getTheme()
            updateGlassTheme(activeTheme)
        }
    }

    val activity = ctx as? android.app.Activity

    LaunchedEffect(Unit) {
        val intent = activity?.intent
        if (intent?.getBooleanExtra("auto_listen", false) == true) {
            intent.removeExtra("auto_listen")
            vm.startListening()
        }
        if (intent?.getBooleanExtra("stop_all", false) == true) {
            intent.removeExtra("stop_all")
            vm.stopSpeaking()
            vm.stopListening()
        }
        val pendingQuery = intent?.getStringExtra("pending_query")
        if (!pendingQuery.isNullOrBlank()) {
            intent.removeExtra("pending_query")
            vm.sendMessage(pendingQuery)
        }
    }

    if (!onboardingDone) {
        OnboardingFlow { name, profile, performance, provider, apiKey ->
            profileStore.setUserName(name)
            profileStore.setUserProfile(
                if (profile == UserProfile.BASIC) ProfileStore.PROFILE_BASIC else ProfileStore.PROFILE_ADVANCED
            )
            profileStore.setPerformanceMode(
                if (performance == PerformanceMode.LOW_POWER) ProfileStore.PERF_LOW_POWER else ProfileStore.PERF_HIGH
            )
            profileStore.setOnboardingDone(true)
            if (apiKey.isNotBlank()) {
                val settings = vm.getSettings()
                settings.setApiKey(provider, apiKey)
                kotlinx.coroutines.MainScope().launch { settings.setProvider(provider) }
            }
            userProfile    = if (profile == UserProfile.BASIC) ProfileStore.PROFILE_BASIC else ProfileStore.PROFILE_ADVANCED
            isLowPower     = performance == PerformanceMode.LOW_POWER
            onboardingDone = true
        }
        return
    }

    val nav         = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope       = rememberCoroutineScope()
    val isAdvanced  = userProfile == ProfileStore.PROFILE_ADVANCED

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DeltaDrawerContent(nav, drawerState, isAdvanced, scope)
        }
    ) {
        Scaffold(
            containerColor = DeltaBg,
            topBar = {
                DeltaTopBar(vm, nav, onMenuClick = { scope.launch { drawerState.open() } })
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                NavHost(
                    navController  = nav,
                    startDestination = Screen.Inicio.route,
                    enterTransition  = { fadeIn(tween(220)) },
                    exitTransition   = { fadeOut(tween(180)) }
                ) {
                    composable(Screen.Inicio.route)         { HomeScreen(vm, nav) }
                    composable(Screen.Configuracion.route)  { SettingsScreen(vm, onProfileChanged) }
                    composable(Screen.Perfil.route)         { ProfileScreen(vm) }
                    composable(Screen.Memorias.route)       { MemoriesScreen(vm) }
                    composable(Screen.Diario.route)         { JournalScreen(vm) }
                    composable(Screen.Agendas.route)        { SchedulesScreen(vm) }
                    composable(Screen.Registros.route)      { LogScreen() }
                    composable(Screen.Permisos.route)       { PermissionsScreen() }
                    composable(Screen.AjustesBasicos.route) { FriendlySettingsScreen(vm) }
                }
            }
        }
    }
}

// ── Top Bar ──────────────────────────────────────────────────────
@Composable
private fun DeltaTopBar(vm: MainViewModel, nav: NavController, onMenuClick: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(DeltaAccent.copy(alpha = 0.15f))
                        .border(1.dp, DeltaAccent.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("δ", style = DoeyTypography.labelLarge, color = DeltaAccent)
                }
                Spacer(Modifier.width(10.dp))
                Text("Doey Delta", style = DoeyTypography.titleLarge, color = DeltaText1)
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(CustomIcons.Menu, "Menú", tint = DeltaText1)
            }
        },
        actions = {
            IconButton(onClick = { nav.navigate(Screen.Perfil.route) }) {
                Icon(CustomIcons.Person, "Perfil", tint = DeltaText2)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

// ── Drawer ───────────────────────────────────────────────────────
@Composable
private fun DeltaDrawerContent(
    nav: NavController,
    drawerState: DrawerState,
    isAdvanced: Boolean,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun navigate(route: String) {
        scope.launch { drawerState.close() }
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    }

    ModalDrawerSheet(
        drawerContainerColor = DeltaBg,
        drawerContentColor   = DeltaText1,
        modifier = Modifier.width(300.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            DeltaBackground()

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                // Header
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeltaAccent.copy(alpha = 0.08f))
                        .border(1.dp, DeltaAccent.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(DeltaAccent.copy(alpha = 0.12f))
                                    .border(1.5.dp, DeltaAccent.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("δ", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    color = DeltaAccent)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Doey Delta",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize   = 18.sp,
                                    color      = DeltaText1)
                                Text(if (isAdvanced) "Modo Experto" else "Modo Básico",
                                    fontSize = 12.sp,
                                    color    = DeltaAccent.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                DrawerSectionHeader("Principal")
                DrawerItem(CustomIcons.Home,    "Inicio",    currentRoute == Screen.Inicio.route)        { navigate(Screen.Inicio.route) }
                DrawerItem(CustomIcons.Person,  "Mi Perfil", currentRoute == Screen.Perfil.route)        { navigate(Screen.Perfil.route) }

                DrawerSectionHeader("Herramientas")
                DrawerItem(CustomIcons.Alarm,      "Agendas",  currentRoute == Screen.Agendas.route)     { navigate(Screen.Agendas.route) }
                DrawerItem(CustomIcons.Psychology, "Memorias", currentRoute == Screen.Memorias.route)    { navigate(Screen.Memorias.route) }
                DrawerItem(CustomIcons.LibraryBooks,"Diario",  currentRoute == Screen.Diario.route)      { navigate(Screen.Diario.route) }

                if (isAdvanced) {
                    DrawerSectionHeader("Avanzado")
                    DrawerItem(CustomIcons.BugReport, "Logs",    currentRoute == Screen.Registros.route) { navigate(Screen.Registros.route) }
                    DrawerItem(CustomIcons.Lock,      "Permisos",currentRoute == Screen.Permisos.route)  { navigate(Screen.Permisos.route) }
                }

                DrawerSectionHeader("Sistema")
                DrawerItem(CustomIcons.Settings, "Ajustes", currentRoute == Screen.Configuracion.route) { navigate(Screen.Configuracion.route) }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}
