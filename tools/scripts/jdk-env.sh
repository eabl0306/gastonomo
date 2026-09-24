# Deja JAVA_HOME y PATH listos para Gradle. Se usa con `source`, no se ejecuta.
#
#   source tools/scripts/jdk-env.sh
#
# En muchas maquinas de desarrollo el unico JDK es el que trae Android Studio y
# no esta en el PATH. Aqui se busca uno antes de rendirse.

if ! command -v java >/dev/null 2>&1; then
  for candidate in \
    "${JAVA_HOME:-}" \
    /snap/android-studio/*/jbr \
    /opt/android-studio/jbr \
    /usr/lib/jvm/*/ \
    "${HOME}/.jdks"/* \
    "${HOME}/.sdkman/candidates/java/current"; do
    if [ -x "${candidate%/}/bin/java" ]; then
      JAVA_HOME="${candidate%/}"
      export JAVA_HOME
      break
    fi
  done
fi

if [ -n "${JAVA_HOME:-}" ]; then
  PATH="${JAVA_HOME}/bin:${PATH}"
  export PATH
fi

if ! command -v java >/dev/null 2>&1; then
  echo "No hay ningun JDK a la vista. Exporta JAVA_HOME e intentalo de nuevo." >&2
  echo "Android necesita JDK 21 o superior (aqui se uso el 25)." >&2
  return 1 2>/dev/null || exit 1
fi
