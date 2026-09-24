package com.bahoque.gastonomo.core.domain.health

/**
 * Version semantica del servicio. El dominio rechaza cualquier cadena que no sea semver.
 */
@JvmInline
value class ServiceVersion(val value: String) {

    init {
        require(SEMVER.matches(value)) { "Not a semantic version: '$value'" }
    }

    override fun toString(): String = value

    companion object {
        private val SEMVER = Regex("""^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?$""")

        /** Crea la version o devuelve null si la cadena no respeta semver. */
        fun parseOrNull(raw: String): ServiceVersion? =
            if (SEMVER.matches(raw)) ServiceVersion(raw) else null
    }
}
