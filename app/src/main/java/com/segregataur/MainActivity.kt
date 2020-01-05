package com.segregataur

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.segregataur.core.JunkImageClassifier
import com.segregataur.db.DatabaseManager
import com.segregataur.view.ClassifierView
import com.segregataur.viewmodel.ClassifierViewModel
import com.segregataur.viewmodel.ClassifierViewModelFactory
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var classifierViewModel: ClassifierViewModel
    private lateinit var classifierView: ClassifierView

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        classifierViewModel = ViewModelProviders.of(this,
                ClassifierViewModelFactory(
                        JunkImageClassifier(assets),
                        DatabaseManager.getAppDatabaseInstance(applicationContext).classificationDAO()))
                .get(ClassifierViewModel::class.java)


        classifierView = ClassifierView(main_content, object : ClassifierViewPresenter {

            override fun setSegregationOption(position: Int) {
                classifierViewModel.setSegregationOption(position)
            }

            override fun startClassification() {
                if (isReadStoragePermissionGranted())
                    runClassification()
            }

            override fun userCheckOverride(path: String, selected: Boolean) {
                classifierViewModel.userClassification(path, selected)
            }

            override fun deleteJunkFiles(pathsToDelete: List<String>): Double {
                return classifierViewModel.deleteFiles(applicationContext, pathsToDelete)
            }
        })

        classifierViewModel.classifierStateModel.observe(this, Observer {
            classifierView.onChanged(it)
        })
    }

    private fun runClassification() {
        classifierViewModel.runBatchImageClassification(applicationContext).observe(this@MainActivity, Observer {
            classifierView.onChanged(it)
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_WRITE_PERMISSION_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runClassification()
                }
            }
        }
    }

    private fun isReadStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), READ_WRITE_PERMISSION_REQUEST_CODE)
                false
            }
        } else {
            true
        }
    }

    companion object {
        const val READ_WRITE_PERMISSION_REQUEST_CODE = 442
    }
}
