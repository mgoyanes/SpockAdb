package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ShellOutputReceiver
import spock.adb.getApiVersion
import spock.adb.models.ActivityData
import java.util.concurrent.TimeUnit

class GetBackStackCommand : Command<Any, List<ActivityData>> {

    companion object {
        const val ACTIVITY_DELIMITER = "Running activities (most recent first):"
        const val ACTIVITY_PREFIX = "Run #"
        const val HIST_PREFIX = "* Hist"
        const val ACTIVITY_PREFIX_DELIMITER = "."
        const val API_VERSION_11 = 11
        const val EMPTY = ""
        val extractAppRegex = Regex("(A=|I=|u0\\s)([a-zA-Z.]+)")
        val extractActivityRegex = Regex("(u0\\s[a-zA-Z.]+/)([a-zA-Z.]+)")
    }

    override fun execute(p: Any, project: Project, device: IDevice): List<ActivityData> {
        val shellOutputReceiver = ShellOutputReceiver()
        val apiVersion = device.getApiVersion()

        return when {
            apiVersion == null || apiVersion < API_VERSION_11 -> {
                device.executeShellCommand(
                    "dumpsys activity activities | sed -En -e '/Running activities/,/Run #0/p'",
                    shellOutputReceiver,
                    15L,
                    TimeUnit.SECONDS
                )
                getCurrentRunningActivities(shellOutputReceiver.toString())
            }
            else -> {
                device.executeShellCommand(
                    "dumpsys activity activities | grep Hist",
                    shellOutputReceiver,
                    15L,
                    TimeUnit.SECONDS
                )
                getCurrentRunningActivitiesAboveApi11(shellOutputReceiver.toString())
            }
        }
    }

    private fun getCurrentRunningActivities(bulkActivitiesData: String): List<ActivityData> {
        lateinit var appPackage: String

        return bulkActivitiesData
            .split(ACTIVITY_DELIMITER)
            .filter { it.isNotBlank() }
            .mapNotNull { bulkAppData ->
                appPackage = extractAppRegex.find(bulkAppData)?.groups?.lastOrNull()?.value ?: return@mapNotNull null

                ActivityData(
                    appPackage = appPackage,
                    activitiesList = bulkAppData
                        .lines()
                        .filter { it.trim().startsWith(ACTIVITY_PREFIX) }
                        .mapNotNull { bulkActivityData: String ->
                            extractActivityRegex
                                .find(bulkActivityData)
                                ?.groups
                                ?.lastOrNull()
                                ?.value
                                ?.let { activityName ->
                                    when {
                                        activityName.startsWith(ACTIVITY_PREFIX_DELIMITER) -> "$appPackage$activityName"
                                        else -> activityName
                                    }
                                }
                        }
                )
            }
    }

    private fun getCurrentRunningActivitiesAboveApi11(bulkActivitiesData: String): List<ActivityData> {
        lateinit var appPackage: String

        return bulkActivitiesData
            .lines()
            .filter { it.trim().startsWith(HIST_PREFIX) }
            .groupBy(
                keySelector = {
                    appPackage = extractAppRegex.find(it)?.groups?.lastOrNull()?.value ?: EMPTY
                    appPackage
                },
                valueTransform = { bulkActivityData ->
                    extractActivityRegex.find(bulkActivityData)?.groups?.lastOrNull()?.value?.let { activityName ->
                        when {
                            activityName.startsWith(ACTIVITY_PREFIX_DELIMITER) -> "$appPackage$activityName"
                            else -> activityName
                        }
                    } ?: EMPTY
                }
            )
            .filter { it.key.isNotBlank() }
            .map { activityData -> ActivityData(activityData.key, activityData.value) }
    }
}
