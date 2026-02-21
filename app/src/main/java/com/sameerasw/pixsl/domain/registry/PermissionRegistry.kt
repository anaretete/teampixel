package com.sameerasw.pixsl.domain.registry

import com.sameerasw.pixsl.R

object PermissionRegistry {
    private val registry = mutableMapOf<String, MutableList<Int>>()

    fun register(permissionKey: String, featureTitleRes: Int) {
        val list = registry.getOrPut(permissionKey) { mutableListOf() }
        if (!list.contains(featureTitleRes)) list.add(featureTitleRes)
    }

    fun getFeatures(permissionKey: String): List<Int> =
        registry[permissionKey]?.toList() ?: emptyList()
}

fun initPermissionRegistry() {
    PermissionRegistry.register("MEDIA", R.string.feat_media_upload_title)
}
