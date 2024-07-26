package com.example.audioapp

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.MediaItem

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: AudioViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AudioAdapter
    private lateinit var currentlyPlayingTextView: TextView
    private lateinit var playerView: PlayerView
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(AudioViewModel::class.java)
        recyclerView = findViewById(R.id.recycler_view)
        adapter = AudioAdapter { audioFile -> playAudio(audioFile) }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        currentlyPlayingTextView = findViewById(R.id.currently_playing)
        playerView = findViewById(R.id.player_view)

        // Set controller to be always visible
        playerView.controllerShowTimeoutMs = 0

        findViewById<Button>(R.id.btn_open_files).setOnClickListener {
            openFilePicker()
        }

        viewModel.audioFiles.observe(this, { audioFiles ->
            adapter.submitList(audioFiles)
        })

        initializePlayer()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, PICK_AUDIO_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_REQUEST_CODE && resultCode == RESULT_OK) {
            val clipData = data?.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    val audioFile = getAudioFileFromUri(uri)
                    audioFile?.let {
                        viewModel.addAudioFile(it)
                    }
                }
            } else {
                data?.data?.also { uri ->
                    val audioFile = getAudioFileFromUri(uri)
                    audioFile?.let {
                        viewModel.addAudioFile(it)
                    }
                }
            }
        }
    }

    @SuppressLint("Range")
    private fun getAudioFileFromUri(uri: Uri): AudioFile? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val name = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                return AudioFile(uri, name)
            }
        }
        return null
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        playerView.player = exoPlayer
    }

    private fun playAudio(audioFile: AudioFile) {
        val mediaItem = MediaItem.fromUri(audioFile.uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()
        currentlyPlayingTextView.text = "Currently Playing: ${audioFile.name}"
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        private const val PICK_AUDIO_REQUEST_CODE = 1
    }
}
