package com.bahoque.gastonomo.core.designsystem.icon

/**
 * Los 65 iconos que usa el diseno, por su nombre en Material Symbols. Son los unicos que
 * lleva la fuente recortada: pedir uno que no este en esta lista pinta un rectangulo
 * vacio, asi que se anaden aqui y se vuelve a recortar la fuente.
 *
 * El recorte se hace pidiendoselo a Google Fonts con `icon_names`, con esta lista y con
 * FILL 0 y FILL 1; esta explicado en el README del modulo.
 */
object GastonomoIcons {
    const val ACCOUNT_BALANCE_WALLET = "account_balance_wallet"
    const val ADD = "add"
    const val ADD_CARD = "add_card"
    const val ARROW_BACK = "arrow_back"
    const val ARROW_DOWNWARD = "arrow_downward"
    const val ARROW_OUTWARD = "arrow_outward"
    const val ARROW_UPWARD = "arrow_upward"
    const val BACKSPACE = "backspace"
    const val BOLT = "bolt"
    const val BUILD = "build"
    const val CARD_GIFTCARD = "card_giftcard"
    const val CHECK = "check"
    const val CHECK_CIRCLE = "check_circle"
    const val CHECKROOM = "checkroom"
    const val CHILD_CARE = "child_care"
    const val CHEVRON_LEFT = "chevron_left"
    const val CHEVRON_RIGHT = "chevron_right"
    const val CLOSE = "close"
    const val CLOUD_DONE = "cloud_done"
    const val CLOUD_UPLOAD = "cloud_upload"
    const val CURRENCY_EXCHANGE = "currency_exchange"
    const val DATA_OBJECT = "data_object"
    const val DATE_RANGE = "date_range"
    const val DELETE = "delete"
    const val DESCRIPTION = "description"
    const val DIRECTIONS_BUS = "directions_bus"
    const val DOWNLOAD = "download"
    const val EDIT = "edit"
    const val EXPAND_MORE = "expand_more"
    const val FINGERPRINT = "fingerprint"
    const val FITNESS_CENTER = "fitness_center"
    const val FLIGHT = "flight"
    const val HANDSHAKE = "handshake"
    const val HOME = "home"
    const val INFO = "info"
    const val LANGUAGE = "language"
    const val LAPTOP_MAC = "laptop_mac"
    const val LOCAL_BAR = "local_bar"
    const val LOCAL_CAFE = "local_cafe"
    const val LOCAL_GAS_STATION = "local_gas_station"
    const val LOCK = "lock"
    const val LOGOUT = "logout"
    const val MARK_EMAIL_READ = "mark_email_read"
    const val MEDICAL_SERVICES = "medical_services"
    const val MIC = "mic"
    const val MIC_OFF = "mic_off"
    const val MORE_HORIZ = "more_horiz"
    const val MOVIE = "movie"
    const val NORTH_EAST = "north_east"
    const val PAYMENTS = "payments"
    const val PETS = "pets"
    const val RECEIPT_LONG = "receipt_long"
    const val RESTAURANT = "restaurant"
    const val SAVINGS = "savings"
    const val SCHOOL = "school"
    const val SEARCH = "search"
    const val SELL = "sell"
    const val SETTINGS = "settings"
    const val SHOPPING_CART = "shopping_cart"
    const val SOUTH_WEST = "south_west"
    const val SPA = "spa"
    const val SPORTS_ESPORTS = "sports_esports"
    const val SUBSCRIPTIONS = "subscriptions"
    const val SWAP_VERT = "swap_vert"
    const val TABLE_VIEW = "table_view"
    const val UNARCHIVE = "unarchive"
    const val VISIBILITY = "visibility"
    const val VISIBILITY_OFF = "visibility_off"
    const val VOLUNTEER_ACTIVISM = "volunteer_activism"

    /** Todos, para el test que comprueba que la fuente los tiene. */
    val ALL: List<String> = listOf(
        ACCOUNT_BALANCE_WALLET, ADD, ADD_CARD, ARROW_BACK, ARROW_DOWNWARD, ARROW_OUTWARD,
        ARROW_UPWARD, BACKSPACE, BOLT, BUILD, CARD_GIFTCARD, CHECK, CHECK_CIRCLE, CHECKROOM,
        CHILD_CARE, CHEVRON_LEFT, CHEVRON_RIGHT, CLOSE, CLOUD_DONE, CURRENCY_EXCHANGE,
        CLOUD_UPLOAD, DATA_OBJECT, DATE_RANGE, DELETE,
        DESCRIPTION, DIRECTIONS_BUS, DOWNLOAD, EDIT, EXPAND_MORE, FINGERPRINT, FITNESS_CENTER,
        FLIGHT, HANDSHAKE, HOME, INFO, LANGUAGE, LAPTOP_MAC, LOCAL_BAR, LOCAL_CAFE,
        LOCAL_GAS_STATION,
        LOCK, LOGOUT, MARK_EMAIL_READ, MEDICAL_SERVICES, MIC, MIC_OFF, MORE_HORIZ, MOVIE,
        NORTH_EAST,
        PAYMENTS, PETS, RECEIPT_LONG, RESTAURANT, SAVINGS, SCHOOL, SEARCH, SELL, SETTINGS,
        SHOPPING_CART, SOUTH_WEST, SPA, SPORTS_ESPORTS, SUBSCRIPTIONS, SWAP_VERT, TABLE_VIEW,
        UNARCHIVE,
        VISIBILITY, VISIBILITY_OFF, VOLUNTEER_ACTIVISM,
    )
}
