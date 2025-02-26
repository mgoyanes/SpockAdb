package com.mgm.adbtools

import ProcessCommand
import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.wm.ToolWindow
import com.intellij.psi.PsiClass
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import org.jetbrains.android.sdk.AndroidSdkUtils
import com.mgm.adbtools.avsb.AVSBAdbController
import com.mgm.adbtools.command.AnimatorDurationScaleCommand
import com.mgm.adbtools.command.ClearAppDataAndRestartCommand
import com.mgm.adbtools.command.ClearAppDataCommand
import com.mgm.adbtools.command.ConnectDeviceOverIPCommand
import com.mgm.adbtools.avsb.DMSCommand
import com.mgm.adbtools.avsb.KeyEventCommand
import com.mgm.adbtools.avsb.OpenStatusCommand
import com.mgm.adbtools.avsb.AppsCommand
import com.mgm.adbtools.avsb.GetAVSBInfoCommand
import com.mgm.adbtools.avsb.InstallApkCommand
import com.mgm.adbtools.avsb.OpenSettingsCommand
import com.mgm.adbtools.command.EnableDisableDarkModeCommand
import com.mgm.adbtools.command.EnableDisableShowLayoutBoundsCommand
import com.mgm.adbtools.command.EnableDisableShowTapsCommand
import com.mgm.adbtools.command.FirebaseCommand
import com.mgm.adbtools.command.ForceKillAppCommand
import com.mgm.adbtools.command.GetActivityCommand
import com.mgm.adbtools.command.GetApplicationBackStackCommand
import com.mgm.adbtools.command.GetApplicationIDCommand
import com.mgm.adbtools.command.GetApplicationPermission
import com.mgm.adbtools.command.GetBackStackCommand
import com.mgm.adbtools.command.GetFragmentsCommand
import com.mgm.adbtools.command.GetPackageNameCommand
import com.mgm.adbtools.command.GrantPermissionCommand
import com.mgm.adbtools.command.InputOnDeviceCommand
import com.mgm.adbtools.command.Network
import com.mgm.adbtools.command.NetworkRateLimitCommand
import com.mgm.adbtools.command.OpenAccountsCommand
import com.mgm.adbtools.command.OpenAppSettingsCommand
import com.mgm.adbtools.command.OpenDeepLinkCommand
import com.mgm.adbtools.command.OpenDeveloperOptionsCommand
import com.mgm.adbtools.command.ProcessDeathCommand
import com.mgm.adbtools.avsb.ProxyCommand
import com.mgm.adbtools.avsb.TalkbackToggleCommand
import com.mgm.adbtools.command.RestartAppCommand
import com.mgm.adbtools.command.RestartAppWithDebuggerCommand
import com.mgm.adbtools.command.RevokePermissionCommand
import com.mgm.adbtools.command.ToggleNetworkCommand
import com.mgm.adbtools.command.TransitionAnimatorScaleCommand
import com.mgm.adbtools.command.UninstallAppCommand
import com.mgm.adbtools.command.WindowAnimatorScaleCommand
import com.mgm.adbtools.models.ActivityData
import com.mgm.adbtools.models.BackStackData
import com.mgm.adbtools.models.FragmentData
import com.mgm.adbtools.notification.CommonNotifier
import com.mgm.adbtools.premission.ListItem
import java.awt.Window
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.math.max


class AdbControllerImp(private val project: Project, private var debugBridge: AndroidDebugBridge?, private var toolWindow: ToolWindow? = null) :
    AdbController,
    AVSBAdbController,
    AndroidDebugBridge.IDeviceChangeListener,
    AndroidDebugBridge.IDebugBridgeChangeListener {

    companion object {
        private const val INDENT = "\t\t\t\t"
        private const val ACTIVITY_KILLED = " [Killed]"
    }

    private var updateDeviceList: ((List<IDevice>) -> Unit)? = null

    init {
        AndroidDebugBridge.addDeviceChangeListener(this)
    }

    private fun getApplicationID(device: IDevice) =
        GetApplicationIDCommand().execute(Any(), project, device).toString()

    private fun getPackageName(device: IDevice) = GetPackageNameCommand().execute(Any(), project, device).toString()

    override fun refresh() {
        AndroidDebugBridge.terminate()
        debugBridge?.startAdb(Long.MAX_VALUE, TimeUnit.MILLISECONDS)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
    }

    override fun refresh2() {
        debugBridge?.restart(Long.MAX_VALUE, TimeUnit.MILLISECONDS)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
    }

    override fun refresh3() {
        AndroidDebugBridge.terminate()
        debugBridge?.restart(Long.MAX_VALUE, TimeUnit.MILLISECONDS)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
    }

    override fun refresh4() {
        debugBridge = AndroidSdkUtils.getDebugBridge(project)

        AndroidDebugBridge.removeDeviceChangeListener(this)
        AndroidDebugBridge.addDeviceChangeListener(this)
    }

    override fun connectedDevices(block: (devices: List<IDevice>) -> Unit) {
        updateDeviceList = block
        updateDeviceList?.invoke(debugBridge?.devices?.toList() ?: listOf())
    }


    //region IDebugBridgeChangeListener
    override fun bridgeChanged(bridge: AndroidDebugBridge?) {
        debugBridge = bridge

        showSuccess("bridgeChanged")
    }

    override fun restartInitiated() {
        super.restartInitiated()
        showSuccess("restartInitiated")
    }

    override fun restartCompleted(isSuccessful: Boolean) {
        super.restartCompleted(isSuccessful)
        showSuccess("restartCompleted isSuccessful=$isSuccessful")
    }

    override fun initializationError(exception: java.lang.Exception?) {
        super.initializationError(exception)
        showError("initializationError. Error was=${exception?.message}")
    }
    //endregion

    //region IDeviceChangeListener
    override fun deviceConnected(iDevice: IDevice) {
        updateDeviceList?.invoke(debugBridge?.devices?.toList() ?: listOf())
    }

    override fun deviceDisconnected(iDevice: IDevice) {
        updateDeviceList?.invoke(debugBridge?.devices?.toList() ?: listOf())
    }

    override fun deviceChanged(iDevice: IDevice, i: Int) {}
    //endregion

    override fun currentBackStack(device: IDevice) {
        val activitiesList = mutableListOf<String>()
        val activitiesClass: List<BackStackData> = GetBackStackCommand().execute(Any(), project, device)

        activitiesClass.forEachIndexed { index, activityData ->
            activitiesList.add("\t[$index]-${activityData.appPackage}")

            activityData.activitiesList.forEachIndexed { activityIndex, activityData ->
                activitiesList.add("\t\t\t\t[$activityIndex]-${activityData.activity}${if (activityData.isKilled) ACTIVITY_KILLED else EMPTY}")
            }
        }

        val list = JBList(activitiesList)
        showClassPopup(
            "Activities",
            list,
            activitiesList.map { it.trim().replace(ACTIVITY_KILLED, EMPTY).substringAfter(HYPHEN).psiClassByNameFromProjct(project) }
        )
    }

    override fun currentApplicationBackStack(device: IDevice) {
        val packageName = getPackageName(device)
        val applicationID = getApplicationID(device)
        val backStackList = mutableMapOf<String, Int>()
        val backStackData: List<ActivityData> = GetApplicationBackStackCommand().execute(listOf(packageName, applicationID), device)

        backStackData
            .sortedByDescending { it.activityStackPosition }
            .forEachIndexed { index, activityData ->
                backStackList[activityData.activity] = index

                activityData.fragment.forEachIndexed { fragmentIndex, fragmentData ->
                    backStackList[fragmentData.fragment] = fragmentIndex

                    addInnerFragmentsToList(fragmentData = fragmentData, fragmentsList = backStackList, indent = INDENT, includeIndex = false)
                }
            }

        val list = JBList(backStackList.keys.toList())
        var margin: Int
        list.installCellRenderer { o: Any ->
            val displayTitle: String
            val title = o.toString()
            displayTitle = if (title.contains(DOT)) {
                margin = 10
                StringBuilder().insert(ZERO, "[${backStackList[title]}]-").append(
                    (title.split(DOT).lastOrNull() ?: EMPTY) + " [Activity]${if (backStackData.firstOrNull { it.activity == title }?.isKilled == true) ACTIVITY_KILLED else EMPTY}"
                ).toString()
            } else {
                margin = 20
                StringBuilder(title).insert(max(ZERO, title.indexOfLast { char -> char == TAB }), "[${backStackList[title]}]-").append(" [Fragment]").toString()
            }

            val label = JBLabel(displayTitle)
            label.border = JBUI.Borders.empty(5, margin, 5, 20)
            label
        }
        PopupChooserBuilder(list).apply {
            this.setTitle("Activities")
            this.setItemChoosenCallback {
                val current = backStackList.keys.elementAtOrNull(list.selectedIndex)
                current?.let {
                    if (it.contains(DASH))
                        it.trim().replace(ACTIVITY_KILLED, EMPTY).replaceFirst(DASH.toString(), EMPTY).psiClassByNameFromProjct(project)?.openIn(project)
                    else
                        it.trim().psiClassByNameFromCache(project)?.openIn(project)
                }
            }
            this.createPopup().showCenteredInCurrentWindow(project)
        }
    }

    override fun currentActivity(device: IDevice) {
        execute {
            val activity =
                GetActivityCommand().execute(Any(), project, device) ?: throw Exception("No activities found")
            activity.psiClassByNameFromProjct(project)?.openIn(project)
                ?: throw Exception("class $activity  Not Found")
        }
    }

    override fun currentFragment(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)

            val fragmentsClass = GetFragmentsCommand().execute(applicationID, project, device)

            if (fragmentsClass.size > 1) {
                val fragmentsList = mutableMapOf<String, Int>()

                fragmentsClass.forEachIndexed { index, fragmentData ->
                    fragmentsList["\t[$index]-${fragmentData.fragment}"] = index

                    addInnerFragmentsToList(fragmentData = fragmentData, fragmentsList = fragmentsList, indent = INDENT, includeIndex = true)
                }

                val list = JBList(fragmentsList.keys.toList())
                showClassPopup(
                    "Fragments",
                    list,
                    fragmentsList.map { it.key.trim().substringAfter(HYPHEN).psiClassByNameFromCache(project) }
                )
            } else {
                fragmentsClass
                    .firstOrNull()
                    ?.let {
                        it
                            .fragment
                            .psiClassByNameFromCache(project)
                            ?.openIn(project)
                            ?: throw Exception("Class $it Not Found")
                    }
            }
        }
    }

    override fun forceKillApp(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            ForceKillAppCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID force killed")
        }
    }

    override fun testProcessDeath(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            ProcessDeathCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID killed. App launched.")
        }
    }

    override fun restartApp(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            RestartAppCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID Restart")
        }
    }

    override fun restartAppWithDebugger(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            RestartAppWithDebuggerCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID Restarted with debugger")
        }
    }

    override fun clearAppData(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            ClearAppDataCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID data cleared")
        }
    }

    override fun clearAppDataAndRestart(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            ClearAppDataAndRestartCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID data cleared and restarted")
        }
    }

    override fun uninstallApp(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            UninstallAppCommand().execute(applicationID, project, device)
            showSuccess("application $applicationID uninstalled")
        }
    }

    override fun getApplicationPermissions(device: IDevice, block: (devices: List<ListItem>) -> Unit) {
        execute {
            val applicationID = getApplicationID(device)
            val permissions = GetApplicationPermission().execute(applicationID, project, device)
            if (permissions.isNotEmpty()) {
                block(permissions)
            } else {
                error("Your Application Doesn't Require any of Runtime Permissions ")
            }
        }
    }

    override fun grantOrRevokeAllPermissions(device: IDevice, permissionOperation: GetApplicationPermission.PermissionOperation) {
        getApplicationPermissions(device) { permissionsList ->
            val applicationID = getApplicationID(device)

            val operation: (ListItem) -> Unit = when (permissionOperation) {
                GetApplicationPermission.PermissionOperation.GRANT ->
                    { permission -> GrantPermissionCommand().execute(applicationID, permission, project, device) }

                GetApplicationPermission.PermissionOperation.REVOKE ->
                    { permission -> RevokePermissionCommand().execute(applicationID, permission, project, device) }
            }

            permissionsList
                .forEach { permission -> operation(permission) }
                .also { showSuccess("All permissions ${permissionOperation.operationResult}") }
        }
    }

    override fun revokePermission(device: IDevice, listItem: ListItem) {
        execute {
            val applicationID = getApplicationID(device)
            RevokePermissionCommand().execute(applicationID, listItem, project, device)
            showSuccess("permission $listItem revoked")
        }
    }

    override fun grantPermission(device: IDevice, listItem: ListItem) {
        execute {
            val applicationID = getApplicationID(device)
            GrantPermissionCommand().execute(applicationID, listItem, project, device)
            showSuccess("permission $listItem granted")
        }
    }

    override fun connectDeviceOverIp(ip: String) {
        execute {
            ConnectDeviceOverIPCommand().execute(ip, project)
            showSuccess("connected to $ip")
        }
    }

    override fun enableDisableShowTaps(device: IDevice) {
        execute {
            val result = EnableDisableShowTapsCommand().execute(Any(), project, device)
            showSuccess(result)
        }
    }

    override fun enableDisableShowLayoutBounds(device: IDevice) {
        execute {
            val result = EnableDisableShowLayoutBoundsCommand().execute(Any(), project, device)
            showSuccess(result)
        }
    }

    override fun enableDisableDarkMode(device: IDevice) {
        execute {
            val result = EnableDisableDarkModeCommand().execute(Any(), project, device)
            showSuccess(result)
        }
    }

    override fun setWindowAnimatorScale(scale: String, device: IDevice) {
        execute {
            val result = WindowAnimatorScaleCommand().execute(scale, project, device)
            showSuccess(result)
        }
    }

    override fun setTransitionAnimatorScale(scale: String, device: IDevice) {
        execute {
            val result = TransitionAnimatorScaleCommand().execute(scale, project, device)
            showSuccess(result)
        }
    }

    override fun setAnimatorDurationScale(scale: String, device: IDevice) {
        execute {
            val result = AnimatorDurationScaleCommand().execute(scale, project, device)
            showSuccess(result)
        }
    }

    override fun setNetworkRateLimit(scale: String, device: IDevice) {
        execute {
            val result = NetworkRateLimitCommand().execute(scale, project, device)
            showSuccess(result)
        }
    }

    override fun toggleNetwork(device: IDevice, network: Network) {
        execute {
            val result = ToggleNetworkCommand().execute(network, project, device)
            showSuccess(result)
        }
    }

    override fun inputOnDevice(input: String, device: IDevice) {
        execute {
            val result = InputOnDeviceCommand().execute(input, project, device)
            showSuccess(result)
        }
    }

    override fun setDMS(dms: String, device: IDevice) {
        execute {
            val result = DMSCommand().execute(dms, project, device)

            showSuccess(result)
        }
    }

    override fun openStatus(device: IDevice) {
        execute {
            val result = OpenStatusCommand().execute(project, device)

            showSuccess(result)
        }
    }

    override fun openSettings(device: IDevice) {
        execute {
            val result = OpenSettingsCommand().execute(project, device)

            showSuccess(result)
        }
    }

    override fun inputKeyEvent(keyEvent: Int, device: IDevice) {
        execute {
            val result = KeyEventCommand().execute(keyEvent, project, device)

            if (result != EMPTY) {
                showSuccess(result)
            }
        }
    }

    override fun openApp(app: String, device: IDevice) {
        execute {
            val result = AppsCommand().execute(app, AppsCommand.AppAction.OPEN, project, device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun closeApp(app: String, device: IDevice) {
        execute {
            val result = AppsCommand().execute(app, AppsCommand.AppAction.CLOSE, project, device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun processCommand(command: ProcessCommand.Command) {
        execute {
            val result = ProcessCommand().execute(command, project)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun openAVSBAppSettings(device: IDevice) {
        execute {
            showSuccess(com.mgm.adbtools.avsb.OpenAppSettingsCommand().execute(project, device))
        }
    }

    override fun setProxy(hostname: String?, port: String?, device: IDevice) {
        execute {
            val result = ProxyCommand().setProxy(hostname, port, project, device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun clearProxy(device: IDevice) {
        execute {
            val result = ProxyCommand().clearProxy(project, device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun toggleTalkback(device: IDevice) {
        execute {
            val result = TalkbackToggleCommand().execute(device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun copyBoxInfoToClipboard(device: IDevice) {
        execute {
            val result = GetAVSBInfoCommand().execute(device)
            if (result != EMPTY) showSuccess(result)
        }
    }

    override fun installApk(device: IDevice) {
        val desktopPath = System.getProperty("user.home") + File.separator + "Desktop"
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Select an APK File"
            fileSelectionMode = JFileChooser.FILES_ONLY
            currentDirectory = File(desktopPath)
        }

        val parentWindow: Window? = SwingUtilities.getWindowAncestor(toolWindow?.component)

        val dialogResult = fileChooser.showOpenDialog(parentWindow)

        if (dialogResult == JFileChooser.APPROVE_OPTION) {
            val selectedFile: File = fileChooser.selectedFile

            if (!selectedFile.name.endsWith(".apk", ignoreCase = true)) {
                JOptionPane.showMessageDialog(parentWindow, "Error: Selected file is not an APK!", "Invalid File", JOptionPane.ERROR_MESSAGE)
                return
            }

            execute {
                showSuccess("Please wait while app is being installed")
                val result = InstallApkCommand().execute(selectedFile.absolutePath, device)
                showSuccess(result)
            }
        }
    }

    private fun showError(message: String) {
        CommonNotifier.showNotifier(project = project, content = message, type = NotificationType.ERROR)
    }

    private fun showSuccess(message: String) {
        CommonNotifier.showNotifier(project = project, content = message, type = NotificationType.INFORMATION)
    }

    private fun execute(execute: () -> Unit) {
        try {
            execute.invoke()
        } catch (e: Exception) {
            showError(e.message ?: "not found")
        }
    }

    private fun showClassPopup(
        title: String,
        list: JBList<String>,
        classes: List<PsiClass?>
    ) {
        list.installCellRenderer { displayTitle ->
            val label = JBLabel(displayTitle)
            label.border = JBUI.Borders.empty(5, 5, 5, 20)
            label
        }

        PopupChooserBuilder(list).apply {
            this.setTitle(title)
            this.setItemChoosenCallback {
                classes.getOrNull(list.selectedIndex)?.openIn(project)
            }
            this.createPopup().showCenteredInCurrentWindow(project)
        }
    }

    private fun addInnerFragmentsToList(
        fragmentData: FragmentData,
        fragmentsList: MutableMap<String, Int>,
        indent: String,
        includeIndex: Boolean,
    ) {
        fragmentData.innerFragments.forEachIndexed { fragmentIndex, innerFragmentData ->
            fragmentsList[
                if (includeIndex) {
                    "$indent[$fragmentIndex]-${innerFragmentData.fragment}"
                } else {
                    "$indent${innerFragmentData.fragment}"
                }
            ] = fragmentIndex
            addInnerFragmentsToList(innerFragmentData, fragmentsList, "$INDENT$indent", includeIndex)
        }
    }

    override fun openDeveloperOptions(device: IDevice) {
        execute {
            showSuccess(OpenDeveloperOptionsCommand().execute(device))
        }
    }

    override fun openDeepLink(input: String, device: IDevice) {
        execute {
            val result = OpenDeepLinkCommand().execute(input, project, device)
            showSuccess(result)
        }
    }

    override fun openAccounts(device: IDevice) {
        execute {
            showSuccess(OpenAccountsCommand().execute(device))
        }
    }

    override fun openAppSettings(device: IDevice) {
        execute {
            val applicationID = getApplicationID(device)
            showSuccess(OpenAppSettingsCommand().execute(applicationID, project, device))
        }
    }

    override fun setFirebaseDebugApp(device: IDevice, firebaseDebugApp: String) {
        execute {
            FirebaseCommand().execute(getApplicationID(device), firebaseDebugApp, project, device)
        }
    }
}
