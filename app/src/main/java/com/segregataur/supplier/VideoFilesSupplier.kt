package com.segregataur.supplier

class VideoFilesSupplier : BaseSupplier() {

    override fun getMediaSuffix(): String {
        return WHATSAPP_VIDEO_SUFFIX
    }

    companion object {
        private const val WHATSAPP_VIDEO_SUFFIX = "/WhatsApp/Media/WhatsApp Video"
    }
}