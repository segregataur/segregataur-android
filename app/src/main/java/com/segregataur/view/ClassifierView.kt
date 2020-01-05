package com.segregataur.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.segregataur.ClassifierViewPresenter
import com.segregataur.R
import com.segregataur.data.ClassifierStateModel
import com.segregataur.data.SegregationOptions
import com.segregataur.util.SharedPreferencesUtil
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.content_main.*


class ClassifierView(
        override val containerView: View,
        private val viewPresenter: ClassifierViewPresenter)
    : LayoutContainer {

    private val context = containerView.context
    private var isSegregationViewVisible: Boolean

    private val safeItemsAdapter = ItemAdapter<ClassifiedItem>()
    private val safeItemsFastAdapter =
            FastAdapter.with<ClassifiedItem, ItemAdapter<ClassifiedItem>>(safeItemsAdapter)
                    .withSelectable(true)
                    .withMultiSelect(true)
                    .withSelectWithItemUpdate(true)
                    .withSelectOnLongClick(false)
                    .withSelectionListener { item, selected ->
                        item?.let {
                            viewPresenter.userCheckOverride(it.path, selected)
                        }
                    }

    private val junkItemsAdapter = ItemAdapter<ClassifiedItem>()
    private val junkItemsFastAdapter =
            FastAdapter.with<ClassifiedItem, ItemAdapter<ClassifiedItem>>(junkItemsAdapter)
                    .withSelectable(true)
                    .withMultiSelect(true)
                    .withSelectWithItemUpdate(true)
                    .withSelectOnLongClick(false)
                    .withSelectionListener { item, selected ->
                        item?.let {
                            viewPresenter.userCheckOverride(it.path, selected)
                        }
                    }

    private val junkLayoutManager = LinearLayoutManager(
            context, RecyclerView.VERTICAL, true)

    private val safeLayoutManager = LinearLayoutManager(
            context, RecyclerView.VERTICAL, true)

    init {
        isSegregationViewVisible = false

        s_segregation_options.adapter =
                ArrayAdapter<String>(
                        context,
                        R.layout.layout_item_segregation_option,
                        SegregationOptions
                                .values().map {
                                    it.text
                                }
                )

        s_segregation_options.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewPresenter.setSegregationOption(position)
            }
        }

        b_cta.setOnClickListener { button ->
            button.visibility = View.INVISIBLE
            viewPresenter.startClassification()
        }

        ib_help.setOnClickListener {
            AlertDialog.Builder(context)
                    .setTitle(R.string.help_title)
                    .setMessage(R.string.help_text)
                    .setPositiveButton("Thank you") { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
        }

        ib_share_app.setOnClickListener {
            sendShareAppMessage()
        }

        rv_classification_safe.layoutManager = safeLayoutManager
        rv_classification_safe.adapter = safeItemsFastAdapter

        rv_classification_junk.layoutManager = junkLayoutManager
        rv_classification_junk.adapter = junkItemsFastAdapter
    }

    private fun sendShareAppMessage() {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                "This app can help you clear out your needless media : " +
                        "https://play.google.com/store/apps/details?id=com.segregataur")
        sendIntent.type = "text/plain"
        context.startActivity(sendIntent)
    }

    fun onChanged(itemArrays: Triple<Array<String>, Array<String>, Boolean>?) {
        if (itemArrays == null) return
        val (junkArray, safeArray, reformat) = itemArrays

        if (reformat) {
            junkItemsAdapter.clear()
            safeItemsAdapter.clear()
        }

        if (junkArray.isNotEmpty()) {
            junkItemsAdapter.add(
                    junkArray.map {
                        ClassifiedItem(it)
                                .withIdentifier(it.hashCode().toLong())
                                .withSelectable(true)
                                .withSetSelected(true)
                    })

            rv_classification_junk.post {
                val itemCount = junkItemsAdapter.adapterItemCount
                if (junkLayoutManager.findLastVisibleItemPosition()
                        in (itemCount - 3) until itemCount)
                    rv_classification_junk.smoothScrollToPosition(itemCount - 1)
            }
        }

        if (safeArray.isNotEmpty()) {
            safeItemsAdapter.add(
                    safeArray.map {
                        ClassifiedItem(it)
                                .withIdentifier(it.hashCode().toLong())
                                .withSelectable(true)
                                .withSetSelected(false)
                    })

            rv_classification_safe.post {
                val itemCount = safeItemsAdapter.adapterItemCount
                if (safeLayoutManager.findLastVisibleItemPosition()
                        in (itemCount - 3) until itemCount)
                    rv_classification_safe.smoothScrollToPosition(itemCount - 1)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun onChanged(classifierStateModel: ClassifierStateModel) {
        if (classifierStateModel.classificationStarted && !isSegregationViewVisible) {
            classificationStarted()
            pb_classifying.max = classifierStateModel.totalItemsToProcess
        }

        pb_classifying.progress = classifierStateModel.itemsProcessed
        tv_junk_size.text =
                "%.1f MB".format((classifierStateModel.junkSize.toDouble() / (1024 * 1024)))

        if (classifierStateModel.classificationComplete) {
            classificationCompleted()
        }

        classifierStateModel.displayMessage?.let { setMessageText(it) }
    }

    private fun showVerificationDialog() {
        val finalVerificationFastAdapter = FastAdapter.with<ClassifiedItem, ItemAdapter<ClassifiedItem>>(
                ItemAdapter<ClassifiedItem>().apply {
                    this.add(junkItemsFastAdapter.selectedItems.toList())
                    this.add(safeItemsFastAdapter.selectedItems.toList())
                })
                .withSelectable(true)
                .withMultiSelect(true)
                .withSelectWithItemUpdate(true)
                .withSelectOnLongClick(false)
                .withSelectionListener { item, selected ->
                    item?.let {
                        viewPresenter.userCheckOverride(it.path, selected)
                    }
                }
        val alertDialogBuilder = AlertDialog.Builder(context)
        if (finalVerificationFastAdapter.selectedItems.isEmpty()) {
            alertDialogBuilder.setMessage("No junk found")
                    .setPositiveButton("Ok") { dialog, _ ->
                        segregationFlowComplete(0, Double.NaN)
                        dialog.dismiss()
                    }
        } else {
            val view = LayoutInflater.from(context)
                    .inflate(
                            R.layout.layout_junk_files_dialog,
                            containerView as ViewGroup,
                            false)
            alertDialogBuilder.setView(view)
            val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_images_list)
            recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 3)
            recyclerView.adapter = finalVerificationFastAdapter
            alertDialogBuilder.setPositiveButton("Delete") { dialog, _ ->
                val pathsToDelete = finalVerificationFastAdapter.selectedItems.map { it.path }
                showDeleteSegregatedDialog(pathsToDelete, dialog)
            }
        }
        alertDialogBuilder
                .create()
                .show()
    }

    @SuppressLint("SetTextI18n")
    private fun showDeleteSegregatedDialog(
            pathsToDelete: List<String>,
            dialogParent: DialogInterface) {

        val pluralModifier = if (pathsToDelete.size > 1) "s" else ""
        AlertDialog.Builder(context)
                .setTitle(
                        "Are you sure you want to delete " +
                                "${pathsToDelete.size} " +
                                "file$pluralModifier ?")
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .setPositiveButton("Delete") { dialog, _ ->
                    val deletedBytes = viewPresenter.deleteJunkFiles(pathsToDelete)
                    segregationFlowComplete(pathsToDelete.size, deletedBytes)
                    dialog.dismiss()
                    dialogParent.dismiss()
                }
                .create()
                .show()
    }

    private fun classificationStarted() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(context, R.layout.content_main_post_start_constraint_set)
        TransitionManager.beginDelayedTransition(main_content)
        constraintSet.applyTo(main_content)

        val rotationAnimation = AnimationUtils.loadAnimation(context, R.anim.rotation_infinite)
        logo.startAnimation(rotationAnimation)
        pb_classifying.visibility = View.VISIBLE
        isSegregationViewVisible = true
    }

    private fun classificationCompleted() {
        rv_classification_safe.stopScroll()
        rv_classification_junk.stopScroll()

        logo.clearAnimation()
        b_verify.visibility = View.VISIBLE
        b_verify.setOnClickListener {
            showVerificationDialog()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun segregationFlowComplete(deletedFilesCount: Int, deletedBytes: Double) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(context, R.layout.content_main)
        TransitionManager.beginDelayedTransition(main_content)
        constraintSet.applyTo(main_content)


        tv_onboarding.text = "Nice Work!"
        tv_options_label.text =
                if (deletedFilesCount > 0)
                    "You deleted $deletedFilesCount files and\nsaved %.1f mb"
                            .format(deletedBytes / (1024 * 1024))
                else
                    "All Cleaned up!"

        tv_onboarding_hint.text = "If you found this app useful, spread some \uD83D\uDC93\nShare this app with others\nand get us ⭐⭐⭐⭐⭐"

        b_verify.visibility = View.GONE
        s_segregation_options.visibility = View.GONE

        if (SharedPreferencesUtil.getShowAskForRating(context)) {
            b_cta.text = "Rate us"
            b_cta.setOnClickListener {
                context.startActivity(
                        Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.segregataur")))
                SharedPreferencesUtil
                        .setShowAskForRatingFalse(context)
            }
        } else {
            b_cta.text = "Share"
            b_cta.setOnClickListener {
                sendShareAppMessage()
            }
        }
    }

    private fun setMessageText(text: String) {
        if (tv_message_display.text == text) return
        tv_message_display.text = text
    }
}