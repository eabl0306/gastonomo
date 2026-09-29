-- Quien tiene el dictado disponible.
--
-- El dictado no es una funcion mas del producto: cada peticion sale a un modelo de
-- lenguaje que se paga por palabra, y quien la paga es el dueno de la instancia. Si
-- estuviera abierto a cualquiera que se registre, la factura la decidiria un
-- desconocido. Por eso se concede cuenta a cuenta.
--
-- Es una tabla aparte y no una columna en `users` a proposito. La aprobacion es una
-- decision del dueno de la instancia, no un atributo de la persona: no la elige el
-- usuario, no la ve en su perfil y no viaja en `/v1/me`. Asi conceder o quitar el
-- acceso es un INSERT o un DELETE por consola, y no una migracion del agregado
-- `User` mas un campo que arrastrar por el dominio, la API y los tres clientes. El
-- precedente es `user_change_counters`, en la migracion 000800: lo que es DE un
-- usuario pero no es SUYO vive en su propia tabla.
--
-- Estar en la lista es toda la informacion que hay. No hay columna de estado ni de
-- revocado: quitar el acceso es borrar la fila. Una bandera se puede leer al reves
-- en una consulta y dejar el dictado abierto a todo el mundo; una fila que no esta
-- no se lee al reves.
--
-- La fila se va con la cuenta. Una aprobacion sin cuenta a la que apuntar no es
-- nada, y esta tabla no es historico: aqui si se borra.

CREATE TABLE IF NOT EXISTS dictation_allowlist (
    user_id    uuid        PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    granted_at timestamptz NOT NULL DEFAULT now()
);

COMMENT ON TABLE dictation_allowlist IS
    'Cuentas con el dictado aprobado. Estar en la tabla ES la aprobacion, y quitarla '
    'es borrar la fila. Las pone y las quita a mano el dueno de la instancia.';

COMMENT ON COLUMN dictation_allowlist.user_id IS
    'La cuenta aprobada. Es la clave primaria, asi que la comprobacion ya entra por '
    'indice y la lista no puede tener duplicados.';

COMMENT ON COLUMN dictation_allowlist.granted_at IS
    'Cuando se concedio. El codigo no lo lee. Esta para quien mire la tabla a mano: '
    'las filas las escribe una persona y no queda rastro en ningun otro sitio.';
