package com.aks_labs.tulsi.helpers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.core.content.FileProvider
import com.aks_labs.tulsi.BuildConfig
import com.aks_labs.tulsi.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.json.responseJson
import java.io.File
import org.json.JSONObject

private const val TAG = "UPDATER"

class Updater(
    private val context: Context,
	private val coroutineScope: CoroutineScope
) {
    private val currentVersionCode = BuildConfig.VERSION_CODE

    private var githubResponseBody: MutableState<JSONObject?> = mutableStateOf(null)

    val githubVersionName = derivedStateOf {
    	if (githubResponseBody.value == null) "" else githubResponseBody.value!!["tag_name"].toString()
    }

    private val githubVersionCode = derivedStateOf {
        if (githubVersionName.value == "") 0
        else githubVersionName.value
            .replace(".", "")
            .removeSuffix("-beta")
            .removePrefix("v")
            .toInt()
    }

    private val updateFile by derivedStateOf {
        val file = File(context.appStorageDir, "Gallery_signed_release_${githubVersionName.value}.apk")
        file.parentFile?.mkdirs()
        file
    }

    /** check for updates and return whether there is one */
    val hasUpdates = derivedStateOf { githubVersionCode.value > currentVersionCode }

    init {
        updateFile.parentFile
                ?.listFiles()
                ?.filter { file ->
                    file.name.endsWith(".apk") && file.name != updateFile.name
                }
                ?.forEach { file ->
                    file.delete()
                }
    }

    fun refresh (onRefresh: (state: CheckUpdateState) -> Unit) {
    	coroutineScope.launch(Dispatchers.IO) {
    		async {
    			onRefresh(CheckUpdateState.Checking)
		        val url = "https://api.github.com/repos/AKS-Labs/Tulsi/releases/latest"

		        val body = try {
		        	Fuel.get(url).responseJson().third.fold(
                        success = { result ->
                            result.obj()
                        },

                        failure = { error ->
                            Log.e(TAG, error.toString())
                            error.printStackTrace()

                            onRefresh(CheckUpdateState.Failed)
                            return@async
                        }
                    )
		       	} catch (e: Throwable) {
		       		Log.e(TAG, e.toString())
		       		e.printStackTrace()

		       		onRefresh(CheckUpdateState.Failed)
		       		return@async
		       	}

		        githubResponseBody.value = body
		        Log.d(TAG, "body of https response is $body")
		        Log.d(TAG, "github version name is ${githubVersionName.value}")
		        Log.d(TAG, "github version code is ${githubVersionCode.value}")
		        Log.d(TAG, "app has updates? ${hasUpdates.value}")

		        onRefresh(CheckUpdateState.Succeeded)
    		}.await()
   		}
    }

    /** Update functionality has been removed */
    fun startUpdate(
        progress: (progress: Float) -> Unit,
        onDownloadStopped: (success: Boolean) -> Unit
    ) {
        Log.d(TAG, "Update functionality has been removed")
        onDownloadStopped(false)
    }

    /** Update functionality has been removed */
    fun installUpdate() {
        Log.d(TAG, "Update functionality has been removed")
    }

    fun getChangelog() : String =
        githubResponseBody.value?.let {
            it["body"]
                .toString()
        } ?: "No Changelog Available"
}


enum class CheckUpdateState {
	Checking,
	Failed,
	Succeeded
}


