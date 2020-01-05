package com.segregataur.view

import android.view.View
import com.mikepenz.fastadapter.items.AbstractItem
import com.segregataur.R

class ClassifiedItem(val path: String) : AbstractItem<ClassifiedItem, ClassifiedItemViewHolder>() {

    override fun getType(): Int = R.id.adapter_item_image

    override fun getViewHolder(v: View): ClassifiedItemViewHolder = ClassifiedItemViewHolder(v)

    override fun getLayoutRes(): Int = R.layout.layout_image_view
}
