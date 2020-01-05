package com.segregataur.supplier

class ImageFilesSupplier : BaseSupplier() {

    override fun getMediaSuffix(): String {
        return WHATSAPP_PATH_SUFFIX
    }

    companion object {
        private const val WHATSAPP_PATH_SUFFIX = "/WhatsApp/Media/WhatsApp Images"
    }
}