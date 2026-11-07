package id.my.nanclouder.nanhistory.core.common.utils

inline fun <reified T> matchOrNull(value: Any?): T? = if (value is T) value else null
