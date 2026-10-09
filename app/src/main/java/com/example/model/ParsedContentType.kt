package com.example.model

enum class ParsedContentType(val label: String) {
    URL("URL / Web"),
    TEXT("Plain Text"),
    WIFI("Wi-Fi Network"),
    CONTACT("Contact (vCard)"),
    PHONE("Phone Number"),
    EMAIL("Email"),
    SMS("SMS Message"),
    LOCATION("Geo Location"),
    CALENDAR("Calendar Event"),
    PRODUCT("Product Barcode"),
    UNKNOWN("Raw Data")
}
