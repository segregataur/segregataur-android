package com.segregataur

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.layout_enlarged_image_view.*
import kotlinx.android.synthetic.main.layout_enlarged_video_view.*
import java.io.File

class ThumbnailZoomDialogFragment : AppCompatDialogFragment() {

    private lateinit var mediaPath: String
    private var isVideo: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPath = arguments!!.getString(IMAGE_PATH_ARGUMENT) ?: ""
        isVideo = mediaPath.endsWith(".mp4")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return if (isVideo)
            inflater.inflate(R.layout.layout_enlarged_video_view, container)
        else
            inflater.inflate(R.layout.layout_enlarged_image_view, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isVideo) {
            vv_enlarged_video.setZOrderOnTop(true)
            vv_enlarged_video.setVideoPath(Uri.fromFile(File(mediaPath)).toString())
            vv_enlarged_video.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.start()
            }
        } else {
            Glide.with(iv_enlarged_image.context)
                    .load(mediaPath)
                    .into(iv_enlarged_image)

            iv_enlarged_image.setOnLongClickListener {
                dismissAllowingStateLoss()
                true
            }
        }
    }

    companion object {
        private const val IMAGE_PATH_ARGUMENT = "image_path_argument"

        fun newInstance(path: String): ThumbnailZoomDialogFragment {
            val fragment = ThumbnailZoomDialogFragment()
            val bundle = Bundle()
            bundle.putString(IMAGE_PATH_ARGUMENT, path)
            fragment.arguments = bundle
            return fragment
        }
    }
}