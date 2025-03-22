package com.magus.myapplication

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.magus.myapplication.common.utils.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

private const val LOG_TAG = "HomeViewModel"

class HomeViewModel(private val application: Application) : ViewModel() {
    var canTranscribe by mutableStateOf(false)
        private set
    var dataLog by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set

    private val modelsPath = File(application.filesDir, "models")
    private val samplesPath = File(application.filesDir, "samples")
    private var whisperContext: WhisperContext? = null
    private var mediaPlayer: MediaPlayer? = null

    init {
        viewModelScope.launch {
            printSystemInfo()
            loadData()
        }
    }

    private suspend fun printSystemInfo() {
        printMessage(String.format("系统信息: %s\n", WhisperContext.getSystemInfo()))
    }

    private suspend fun loadData() {
        printMessage("正在加载数据...\n")
        try {
            copyAssets()
            loadBaseModel()
            canTranscribe = true
        } catch (e: Exception) {
            Log.w(LOG_TAG, e)
            printMessage("${e.localizedMessage}\n")
        }
    }

    private suspend fun printMessage(msg: String) = withContext(Dispatchers.Main) {
        dataLog += msg
    }

    private suspend fun copyAssets() = withContext(Dispatchers.IO) {
        modelsPath.mkdirs()
        samplesPath.mkdirs()
        application.copyData("models", modelsPath, ::printMessage)
        application.copyData("samples", samplesPath, ::printMessage)
        printMessage("所有数据已复制到工作目录。\n")
    }

    private suspend fun loadBaseModel() = withContext(Dispatchers.IO) {
        printMessage("正在加载模型...\n")
        val models = application.assets.list("models/")
        if (models != null && models.isNotEmpty()) {
            whisperContext = WhisperContext.createContextFromAsset(application.assets, "models/" + models[0])
            printMessage("已加载模型 ${models[0]}。\n")
        } else {
            printMessage("没有找到模型文件。\n")
        }
    }

    fun transcribeSample() = viewModelScope.launch {
        if (!canTranscribe) {
            return@launch
        }

        isLoading = true
        try {
            val firstSample = getFirstSample()
            printMessage("正在处理示例音频...\n")
            transcribeAudio(firstSample)
        } catch (e: Exception) {
            Log.w(LOG_TAG, e)
            printMessage("错误: ${e.localizedMessage}\n")
        } finally {
            isLoading = false
        }
    }

    private suspend fun getFirstSample(): File = withContext(Dispatchers.IO) {
        val files = samplesPath.listFiles()
        if (files.isNullOrEmpty()) {
            throw Exception("找不到示例音频文件")
        }
        return@withContext files.first()
    }

    private suspend fun readAudioSamples(file: File): FloatArray = withContext(Dispatchers.IO) {
        stopPlayback()
        startPlayback(file)
        return@withContext decodeWaveFile(file)
    }

    private suspend fun stopPlayback() = withContext(Dispatchers.Main) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private suspend fun startPlayback(file: File) = withContext(Dispatchers.Main) {
        mediaPlayer = MediaPlayer.create(application, file.absolutePath.toUri())
        mediaPlayer?.start()
    }

    private suspend fun transcribeAudio(file: File) {
        try {
            printMessage("正在读取音频样本... ")
            val data = readAudioSamples(file)
            printMessage("${data.size / (16000 / 1000)} 毫秒\n")
            printMessage("正在转录数据...\n")
            val start = System.currentTimeMillis()
            val text = whisperContext?.transcribeData(data)
            val elapsed = System.currentTimeMillis() - start
            printMessage("完成 ($elapsed 毫秒): \n$text\n")
        } catch (e: Exception) {
            Log.w(LOG_TAG, e)
            printMessage("${e.localizedMessage}\n")
        }
    }

    override fun onCleared() {
        runBlocking {
            whisperContext?.release()
            whisperContext = null
            stopPlayback()
        }
    }

    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                HomeViewModel(application)
            }
        }
    }
}

private suspend fun Context.copyData(
    assetDirName: String,
    destDir: File,
    printMessage: suspend (String) -> Unit
) = withContext(Dispatchers.IO) {
    assets.list(assetDirName)?.forEach { name ->
        val assetPath = "$assetDirName/$name"
        Log.v(LOG_TAG, "Processing $assetPath...")
        val destination = File(destDir, name)
        Log.v(LOG_TAG, "Copying $assetPath to $destination...")
        printMessage("正在复制 $name...\n")
        assets.open(assetPath).use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Log.v(LOG_TAG, "Copied $assetPath to $destination")
    }
}

// 从原来的代码中获取解码wave文件的函数
fun decodeWaveFile(file: File): FloatArray {
    val bytes = file.readBytes()
    val shorts = ShortArray(bytes.size / 2)
    
    for (i in shorts.indices) {
        shorts[i] = ((bytes[i * 2 + 1].toInt() shl 8) or (bytes[i * 2].toInt() and 0xFF)).toShort()
    }
    
    val floats = FloatArray(shorts.size)
    for (i in floats.indices) {
        floats[i] = shorts[i] / 32767.0f
    }
    
    return floats
}
