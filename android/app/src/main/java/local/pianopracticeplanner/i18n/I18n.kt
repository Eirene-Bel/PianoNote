package local.pianopracticeplanner.i18n

/** Offline presentation catalog. Storage, backup keys and canonical note tokens are never translated. */
object I18n {
    @Volatile var language: String = "ja"
    val languages = linkedMapOf("" to "", "ja" to "日本語", "en" to "English", "zh-Hans" to "简体中文", "zh-Hant" to "繁體中文", "ko" to "한국어", "es" to "Español", "fr" to "Français", "de" to "Deutsch", "pt" to "Português")
    fun resolve(tag: String): String {
        val locale = java.util.Locale.forLanguageTag(tag)
        return when (locale.language) {
            "zh" -> if (locale.script == "Hant" || (locale.script.isEmpty() && locale.country in listOf("TW", "HK", "MO"))) "zh-Hant" else "zh-Hans"
            "ja", "en", "ko", "es", "fr", "de", "pt" -> locale.language
            else -> "en"
        }
    }
    fun note(token: String): String {
        val index = listOf("ド", "レ", "ミ", "ファ", "ソ", "ラ", "シ").indexOf(token)
        if (index < 0) return token
        return when (language) {
            "ja" -> token
            "ko" -> listOf("도", "레", "미", "파", "솔", "라", "시")[index]
            "de" -> listOf("C", "D", "E", "F", "G", "A", "H")[index]
            "en" -> listOf("C", "D", "E", "F", "G", "A", "B")[index]
            "fr" -> listOf("Do", "Ré", "Mi", "Fa", "Sol", "La", "Si")[index]
            "pt" -> listOf("Dó", "Ré", "Mi", "Fá", "Sol", "Lá", "Si")[index]
            else -> listOf("Do", "Re", "Mi", "Fa", "Sol", "La", "Si")[index]
        }
    }
}

fun tr(key: String, vararg arguments: Any?): String {
    val template = Catalogs.get(I18n.language)[key] ?: error("Missing translation: $key")
    // Replace only placeholders in the template, never placeholders inside user-entered text.
    val value = Regex("\\{(\\d+)\\}").replace(template) { match -> arguments[match.groupValues[1].toInt()].toString() }
    return Messages.remember(String(value.toCharArray()), key, arguments)
}

/** Remembers provenance by object identity, so a user string matching a label is never translated.
 * Weak keys release transient messages, including the continuously updating timer text. */
private object Messages {
    private class Ref(value: String, queue: java.lang.ref.ReferenceQueue<String>? = null) : java.lang.ref.WeakReference<String>(value, queue) {
        private val identity = System.identityHashCode(value)
        override fun hashCode() = identity
        override fun equals(other: Any?): Boolean = this === other || (other is Ref && get() != null && get() === other.get())
    }
    private data class Recipe(val key: String, val arguments: List<Any?>) {
        fun render(): String = tr(key, *arguments.map { if (it is Recipe) it.render() else it }.toTypedArray())
    }
    private val queue = java.lang.ref.ReferenceQueue<String>()
    private val recipes = mutableMapOf<Ref, Recipe>()
    @Synchronized fun remember(value: String, key: String, arguments: Array<out Any?>): String {
        while (true) { val expired = queue.poll() ?: break; recipes.remove(expired as Ref) }
        recipes[Ref(value, queue)] = Recipe(key, arguments.map { if (it is String) recipes[Ref(it)] ?: it else it })
        return value
    }
    @Synchronized fun refresh(value: String): String = recipes[Ref(value)]?.render() ?: value
}

fun refreshTranslation(value: String): String = Messages.refresh(value)
