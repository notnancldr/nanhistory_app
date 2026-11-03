package id.my.nanclouder.nanhistory.utils.graph

/**
 * Built-in color palettes and graph templates.
 */
object DefaultGraphPresets {

    // --- Color Presets ---
    val COLOR_PRESET_SPEED = ColorPreset(
        id = "preset_speed",
        name = "Speed",
        category = ColorPresetCategory.PRESET,
        stops = mapOf(
            0f to 0xFF800000,     // Dark Red
            6f to 0xFF992200,     // Dark Orange-Red
            10f to 0xFFCC5500,    // Bright Orange
            29.6f to 0xFF88BB00,  // Lime Green / Yellow
            49.2f to 0xFF00AA33,  // Bright Green
            68.8f to 0xFF009999,  // Cyan / Teal
            88.4f to 0xFF0055CC,  // Bright Blue
            108f to 0xFF7700BB    // Bright Purple
        )
    )

    val COLOR_PRESET_ACCELERATION = ColorPreset(
        id = "preset_acceleration",
        name = "Acceleration",
        category = ColorPresetCategory.PRESET,
        stops = mapOf(
            -3f to 0xFFDD2C00,   // Strong Deceleration (Red)
            0f to 0xFFFFD600,    // Steady (Yellow)
            3f to 0xFF00C853     // Strong Acceleration (Green)
        )
    )

    val COLOR_PRESET_ALTITUDE = ColorPreset(
        id = "preset_altitude",
        name = "Altitude",
        category = ColorPresetCategory.PRESET,
        stops = mapOf(
            0f to 0xFF00695C,     // Deep Teal (Sea level)
            250f to 0xFF2E7D32,   // Forest Green
            500f to 0xFFF57F17,   // Hill Yellow/Amber
            750f to 0xFF6D4C41,   // Mountain Brown
            1000f to 0xFFECEFF1   // Snow White
        )
    )

    val QUICK_GREEN_BLUE = ColorPreset(
        id = "quick_green_blue",
        name = "Green-Blue",
        category = ColorPresetCategory.QUICK,
        stops = mapOf(
            0f to 0xFF00B0FF,     // Blue
            100f to 0xFF00E676    // Vibrant Green
        )
    )

    val QUICK_RED_YELLOW = ColorPreset(
        id = "quick_red_yellow",
        name = "Red-Yellow",
        category = ColorPresetCategory.QUICK,
        stops = mapOf(
            0f to 0xFFFF1744,     // Red
            100f to 0xFFFFEA00    // Yellow
        )
    )

    val QUICK_GRAYSCALE = ColorPreset(
        id = "quick_grayscale",
        name = "Grayscale",
        category = ColorPresetCategory.QUICK,
        stops = mapOf(
            0f to 0xFF37474F,     // Dark Slate Gray
            100f to 0xFFCFD8DC    // Light Silver
        )
    )

    val DEFAULT_COLOR_PRESETS: List<ColorPreset> = listOf(
        COLOR_PRESET_SPEED,
        COLOR_PRESET_ACCELERATION,
        COLOR_PRESET_ALTITUDE,
        QUICK_GREEN_BLUE,
        QUICK_RED_YELLOW,
        QUICK_GRAYSCALE
    )

    // --- Built-in Graph Templates ---
    fun createSpeedGraph(): GraphConfig = GraphConfig(
        title = "Speed Overview",
        series = listOf(
            SeriesConfig(
                name = "Speed",
                dataSource = SeriesDataSource.Standard(StandardProperty.SPEED),
                colorPresetId = COLOR_PRESET_SPEED.id
            )
        ),
        scaleMode = GraphScaleMode.NORMALIZED
    )

    fun createAltitudeGraph(): GraphConfig = GraphConfig(
        title = "Altitude Profile",
        series = listOf(
            SeriesConfig(
                name = "Altitude",
                dataSource = SeriesDataSource.Standard(StandardProperty.ALTITUDE),
                colorPresetId = COLOR_PRESET_ALTITUDE.id
            )
        ),
        scaleMode = GraphScaleMode.NORMALIZED
    )

    fun createSpeedAndAccelGraph(): GraphConfig = GraphConfig(
        title = "Speed & Acceleration",
        series = listOf(
            SeriesConfig(
                name = "Speed",
                dataSource = SeriesDataSource.Standard(StandardProperty.SPEED),
                colorPresetId = COLOR_PRESET_SPEED.id
            ),
            SeriesConfig(
                name = "Acceleration",
                dataSource = SeriesDataSource.Standard(StandardProperty.ACCELERATION),
                colorPresetId = COLOR_PRESET_ACCELERATION.id
            )
        ),
        scaleMode = GraphScaleMode.DUAL_AXIS
    )

    fun createFullTelemetryGraph(): GraphConfig = GraphConfig(
        title = "Full Telemetry",
        series = listOf(
            SeriesConfig(
                name = "Speed",
                dataSource = SeriesDataSource.Standard(StandardProperty.SPEED),
                colorPresetId = COLOR_PRESET_SPEED.id
            ),
            SeriesConfig(
                name = "Altitude",
                dataSource = SeriesDataSource.Standard(StandardProperty.ALTITUDE),
                colorPresetId = COLOR_PRESET_ALTITUDE.id
            ),
            SeriesConfig(
                name = "Acceleration",
                dataSource = SeriesDataSource.Standard(StandardProperty.ACCELERATION),
                colorPresetId = COLOR_PRESET_ACCELERATION.id
            )
        ),
        scaleMode = GraphScaleMode.NORMALIZED
    )
}
