package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.widget.Toast
import com.example.model.AppInfo
import com.example.model.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledApps(favoritePackageNames: Set<String>, hiddenPackageNames: Set<String>): List<AppInfo> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfoList: List<ResolveInfo> = try {
            packageManager.queryIntentActivities(launcherIntent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        val myPackageName = context.packageName

        val apps = resolveInfoList
            .filter { it.activityInfo != null && it.activityInfo.packageName != myPackageName }
            .map { resolveInfo ->
                val pkgName = resolveInfo.activityInfo.packageName
                val className = resolveInfo.activityInfo.name
                val label = try {
                    resolveInfo.loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    pkgName
                }
                val iconDrawable = try {
                    resolveInfo.loadIcon(packageManager)
                } catch (e: Exception) {
                    null
                }
                val iconBitmap = iconDrawable?.toComposeImageBitmap()

                AppInfo(
                    packageName = pkgName,
                    className = className,
                    label = label,
                    iconBitmap = iconBitmap,
                    isFavorite = favoritePackageNames.contains(pkgName)
                )
            }
            .filter { !hiddenPackageNames.contains(it.packageName) }
            .sortedBy { it.label.lowercase() }

        apps
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                Toast.makeText(context, "Cannot open $packageName: App not launchable", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch app: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
