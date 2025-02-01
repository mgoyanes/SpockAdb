package spock.adb

import ProcessCommand
import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import spock.adb.avsb.AVSBAdbController
import spock.adb.command.AnimatorDurationScaleCommand
import spock.adb.avsb.DMSCommand
import spock.adb.avsb.KeyEventCommand
import spock.adb.command.DontKeepActivitiesState
import spock.adb.command.EnableDarkModeState
import spock.adb.command.FirebaseCommand
import spock.adb.command.GetApplicationPermission
import spock.adb.command.Network
import spock.adb.command.NetworkRateLimitCommand
import spock.adb.command.ShowLayoutBoundsState
import spock.adb.command.ShowTapsState
import spock.adb.command.TransitionAnimatorScaleCommand
import spock.adb.command.WindowAnimatorScaleCommand
import spock.adb.premission.CheckBoxDialog
import java.awt.event.ActionEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField

class SpockAdbViewer(private val project: Project) : SimpleToolWindowPanel(true) {
    private lateinit var rootPanel: JPanel
    private lateinit var permissionPanel: JPanel
    private lateinit var networkPanel: JPanel
    private lateinit var developerPanel: JPanel
    private lateinit var avsbPanel: JPanel
    private lateinit var devicesListComboBox: JComboBox<String>
    private lateinit var currentActivityButton: JButton
    private lateinit var currentFragmentButton: JButton
    private lateinit var clearAppDataButton: JButton
    private lateinit var clearAppDataAndRestartButton: JButton
    private lateinit var uninstallAppButton: JButton
    private lateinit var refresh: JButton
    private lateinit var refresh2: JButton
    private lateinit var refresh3: JButton
    private lateinit var refresh4: JButton
    private lateinit var permissionButton: JButton
    private lateinit var grantAllPermissionsButton: JButton
    private lateinit var revokeAllPermissionsButton: JButton
    private lateinit var restartAppButton: JButton
    private lateinit var restartAppWithDebuggerButton: JButton
    private lateinit var forceKillAppButton: JButton
    private lateinit var testProcessDeathButton: JButton
    private lateinit var activitiesBackStackButton: JButton
    private lateinit var currentAppBackStackButton: JButton
    private lateinit var adbWifi: JButton
    private lateinit var setting: JButton
    private lateinit var devices: List<IDevice>
    private lateinit var enableDisableDontKeepActivities: JCheckBox
    private lateinit var enableDisableShowTaps: JCheckBox
    private lateinit var enableDisableShowLayoutBounds: JCheckBox
    private lateinit var enableDisableDarkMode: JCheckBox
    private lateinit var windowAnimatorScaleComboBox: JComboBox<String>
    private lateinit var transitionAnimatorScaleComboBox: JComboBox<String>
    private lateinit var animatorDurationScaleComboBox: JComboBox<String>
    private lateinit var networkRateLimitComboBox: JComboBox<String>
    private lateinit var wifiToggle: JButton
    private lateinit var mobileDataToggle: JButton
    private lateinit var inputOnDeviceTextField: JTextField
    private lateinit var openDeepLinkTextField: JTextField
    private lateinit var inputOnDeviceButton: JButton
    private lateinit var openDeepLinkButton: JButton
    private lateinit var openDeveloperOptionsButton: JButton
    private lateinit var openAccountsButton: JButton
    private lateinit var openAppSettingsButton: JButton
    private lateinit var firebaseButton: JButton
    private lateinit var firebaseTextField: JTextField
    private var selectedIDevice: IDevice? = null
    private lateinit var dmsComboBox: JComboBox<String>
    private lateinit var avsbOpenStatus: JButton
    private lateinit var avsbOpenSettings: JButton
    private lateinit var avsbEPG: JButton
    private lateinit var avsbBack: JButton
    private lateinit var avsbExit: JButton
    private lateinit var avsbReboot: JButton
    private lateinit var avsbUninstall: JButton
    private lateinit var avsbForceKill: JButton
    private lateinit var avsbClearData: JButton
    private lateinit var avsbPower: JButton
    private lateinit var avsbHome: JButton
    private lateinit var avsbSearch: JButton
    private lateinit var avsbAllApps: JButton
    private lateinit var avsbAppsOpen: JButton
    private lateinit var avsbAppsClose: JButton
    private lateinit var avsbAppsComboBox: JComboBox<String>
    private lateinit var avsbAppSettingsButton: JButton
    private lateinit var avsbProxySet: JButton
    private lateinit var avsbProxyNone: JButton
    private lateinit var avsbProxyHostname: JTextField
    private lateinit var avsbProxyPort: JTextField
    private lateinit var avsbTalkback: JButton
    private lateinit var adbController: AdbController

    private val showTapsActionListener: (ActionEvent) -> Unit = {
        selectedIDevice?.let { device ->
            adbController.enableDisableShowTaps(device)
        }
    }

    private val showLayoutBoundsActionListener: (ActionEvent) -> Unit = {
        selectedIDevice?.let { device ->
            adbController.enableDisableShowLayoutBounds(device)
            device.refreshUi()
        }
    }

    private val showDarkModeActionListener: (ActionEvent) -> Unit = {
        selectedIDevice?.let { device ->
            adbController.enableDisableDarkMode(device)
            device.refreshUi()
        }
    }

    private val windowAnimatorScaleActionListener: (ActionEvent) -> Unit = {
        selectedIDevice?.let { device ->
            adbController.setWindowAnimatorScale(
                windowAnimatorScaleComboBox.selectedItem as String,
                device
            )
        }
    }

    private val transitionAnimatorScaleActionListener: (ActionEvent) -> Unit = {
        selectedIDevice?.let { device ->
            adbController.setTransitionAnimatorScale(
                transitionAnimatorScaleComboBox.selectedItem as String,
                device

            )
        }
    }

    private val animatorDurationScaleActionListener: (ActionEvent) -> Unit = {
        selectedIDevice?.let { device ->
            adbController.setAnimatorDurationScale(
                animatorDurationScaleComboBox.selectedItem as String,
                device
            )
        }
    }

    private val networkRateLimitActionListener: (ActionEvent) -> Unit = {
        selectedIDevice?.let { device ->
            adbController.setNetworkRateLimit(
                networkRateLimitComboBox.selectedItem as String,
                device
            )
        }
    }

    private val dmsActionListener: (ActionEvent) -> Unit = {
        selectedIDevice?.let { device ->
            (adbController as AVSBAdbController).setDMS(
                dmsComboBox.selectedItem as String,
                device
            )

            CoroutineScope(Dispatchers.IO)
                .launch {
                    delay(2000)
                    val receiver = ShellOutputReceiver()
                    device.executeShellCommandWithTimeout("pm clear $AVSB_PACKAGE ~", receiver, NO_TIME_TO_OUTPUT_RESPONSE)
                }
        }
    }

    init {
        setContent(JScrollPane(rootPanel))
        setToolWindowListener()
        AppSettingService.getInstance().run {
            state?.let {
                updateUi(it)
            }
        }
    }

    fun initPlugin(adbController: AdbController) {
        this.adbController = adbController

        updateDevicesList()

        setting.isEnabled = true
        setting.isVisible = true
        setting.addActionListener {
            AppSettingService.getInstance().run {
                state?.let {
                    val dialog = CheckBoxDialog(it.list) { selectedItem ->
                        println(selectedItem)
                        this.loadState(it.copy(list = it.list.map { item ->
                            if (item.name == selectedItem.name)
                                item.copy(isSelected = selectedItem.isSelected)
                            else item
                        }))
                        updateUi(it)
                    }
                    dialog.setLocationRelativeTo(null)
                    dialog.pack()
                    dialog.isVisible = true
                }

            }

        }
        adbWifi.addActionListener {
            val ip = Messages.showInputDialog(
                "Enter You Android Device IP address",
                "Device connect over Wifi",
                null,
                EMPTY,
                IPAddressInputValidator()
            )
            ip?.let { adbController.connectDeviceOverIp(ip = ip) }
          }

        refresh.addActionListener {
            adbController.refresh()
            updateDevicesList()
        }
        refresh2.addActionListener {
            adbController.refresh2()
            updateDevicesList()
        }
        refresh3.addActionListener {
            adbController.refresh3()
            updateDevicesList()
        }
        refresh4.addActionListener {
            adbController.refresh4()
            updateDevicesList()
        }

        devicesListComboBox.addItemListener {
            selectedIDevice = devices[devicesListComboBox.selectedIndex]

        }
        setting.addActionListener {

        }
        activitiesBackStackButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.currentBackStack(device)
            }
        }
        currentAppBackStackButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.currentApplicationBackStack(device)
            }
        }
        currentActivityButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.currentActivity(device)
            }
        }
        currentFragmentButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.currentFragment(device)
            }
        }
        restartAppButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.restartApp(device)
            }
        }
        restartAppWithDebuggerButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.restartAppWithDebugger(device)
            }
        }
        forceKillAppButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.forceKillApp(device)
            }
        }
        testProcessDeathButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.testProcessDeath(device)
            }
        }
        clearAppDataButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.clearAppData(device)
            }
        }
        clearAppDataAndRestartButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.clearAppDataAndRestart(device)
            }
        }
        uninstallAppButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.uninstallApp(device)
            }
        }

        permissionButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.getApplicationPermissions(device) { list ->
                    val dialog = CheckBoxDialog(list) { selectedItem ->
                        if (selectedItem.isSelected)
                            adbController.grantPermission(device, selectedItem)
                        else
                            adbController.revokePermission(device, selectedItem)
                    }
                    dialog.pack()
                    dialog.isVisible = true

                }
            }
        }
        grantAllPermissionsButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.grantOrRevokeAllPermissions(device, GetApplicationPermission.PermissionOperation.GRANT)
            }
        }
        revokeAllPermissionsButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.grantOrRevokeAllPermissions(device, GetApplicationPermission.PermissionOperation.REVOKE)
            }
        }
        wifiToggle.addActionListener {
            selectedIDevice?.let { device ->
                adbController.toggleNetwork(device, Network.WIFI)
            }
        }
        mobileDataToggle.addActionListener {
            selectedIDevice?.let { device ->
                adbController.toggleNetwork(device, Network.MOBILE)
            }
        }
        inputOnDeviceButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.inputOnDevice(inputOnDeviceTextField.text, device)
            }
        }
        inputOnDeviceTextField.addActionListener { inputOnDeviceButton.doClick() }
        openDeveloperOptionsButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.openDeveloperOptions(device)
            }
        }
        openDeepLinkButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.openDeepLink(openDeepLinkTextField.text, device)
            }
        }
        openDeepLinkTextField.addActionListener { openDeepLinkButton.doClick() }
        openAccountsButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.openAccounts(device)
            }
        }
        openAppSettingsButton.addActionListener {
            selectedIDevice?.let { device ->
                adbController.openAppSettings(device)
            }
        }
        firebaseButton.addActionListener {
            selectedIDevice?.let { device ->
                val firebaseDebugApp = device.getFirebaseDebugApp()
                adbController.setFirebaseDebugApp(device, firebaseDebugApp)
                firebaseTextField.text = firebaseDebugApp
            }
        }

        avsbOpenStatus.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).openStatus(device)
            }
        }

        avsbOpenSettings.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).openSettings(device)
            }
        }

        avsbAppSettingsButton.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).openAVSBAppSettings(device)
            }
        }

        avsbEPG.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.EPG, device)
            }
        }

        avsbBack.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.BACK, device)
            }
        }

        avsbExit.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.EXIT, device)
            }
        }

        avsbReboot.addActionListener {
            (adbController as AVSBAdbController).processCommand(ProcessCommand.Command.REBOOT)
        }

        avsbUninstall.addActionListener {
            selectedIDevice?.let {
                (adbController as AVSBAdbController).processCommand(ProcessCommand.Command.UNINSTALL)
            }
        }

        avsbForceKill.addActionListener {
            selectedIDevice?.let {
                (adbController as AVSBAdbController).processCommand(ProcessCommand.Command.FORCE_KILL)
            }
        }

        avsbClearData.addActionListener {
            selectedIDevice?.let {
                (adbController as AVSBAdbController).processCommand(ProcessCommand.Command.CLEAR_DATA)
            }
        }

        avsbPower.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.POWER, device)
            }
        }

        avsbHome.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.HOME, device)
            }
        }

        avsbSearch.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.SEARCH, device)
            }
        }

        avsbAllApps.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).inputKeyEvent(KeyEventCommand.ALL_APPS, device)
            }
        }

        avsbAppsOpen.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).openApp(avsbAppsComboBox.selectedItem as String, device)
            }
        }

        avsbAppsClose.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).closeApp(avsbAppsComboBox.selectedItem as String, device)
            }
        }

        avsbProxySet.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).setProxy(avsbProxyHostname.text, avsbProxyPort.text, device)
            }
        }

        avsbProxyNone.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).clearProxy(device)
            }
        }

        avsbTalkback.addActionListener {
            selectedIDevice?.let { device ->
                (adbController as AVSBAdbController).toggleTalkback(device)
            }
        }
    }

    private fun updateUi(it: AppSetting) {
        it.list.map {
            when (SpockAction.valueOf(it.name.replace(" ", "_"))) {
                SpockAction.CURRENT_ACTIVITY -> currentActivityButton.isVisible = it.isSelected
                SpockAction.CURRENT_FRAGMENT -> currentFragmentButton.isVisible = it.isSelected
                SpockAction.CURRENT_APP_STACK -> currentAppBackStackButton.isVisible = it.isSelected
                SpockAction.BACK_STACK -> activitiesBackStackButton.isVisible = it.isSelected
                SpockAction.CLEAR_APP_DATA -> clearAppDataButton.isVisible = it.isSelected
                SpockAction.CLEAR_APP_DATA_RESTART -> clearAppDataAndRestartButton.isVisible = it.isSelected
                SpockAction.RESTART -> restartAppButton.isVisible = it.isSelected
                SpockAction.RESTART_DEBUG -> restartAppWithDebuggerButton.isVisible = it.isSelected
                SpockAction.TEST_PROCESS_DEATH -> testProcessDeathButton.isVisible = it.isSelected
                SpockAction.FORCE_KILL -> forceKillAppButton.isVisible = it.isSelected
                SpockAction.UNINSTALL -> uninstallAppButton.isVisible = it.isSelected
                SpockAction.TOGGLE_NETWORK -> networkPanel.isVisible = it.isSelected
                SpockAction.PERMISSIONS -> permissionPanel.isVisible = it.isSelected
                SpockAction.DEVELOPER_OPTIONS -> developerPanel.isVisible = it.isSelected
                SpockAction.INPUT -> {
                    inputOnDeviceButton.isVisible = it.isSelected
                    inputOnDeviceTextField.isVisible = it.isSelected
                }

                SpockAction.DEEP_LINK -> {
                    openDeepLinkButton.isVisible = it.isSelected
                    openDeepLinkTextField.isVisible = it.isSelected
                }
                SpockAction.AVSB -> avsbPanel.isVisible = it.isSelected
            }
            rootPanel.invalidate()
        }
    }

    private fun updateDevicesList() {
        adbController.connectedDevices { devices ->
            this.devices = devices
            selectedIDevice = this.devices.getOrElse(devices.indexOf(selectedIDevice)) { this.devices.getOrNull(0) }

            devicesListComboBox.model = DefaultComboBoxModel(
                devices.map { device ->
                    device.name
                }.toTypedArray()
            )
        }
    }

    private fun removeDeveloperOptionsListeners() {
        enableDisableShowTaps.actionListeners.forEach {
            enableDisableShowTaps.removeActionListener(it)
        }

        enableDisableShowLayoutBounds.actionListeners.forEach {
            enableDisableShowLayoutBounds.removeActionListener(it)
        }

        enableDisableDarkMode.actionListeners.forEach {
            enableDisableDarkMode.removeActionListener(it)
        }

        windowAnimatorScaleComboBox.actionListeners.forEach {
            windowAnimatorScaleComboBox.removeActionListener(it)
        }

        transitionAnimatorScaleComboBox.actionListeners.forEach {
            transitionAnimatorScaleComboBox.removeActionListener(it)
        }

        animatorDurationScaleComboBox.actionListeners.forEach {
            animatorDurationScaleComboBox.removeActionListener(it)
        }

        networkRateLimitComboBox.actionListeners.forEach {
            networkRateLimitComboBox.removeActionListener(it)
        }

        dmsComboBox.actionListeners.forEach {
            dmsComboBox.removeActionListener(it)
        }

        avsbAppsComboBox.actionListeners.forEach {
            avsbAppsComboBox.removeActionListener(it)
        }
    }

    private fun setDeveloperOptionsValues() {
        enableDisableDontKeepActivities.isSelected =
            selectedIDevice?.areDontKeepActivitiesEnabled() == DontKeepActivitiesState.ENABLED

        enableDisableShowTaps.isSelected = selectedIDevice?.areShowTapsEnabled() == ShowTapsState.ENABLED

        enableDisableShowLayoutBounds.isSelected =
            selectedIDevice?.areShowLayoutBoundsEnabled() == ShowLayoutBoundsState.ENABLED

        enableDisableDarkMode.isSelected =
            selectedIDevice?.isDarkModeEnabled() == EnableDarkModeState.ENABLED

        windowAnimatorScaleComboBox.selectedItem =
            WindowAnimatorScaleCommand.getWindowAnimatorScaleIndex(selectedIDevice?.getWindowAnimatorScale())

        transitionAnimatorScaleComboBox.selectedItem =
            TransitionAnimatorScaleCommand.getTransitionAnimatorScaleIndex(selectedIDevice?.getTransitionAnimationScale())

        animatorDurationScaleComboBox.selectedItem =
            AnimatorDurationScaleCommand.getAnimatorDurationScaleIndex(selectedIDevice?.getAnimatorDurationScale())

        networkRateLimitComboBox.selectedItem =
            NetworkRateLimitCommand.getGetNetworkRateLimitIndex(selectedIDevice?.getNetworkRateLimit())

        dmsComboBox.selectedItem = DMSCommand.getDMSIndex(selectedIDevice?.getDMS())

        setFirebaseData()
    }

    private fun setListeners() {
        enableDisableShowTaps.addActionListener(showTapsActionListener)

        enableDisableShowLayoutBounds.addActionListener(showLayoutBoundsActionListener)

        enableDisableDarkMode.addActionListener(showDarkModeActionListener)

        windowAnimatorScaleComboBox.addActionListener(windowAnimatorScaleActionListener)

        transitionAnimatorScaleComboBox.addActionListener(transitionAnimatorScaleActionListener)

        animatorDurationScaleComboBox.addActionListener(animatorDurationScaleActionListener)

        networkRateLimitComboBox.addActionListener(networkRateLimitActionListener)

        dmsComboBox.addActionListener(dmsActionListener)

        firebaseButton.addActionListener {
            setFirebaseData()
        }
    }

    private fun setFirebaseData() {
        val currentFirebaseDebugApp = selectedIDevice?.getFirebaseDebugApp()
        firebaseTextField.text = currentFirebaseDebugApp
        firebaseButton.text = when (currentFirebaseDebugApp) {
            FirebaseCommand.NO_DEBUG_APP -> "Enable Firebase Debug"
            else -> "Disable Firebase Debug"
        }
    }

    private fun setToolWindowListener() {

        ToolWindowManager
            .getInstance(project)
            .run {
                val toolWindow = getToolWindow("Spock ADB")
                if (toolWindow != null) {
                    project.messageBus.connect()
                        .subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
                            override fun stateChanged() {
                                if (toolWindow.isVisible) {
                                    removeDeveloperOptionsListeners()
                                    setDeveloperOptionsValues()
                                    setListeners()
                                }
                            }
                        })
                }
            }
    }
}
