package com.diy.androidmanager

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQ_CODE_SHIZUKU = 1001
    }

    private var shellService: IShellService? = null
    private val shizukuReadyState = mutableStateOf(false)
    private val shizukuStatusText = mutableStateOf("正在检测 Shizuku 服务...")

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(BuildConfig.APPLICATION_ID, ShellUserService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("shell_service")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                shellService = IShellService.Stub.asInterface(binder)
                shizukuReadyState.value = true
                shizukuStatusText.value = "Shizuku 已连接"
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shellService = null
            shizukuReadyState.value = false
            shizukuStatusText.value = "Shizuku 服务已断开"
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkAndBindShizuku()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        shellService = null
        shizukuReadyState.value = false
        shizukuStatusText.value = "Shizuku 服务已停止"
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQ_CODE_SHIZUKU) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindShellService()
            } else {
                shizukuReadyState.value = false
                shizukuStatusText.value = "未授予 Shizuku 权限"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val context = LocalContext.current
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                MainScreen(
                    shizukuReady = shizukuReadyState.value,
                    shizukuStatus = shizukuStatusText.value,
                    onRequestShizuku = { requestShizukuPermission() },
                    execShell = { cmd -> runShellCommand(cmd) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        try {
            Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
        } catch (_: Exception) {
        }
    }

    private fun checkAndBindShizuku() {
        try {
            if (!Shizuku.pingBinder()) {
                shizukuReadyState.value = false
                shizukuStatusText.value = "未检测到 Shizuku（请先安装并启动 Shizuku）"
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bindShellService()
            } else {
                shizukuReadyState.value = false
                shizukuStatusText.value = "Shizuku 已运行，请点击授权"
            }
        } catch (e: Exception) {
            shizukuReadyState.value = false
            shizukuStatusText.value = "Shizuku 检测异常: ${e.message}"
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (!Shizuku.pingBinder()) {
                shizukuStatusText.value = "请先打开 Shizuku App 并通过无线调试启动服务"
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bindShellService()
            } else {
                Shizuku.requestPermission(REQ_CODE_SHIZUKU)
            }
        } catch (e: Exception) {
            shizukuStatusText.value = "请求权限失败: ${e.message}"
        }
    }

    private fun bindShellService() {
        if (shellService != null) {
            shizukuReadyState.value = true
            return
        }
        try {
            shizukuStatusText.value = "正在绑定 Shell 服务..."
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Exception) {
            shizukuStatusText.value = "绑定服务失败: ${e.message}"
        }
    }

    private suspend fun runShellCommand(command: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val svc = shellService ?: return@withContext (false to "Shizuku 服务未就绪")
        try {
            val res = svc.exec(command)
            if (res.startsWith("OK:")) {
                true to res.removePrefix("OK:").trim()
            } else {
                false to res.trim()
            }
        } catch (e: Exception) {
            false to (e.message ?: "执行异常")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    shizukuReady: Boolean,
    shizukuStatus: String,
    onRequestShizuku: () -> Unit,
    execShell: suspend (String) -> Pair<Boolean, String>
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val brandInfo = remember { PackageData.getDictionaryForBrand(Build.BRAND) }
    val dictTitle = brandInfo.first
    val activeDict = brandInfo.second

    var selectedTab by remember { mutableIntStateOf(0) }
    var isBusy by remember { mutableStateOf(false) }

    // Tab 1 状态：应用列表
    var allApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var filterMode by remember { mutableIntStateOf(0) } // 0: 推荐列表, 1: 可恢复, 2: 已启用, 3: 全部
    var searchQuery by remember { mutableStateOf("") }
    var selectedPkgs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var riskyConfirmAction by remember { mutableStateOf<String?>(null) }

    // Tab 2 状态：优化勾选与日志
    val optChecks = remember {
        mutableStateMapOf<String, Boolean>().apply {
            PackageData.optimizationItems.forEach { put(it.id, it.defaultChecked) }
        }
    }
    var optLogs by remember { mutableStateOf("就绪。勾选下方项目后点击应用，所有选项均支持一键恢复系统默认。
") }
    var confirmDialogTitle by remember { mutableStateOf<String?>(null) }
    var confirmDialogMessage by remember { mutableStateOf("") }
    var confirmDialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun refreshApps() {
        if (!shizukuReady || isBusy) return
        scope.launch {
            isBusy = true
            try {
                val (okAll, outAll) = execShell("pm list packages -u")
                val (okInst, outInst) = execShell("pm list packages")
                val (okDis, outDis) = execShell("pm list packages -d")
                if (!okAll || !okInst) {
                    snackbarHostState.showSnackbar("读取应用列表失败：$outAll")
                    return@launch
                }
                val setAll = outAll.lines().filter { it.startsWith("package:") }.map { it.removePrefix("package:").trim() }.toSet()
                val setInst = outInst.lines().filter { it.startsWith("package:") }.map { it.removePrefix("package:").trim() }.toSet()
                val setDis = if (okDis) outDis.lines().filter { it.startsWith("package:") }.map { it.removePrefix("package:").trim() }.toSet() else emptySet()

                val list = setAll.sorted().map { pkg ->
                    val status = when {
                        pkg !in setInst -> "已卸载"
                        pkg in setDis -> "已禁用"
                        else -> "已启用"
                    }
                    val info = activeDict[pkg]
                    AppEntry(
                        pkg = pkg,
                        status = status,
                        name = info?.name ?: "-",
                        isRisky = info?.isRisky ?: false,
                        note = info?.note ?: ""
                    )
                }
                allApps = list
                selectedPkgs = emptySet()
            } finally {
                isBusy = false
            }
        }
    }

    LaunchedEffect(shizukuReady) {
        if (shizukuReady) {
            refreshApps()
        }
    }

    fun executeAppBatch(action: String) {
        val targets = allApps.filter { it.pkg in selectedPkgs }
        if (targets.isEmpty()) return
        scope.launch {
            isBusy = true
            var okCount = 0
            val fails = mutableListOf<String>()
            try {
                for (app in targets) {
                    val cmd = when (action) {
                        "disable" -> "pm disable-user --user 0 ${app.pkg}"
                        "uninstall" -> "pm uninstall --user 0 ${app.pkg}"
                        "restore" -> if (app.status == "已卸载") {
                            "cmd package install-existing ${app.pkg}"
                        } else {
                            "pm enable ${app.pkg}"
                        }
                        else -> ""
                    }
                    val (ok, out) = execShell(cmd)
                    if (ok && !out.contains("Failure")) {
                        okCount++
                    } else {
                        fails.add("${app.pkg}: $out")
                    }
                }
                val msg = if (fails.isEmpty()) {
                    "操作完成！成功 $okCount 个"
                } else {
                    "成功 $okCount 个，失败 ${fails.size} 个"
                }
                snackbarHostState.showSnackbar(msg)
            } finally {
                isBusy = false
                refreshApps()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("精简优化助手", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${Build.BRAND} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}) · $dictTitle",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    if (shizukuReady) {
                        IconButton(onClick = { refreshApps() }, enabled = !isBusy) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Shizuku 未就绪时的引导卡片
            if (!shizukuReady) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "需要 Shizuku 权限才能操作本机系统应用",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "当前状态：$shizukuStatus
步骤：1. 安装并打开 Shizuku -> 2. 通过无线调试启动 Shizuku -> 3. 点击下方授权按钮。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onRequestShizuku) {
                            Text("连接 / 授权 Shizuku")
                        }
                    }
                }
            }

            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // 双标签页切换
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("📦 应用精简与恢复") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("⚡ 性能优化与维护") }
                )
            }

            if (selectedTab == 0) {
                // ==========================================
                // Tab 1: 应用精简与恢复
                // ==========================================
                val filterLabels = listOf("推荐/已知", "可恢复", "已启用", "全部")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterLabels.forEachIndexed { index, label ->
                        FilterChip(
                            selected = filterMode == index,
                            onClick = { filterMode = index },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    placeholder = { Text("搜索包名、中文名或说明...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                val filteredApps = remember(allApps, filterMode, searchQuery) {
                    val kw = searchQuery.trim().lowercase()
                    allApps.filter { app ->
                        val matchFilter = when (filterMode) {
                            0 -> app.pkg in activeDict
                            1 -> app.status != "已启用"
                            2 -> app.status == "已启用"
                            else -> true
                        }
                        val matchKw = kw.isEmpty() ||
                                app.pkg.lowercase().contains(kw) ||
                                app.name.lowercase().contains(kw) ||
                                app.note.lowercase().contains(kw)
                        matchFilter && matchKw
                    }
                }

                Text(
                    "显示 ${filteredApps.size} 项 · 已选中 ${selectedPkgs.size} 项",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.pkg }) { app ->
                        val isSelected = app.pkg in selectedPkgs
                        val statusColor = when (app.status) {
                            "已卸载" -> Color(0xFFD32F2F)
                            "已禁用" -> Color(0xFFEF6C00)
                            else -> Color(0xFF2E7D32)
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPkgs = if (isSelected) selectedPkgs - app.pkg else selectedPkgs + app.pkg
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (app.isRisky) {
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedPkgs = if (checked) selectedPkgs + app.pkg else selectedPkgs - app.pkg
                                    }
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (app.name != "-") app.name else app.pkg,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (app.isRisky) {
                                            Spacer(Modifier.width(6.dp))
                                            Text("⚠️谨慎", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = app.pkg,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (app.note.isNotEmpty()) {
                                        Text(
                                            text = app.note,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = app.status,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                // 底部批量操作栏
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val risky = allApps.filter { it.pkg in selectedPkgs && it.isRisky }
                            if (risky.isNotEmpty()) riskyConfirmAction = "disable" else executeAppBatch("disable")
                        },
                        enabled = shizukuReady && !isBusy && selectedPkgs.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("禁用", fontSize = 13.sp) }

                    OutlinedButton(
                        onClick = {
                            val risky = allApps.filter { it.pkg in selectedPkgs && it.isRisky }
                            if (risky.isNotEmpty()) riskyConfirmAction = "uninstall" else executeAppBatch("uninstall")
                        },
                        enabled = shizukuReady && !isBusy && selectedPkgs.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("卸载", fontSize = 13.sp) }

                    FilledTonalButton(
                        onClick = { executeAppBatch("restore") },
                        enabled = shizukuReady && !isBusy && selectedPkgs.isNotEmpty(),
                        modifier = Modifier.weight(1.2f)
                    ) { Text("一键恢复", fontSize = 13.sp) }
                }

            } else {
                // ==========================================
                // Tab 2: 性能优化与系统维护
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 一键维护区（带二次确认）
                    Text("🛠️ 一键维护（均含二次确认防误触）", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            enabled = shizukuReady && !isBusy,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                confirmDialogTitle = "清理全局缓存确认"
                                confirmDialogMessage = "确定要清理本机所有应用的临时缓存吗？（安全操作，不会删除聊天记录或账号数据）"
                                confirmDialogAction = {
                                    scope.launch {
                                        isBusy = true
                                        optLogs += "▶ 开始清理全局应用缓存...
"
                                        val (ok, out) = execShell("pm trim-caches 999G")
                                        optLogs += if (ok) "✅ 清理完成！$out
" else "❌ 失败：$out
"
                                        isBusy = false
                                    }
                                }
                            }
                        ) { Text("清理缓存", fontSize = 12.sp) }

                        OutlinedButton(
                            enabled = shizukuReady && !isBusy,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                confirmDialogTitle = "DEX 深度编译确认"
                                confirmDialogMessage = "将触发系统 ART/DEX 深度编译优化，耗时约 2~5 分钟，期间请保持亮屏或耐心等待。是否开始？"
                                confirmDialogAction = {
                                    scope.launch {
                                        isBusy = true
                                        optLogs += "▶ 正在执行 ART/DEX 深度编译优化（请稍候）...
"
                                        val (ok, out) = execShell("cmd package bg-dexopt-job")
                                        optLogs += if (ok) "✅ 编译优化完成！$out
" else "❌ 失败：$out
"
                                        isBusy = false
                                    }
                                }
                            }
                        ) { Text("DEX优化", fontSize = 12.sp) }

                        OutlinedButton(
                            enabled = shizukuReady && !isBusy,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                confirmDialogTitle = "重启设备确认"
                                confirmDialogMessage = "确定要立即重启本机吗？（恢复系统组件或开启强制分屏后建议重启生效）"
                                confirmDialogAction = {
                                    scope.launch {
                                        optLogs += "▶ 正在请求重启设备...
"
                                        val (ok, out) = execShell("reboot")
                                        if (!ok) optLogs += "⚠️ 重启命令返回：$out
"
                                    }
                                }
                            }
                        ) { Text("重启设备", fontSize = 12.sp) }
                    }

                    HorizontalDivider()

                    // 推荐优化项
                    Text("—— 基础性能与省电设置（推荐） ——", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 14.sp)
                    PackageData.optimizationItems.filter { it.isRecommended }.forEach { item ->
                        OptCheckRow(
                            item = item,
                            checked = optChecks[item.id] == true,
                            onCheckedChange = { optChecks[item.id] = it }
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("—— 可选与进阶设置（仅限手动单独勾选） ——", fontWeight = FontWeight.Bold, color = Color(0xFFEF6C00), fontSize = 14.sp)
                    PackageData.optimizationItems.filter { !it.isRecommended }.forEach { item ->
                        OptCheckRow(
                            item = item,
                            checked = optChecks[item.id] == true,
                            onCheckedChange = { optChecks[item.id] = it }
                        )
                    }

                    // 批量操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = {
                            PackageData.optimizationItems.filter { it.isRecommended }.forEach { optChecks[it.id] = true }
                        }) { Text("勾选推荐") }

                        TextButton(onClick = {
                            PackageData.optimizationItems.forEach { optChecks[it.id] = false }
                        }) { Text("取消全选") }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            enabled = shizukuReady && !isBusy,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val targets = PackageData.optimizationItems.filter { optChecks[it.id] == true }
                                if (targets.isEmpty()) return@Button
                                scope.launch {
                                    isBusy = true
                                    optLogs += "▶ 开始应用 ${targets.size} 项优化：
"
                                    var okCnt = 0
                                    for (t in targets) {
                                        var itemOk = true
                                        for (cmd in t.applyCmds) {
                                            val (ok, out) = execShell(cmd)
                                            if (!ok) {
                                                itemOk = false
                                                optLogs += "   ❌ [${t.name}] $out
"
                                            }
                                        }
                                        if (itemOk) {
                                            okCnt++
                                            optLogs += "   ✅ [${t.name}] 已应用
"
                                        }
                                    }
                                    optLogs += "🎉 完成：$okCnt/${targets.size}
"
                                    isBusy = false
                                }
                            }
                        ) { Text("🚀 应用选中优化") }

                        FilledTonalButton(
                            enabled = shizukuReady && !isBusy,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val targets = PackageData.optimizationItems.filter { optChecks[it.id] == true }
                                if (targets.isEmpty()) return@Button
                                scope.launch {
                                    isBusy = true
                                    optLogs += "▶ 开始恢复 ${targets.size} 项为系统默认：
"
                                    var okCnt = 0
                                    for (t in targets) {
                                        var itemOk = true
                                        for (cmd in t.restoreCmds) {
                                            val (ok, out) = execShell(cmd)
                                            if (!ok) {
                                                itemOk = false
                                                optLogs += "   ❌ [${t.name}] $out
"
                                            }
                                        }
                                        if (itemOk) {
                                            okCnt++
                                            optLogs += "   ♻️ [${t.name}] 已恢复默认
"
                                        }
                                    }
                                    optLogs += "🎉 恢复完成：$okCnt/${targets.size}
"
                                    isBusy = false
                                }
                            }
                        ) { Text("♻️ 恢复选中默认") }
                    }

                    // 执行日志区
                    Text("📋 执行日志", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = optLogs,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    // 高危包名二次确认弹窗
    if (riskyConfirmAction != null) {
        val riskyNames = allApps.filter { it.pkg in selectedPkgs && it.isRisky }
            .joinToString("
") { "• ${it.name} (${it.pkg})" }
        AlertDialog(
            onDismissRequest = { riskyConfirmAction = null },
            title = { Text("⚠️ 高风险操作确认") },
            text = {
                Text("你选中的项目中包含系统关键依赖：

$riskyNames

禁用或卸载可能导致人脸解锁、S Pen 或系统设置异常，确定要继续吗？")
            },
            confirmButton = {
                TextButton(onClick = {
                    val act = riskyConfirmAction!!
                    riskyConfirmAction = null
                    executeAppBatch(act)
                }) { Text("仍然继续", color = Color(0xFFD32F2F)) }
            },
            dismissButton = {
                TextButton(onClick = { riskyConfirmAction = null }) { Text("取消") }
            }
        )
    }

    // 一键维护二次确认弹窗
    if (confirmDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { confirmDialogTitle = null },
            title = { Text(confirmDialogTitle!!) },
            text = { Text(confirmDialogMessage) },
            confirmButton = {
                TextButton(onClick = {
                    val action = confirmDialogAction
                    confirmDialogTitle = null
                    confirmDialogAction = null
                    action?.invoke()
                }) { Text("确定执行") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmDialogTitle = null
                    confirmDialogAction = null
                }) { Text("取消") }
            }
        )
    }
}

@Composable
fun OptCheckRow(
    item: OptimizationItem,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.padding(start = 6.dp)) {
            Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(item.desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
