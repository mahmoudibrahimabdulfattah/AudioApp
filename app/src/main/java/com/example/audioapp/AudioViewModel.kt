package com.example.audioapp

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject

class AudioViewModel(application: Application) : AndroidViewModel(application) {

    private val _audioFiles = MutableLiveData<List<AudioFile>>()
    val audioFiles: LiveData<List<AudioFile>> get() = _audioFiles

    private val audioFileList = mutableListOf<AudioFile>()
    private val sharedPreferences = application.getSharedPreferences("audio_files", Context.MODE_PRIVATE)

    init {
        loadAudioFiles()
    }

    fun addAudioFile(audioFile: AudioFile) {
        audioFileList.add(audioFile)
        _audioFiles.value = audioFileList.toList() // Ensure LiveData is updated immediately
        saveAudioFiles()
    }

    private fun saveAudioFiles() {
        val editor = sharedPreferences.edit()
        val jsonArray = JSONArray()
        for (audioFile in audioFileList) {
            val jsonObject = JSONObject().apply {
                put("uri", audioFile.uri.toString())
                put("name", audioFile.name)
            }
            jsonArray.put(jsonObject)
        }
        editor.putString("audio_files_list", jsonArray.toString())
        editor.apply()
    }

    private fun loadAudioFiles() {
        val audioFilesString = sharedPreferences.getString("audio_files_list", null)
        audioFilesString?.let {
            val jsonArray = JSONArray(it)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val uri = Uri.parse(jsonObject.getString("uri"))
                val name = jsonObject.getString("name")
                audioFileList.add(AudioFile(uri, name))
            }
            _audioFiles.value = audioFileList
        }
    }
}
