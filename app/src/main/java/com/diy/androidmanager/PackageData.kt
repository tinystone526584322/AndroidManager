package com.diy.androidmanager

data class KnownPackageInfo(
    val name: String,
    val isRisky: Boolean,
    val note: String
)

data class AppEntry(
    val pkg: String,
    val status: String, // "已启用", "已禁用", "已卸载"
    val name: String,
    val isRisky: Boolean,
    val note: String
)

data class OptimizationItem(
    val id: String,
    val isRecommended: Boolean,
    val defaultChecked: Boolean,
    val name: String,
    val desc: String,
    val applyCmds: List<String>,
    val restoreCmds: List<String>
)

object PackageData {

    val lenovoPackages = mapOf(
        "com.tbsmart.levision" to KnownPackageInfo("联想智慧视觉(慧眼)", true, "人脸解锁/视线感知依赖，删除会导致无法录入面部"),
        "com.lenovo.lenovoprivacy" to KnownPackageInfo("联想隐私协议", true, "首次录入面部/开启系统服务需弹窗确认，勿删"),
        "com.lenovo.lsf" to KnownPackageInfo("联想账号框架", true, "登录联想账号、使用自带应用商店需要"),
        "com.lenovo.lsf.device" to KnownPackageInfo("联想设备服务", true, "联想账号与设备认证相关"),
        "com.zui.camera" to KnownPackageInfo("ZUI 系统相机", true, "部分系统级扫码及面部录入依赖底层调用"),
        "com.zui.safecenter" to KnownPackageInfo("ZUI 安全中心", true, "权限管理与系统安全核心，不建议动"),
        "com.zui.cores" to KnownPackageInfo("ZUI 核心服务", true, "系统底层核心组件，勿删"),
        "com.zui.browser" to KnownPackageInfo("ZUI 浏览器", false, "系统自带浏览器"),
        "com.lenovo.leos.cloud.sync" to KnownPackageInfo("联想云服务", false, "不用联想云同步可禁用/卸载"),
        "com.lenovo.xiaotian.trigger" to KnownPackageInfo("小天语音触发器", false, "联想AI语音唤醒组件"),
        "com.lenovo.levoice_agent" to KnownPackageInfo("联想语音助手", false, "语音助手主程序"),
        "com.lenovo.levoice.caption" to KnownPackageInfo("实时字幕", false, "AI实时字幕服务"),
        "com.baidu.map.location" to KnownPackageInfo("百度网络定位", false, "后台定位服务，日常可精简"),
        "com.lenovo.ue.device" to KnownPackageInfo("用户体验计划", false, "后台数据收集与上报"),
        "com.tblenovo.ue.config" to KnownPackageInfo("用户体验配置", false, "数据收集配置组件"),
        "com.tblenovo.lenovowhatsnew" to KnownPackageInfo("新功能介绍", false, "系统更新与功能推广提示"),
        "com.tblenovo.tabpushout" to KnownPackageInfo("平板推送服务", false, "系统广告与消息推送"),
        "com.lenovo.lfh.tianjiao.tablet" to KnownPackageInfo("天骄学堂", false, "儿童/学习模式组件"),
        "com.lenovo.menu_assistant.hd" to KnownPackageInfo("联想菜单助手", false, "辅助菜单服务"),
        "com.lenovo.xbb" to KnownPackageInfo("联想小帮手", false, "售后与保修服务组件"),
        "com.lmsa.app.lmsapad" to KnownPackageInfo("联想智能救援", false, "官方刷机/救援工具")
    )

    val samsungPackages = mapOf(
        "com.samsung.android.aircommandmanager" to KnownPackageInfo("S Pen 浮窗指令", true, "手写笔拔出弹窗、熄屏快写核心依赖，勿删"),
        "com.sec.android.desktopmode.uiservice" to KnownPackageInfo("Samsung DeX", true, "外接显示器/平板切换桌面模式必需"),
        "com.samsung.android.bio.face.service" to KnownPackageInfo("三星人脸识别服务", true, "面部解锁核心组件，勿删"),
        "com.samsung.android.biometrics.app.setting" to KnownPackageInfo("生物识别设置", true, "指纹与面部录入设置界面，勿删"),
        "com.sec.android.app.samsungapps" to KnownPackageInfo("三星应用商店", true, "Good Lock 及系统自带组件更新依赖"),
        "com.samsung.android.knox.containercore" to KnownPackageInfo("Knox 安全核心", true, "安全文件夹依赖，误删可能导致设置闪退"),
        "com.samsung.android.bixby.agent" to KnownPackageInfo("Bixby 语音助手", false, "不用 Bixby 可安全禁用"),
        "com.samsung.android.bixby.wakeup" to KnownPackageInfo("Bixby 语音唤醒", false, "后台语音唤醒监听"),
        "com.samsung.android.bixbyvision.framework" to KnownPackageInfo("Bixby 视觉", false, "相机识图组件"),
        "com.samsung.android.arzone" to KnownPackageInfo("AR 区域", false, "AR 趣味拍照与贴纸"),
        "com.samsung.android.aremoji" to KnownPackageInfo("动态萌拍", false, "AR 表情包组件"),
        "com.samsung.android.app.spage" to KnownPackageInfo("Samsung Free / 负一屏", false, "桌面最左侧资讯页"),
        "com.samsung.android.app.tips" to KnownPackageInfo("三星使用技巧", false, "系统新手引导提示"),
        "com.samsung.android.kidsinstaller" to KnownPackageInfo("三星儿童模式", false, "儿童空间安装器"),
        "com.samsung.android.game.gamehome" to KnownPackageInfo("游戏中心", false, "游戏聚合启动器（不影响游戏助推器）"),
        "com.sec.android.app.sbrowser" to KnownPackageInfo("三星浏览器", false, "若用 Chrome/Edge 可禁用"),
        "com.samsung.android.app.watchmanagerstub" to KnownPackageInfo("穿戴设备引导", false, "不用三星手表/耳机可精简")
    )

    val commonPackages = mapOf(
        "com.facebook.system" to KnownPackageInfo("Facebook 系统服务", false, "外版/港版系统常见后台常驻"),
        "com.facebook.appmanager" to KnownPackageInfo("Facebook 应用管理", false, "后台静默更新组件"),
        "com.facebook.services" to KnownPackageInfo("Facebook 后台服务", false, "后台推送与统计组件"),
        "com.microsoft.skydrive" to KnownPackageInfo("OneDrive 预装", false, "不用微软云盘可禁用"),
        "com.netflix.partner.activation" to KnownPackageInfo("Netflix 激活服务", false, "预装合作推广组件")
    )

    fun getDictionaryForBrand(brandRaw: String): Pair<String, Map<String, KnownPackageInfo>> {
        val b = brandRaw.lowercase()
        val merged = mutableMapOf<String, KnownPackageInfo>()
        merged.putAll(commonPackages)
        return when {
            b.contains("lenovo") || b.contains("motorola") || b.contains("zui") -> {
                merged.putAll(lenovoPackages)
                "联想 / ZUI 字典" to merged
            }
            b.contains("samsung") -> {
                merged.putAll(samsungPackages)
                "三星 / One UI 字典" to merged
            }
            else -> {
                merged.putAll(lenovoPackages)
                merged.putAll(samsungPackages)
                "通用字典" to merged
            }
        }
    }

    val optimizationItems = listOf(
        // 基础性能与省电设置（推荐）
        OptimizationItem(
            id = "anim_window",
            isRecommended = true,
            defaultChecked = true,
            name = "窗口动画缩放 0.5x",
            desc = "提升窗口弹出与 UI 响应速度",
            applyCmds = listOf("settings put global window_animation_scale 0.5"),
            restoreCmds = listOf("settings put global window_animation_scale 1.0")
        ),
        OptimizationItem(
            id = "anim_transition",
            isRecommended = true,
            defaultChecked = true,
            name = "过渡动画缩放 0.5x",
            desc = "加速页面切换与画面过渡",
            applyCmds = listOf("settings put global transition_animation_scale 0.5"),
            restoreCmds = listOf("settings put global transition_animation_scale 1.0")
        ),
        OptimizationItem(
            id = "anim_duration",
            isRecommended = true,
            defaultChecked = true,
            name = "动画程序时长缩放 0.5x",
            desc = "加速系统整体动态效果",
            applyCmds = listOf("settings put global animator_duration_scale 0.5"),
            restoreCmds = listOf("settings put global animator_duration_scale 1.0")
        ),
        OptimizationItem(
            id = "ble_scan",
            isRecommended = true,
            defaultChecked = true,
            name = "关闭 BLE 始终扫描",
            desc = "停止后台低功耗蓝牙扫描，降低待机耗电",
            applyCmds = listOf("settings put global ble_scan_always_enabled 0"),
            restoreCmds = listOf("settings put global ble_scan_always_enabled 1")
        ),
        OptimizationItem(
            id = "wifi_scan",
            isRecommended = true,
            defaultChecked = true,
            name = "关闭 WiFi 始终扫描",
            desc = "停止后台 WiFi 位置扫描，降低待机耗电",
            applyCmds = listOf("settings put global wifi_scan_always_enabled 0"),
            restoreCmds = listOf("settings put global wifi_scan_always_enabled 1")
        ),
        OptimizationItem(
            id = "play_protect",
            isRecommended = true,
            defaultChecked = true,
            name = "关闭 Play Protect 应用验证",
            desc = "加速旁加载安装本地 APK（F-Droid / Obtainium）",
            applyCmds = listOf(
                "settings put global package_verifier_enable 0",
                "settings put global package_verifier_user_consent -1"
            ),
            restoreCmds = listOf(
                "settings put global package_verifier_enable 1",
                "settings put global package_verifier_user_consent 1"
            )
        ),
        OptimizationItem(
            id = "adb_verify",
            isRecommended = true,
            defaultChecked = false,
            name = "关闭 ADB 安装验证",
            desc = "消除 ADB 安装应用时的确认弹窗",
            applyCmds = listOf("settings put global verifier_verify_adb_installs 0"),
            restoreCmds = listOf("settings put global verifier_verify_adb_installs 1")
        ),
        OptimizationItem(
            id = "app_freezer",
            isRecommended = true,
            defaultChecked = true,
            name = "开启后台缓存应用冻结 (Android 11+)",
            desc = "启用安卓原生冻结机制，后台挂起不耗电、切回秒开",
            applyCmds = listOf("settings put global cached_apps_freezer enabled"),
            restoreCmds = listOf("settings put global cached_apps_freezer default")
        ),
        OptimizationItem(
            id = "error_report",
            isRecommended = true,
            defaultChecked = true,
            name = "关闭系统错误报告上报",
            desc = "减少系统崩溃时的后台遥测日志收集",
            applyCmds = listOf("settings put global send_action_app_error 0"),
            restoreCmds = listOf("settings put global send_action_app_error 1")
        ),

        // 可选与进阶设置（仅手动勾选）
        OptimizationItem(
            id = "force_resize",
            isRecommended = false,
            defaultChecked = false,
            name = "强制所有应用支持分屏与小窗（平板神器）",
            desc = "解除部分 App 禁止分屏/自由窗口限制（重启生效）",
            applyCmds = listOf(
                "settings put global force_resizable_activities 1",
                "settings put global enable_freeform_support 1"
            ),
            restoreCmds = listOf(
                "settings put global force_resizable_activities 0",
                "settings put global enable_freeform_support 0"
            )
        ),
        OptimizationItem(
            id = "phantom_procs",
            isRecommended = false,
            defaultChecked = false,
            name = "解除后台“幽灵进程”查杀限制 (Android 12+)",
            desc = "防止模拟器、Termux 或多开应用在后台被系统杀掉",
            applyCmds = listOf(
                "settings put global settings_enable_monitor_phantom_procs false",
                "device_config put activity_manager max_phantom_processes 2147483647"
            ),
            restoreCmds = listOf(
                "settings delete global settings_enable_monitor_phantom_procs",
                "device_config delete activity_manager max_phantom_processes"
            )
        ),
        OptimizationItem(
            id = "private_dns",
            isRecommended = false,
            defaultChecked = false,
            name = "私人 DNS (AdGuard 全局去广告)",
            desc = "使用 dns.adguard-dns.com 拦截应用内广告与追踪器",
            applyCmds = listOf(
                "settings put global private_dns_mode hostname",
                "settings put global private_dns_specifier dns.adguard-dns.com"
            ),
            restoreCmds = listOf(
                "settings put global private_dns_mode opportunistic",
                "settings delete global private_dns_specifier"
            )
        ),
        OptimizationItem(
            id = "heads_up",
            isRecommended = false,
            defaultChecked = false,
            name = "禁用通知弹窗（关闭横幅通知）",
            desc = "玩游戏/看剧时屏蔽顶部弹出的横幅通知条",
            applyCmds = listOf("settings put global heads_up_notifications_enabled 0"),
            restoreCmds = listOf("settings put global heads_up_notifications_enabled 1")
        ),
        OptimizationItem(
            id = "stay_awake",
            isRecommended = false,
            defaultChecked = false,
            name = "充电时保持亮屏",
            desc = "防止 AC / USB / 无线充电时自动息屏",
            applyCmds = listOf("settings put global stay_on_while_plugged_in 7"),
            restoreCmds = listOf("settings put global stay_on_while_plugged_in 0")
        ),
        OptimizationItem(
            id = "haptic_off",
            isRecommended = false,
            defaultChecked = false,
            name = "关闭触觉反馈",
            desc = "完全禁用触摸与键盘操作振动",
            applyCmds = listOf("settings put system haptic_feedback_enabled 0"),
            restoreCmds = listOf("settings put system haptic_feedback_enabled 1")
        )
    )
}
