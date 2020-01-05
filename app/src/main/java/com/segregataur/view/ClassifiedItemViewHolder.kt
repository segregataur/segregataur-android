package com.segregataur.view

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.mikepenz.fastadapter.FastAdapter
import com.segregataur.ThumbnailZoomDialogFragment
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.layout_image_view.*

class ClassifiedItemViewHolder(override val containerView: View) : FastAdapter.ViewHolder<ClassifiedItem>(containerView), LayoutContainer {

    override fun bindView(item: ClassifiedItem, payloads: MutableList<Any>?) {
        iv_video_indicator.visibility =
                if (item.path.endsWith(".mp4"))
                    View.VISIBLE
                else
                    View.GONE
        Glide.with(iv_image)
                .load(item.path)
                .thumbnail(.25f)
                .into(iv_image)
        if (!item.isSelectable) cb_delete_selection.visibility = View.GONE
        else {
            cb_delete_selection.visibility = if (item.isSelected)
                View.VISIBLE
            else
                View.GONE
        }
        containerView.setOnLongClickListener {
            val fragment = ThumbnailZoomDialogFragment.newInstance(item.path)
            fragment.show((containerView.context as AppCompatActivity).supportFragmentManager, null)
            true
        }
    }

    override fun unbindView(item: ClassifiedItem?) {}
}
