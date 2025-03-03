package com.dss.emulator.dsscommand

data class DSSCommand(
    var source: String = "",
    var destination: String = "",
    var command: String = "",
    var data: List<String> = emptyList(),
    var checksum: String = ""
) : Cloneable {

    // Companion Object for Static Members
    companion object {
        // Maximum constraints as constants
        private const val MAX_DATA_POINTS = 3
        private const val MAX_ID_WIDTH = 4
        private const val MAX_DATA_FIELD_WIDTH = 100
        private const val COMMAND_WIDTH = 2
        private const val MAX_CHECKSUM_WIDTH = 9

        // Regex Components
        private const val REGEX_START_PART = "\\$"
        private const val REGEX_DESTINATION_PART = "(?<Destination>((\\d{1,$MAX_ID_WIDTH})|B))"
        private const val REGEX_SOURCE_PART = "(?<Source>(\\d{1,$MAX_ID_WIDTH}))"
        private const val REGEX_COMMAND_PART = "(?<Command>([A-Z]{$COMMAND_WIDTH}))"
        private const val REGEX_DATA_PART =
            """(?:,(?<Data>(?:[^*,]{0,$MAX_DATA_FIELD_WIDTH})(?:,|)){0,$MAX_DATA_POINTS})?"""
        private const val REGEX_CHECKSUM_PART = """(?:,\*(?<Checksum>\d{1,$MAX_CHECKSUM_WIDTH}))?"""
        private const val REGEX_END_PART = "[\r][\n]"

        // Compiled Regex Patterns
        private val COMMAND_REGEX = Regex(
            "$REGEX_START_PART$REGEX_DESTINATION_PART,$REGEX_SOURCE_PART,$REGEX_COMMAND_PART$REGEX_DATA_PART$REGEX_CHECKSUM_PART$REGEX_END_PART"
        )
        private val COMMAND_REGEX_NO_END = Regex(
            "$REGEX_START_PART$REGEX_DESTINATION_PART,$REGEX_SOURCE_PART,$REGEX_COMMAND_PART$REGEX_DATA_PART$REGEX_CHECKSUM_PART"
        )

        // Keys for Special Constructors
        private const val DESTINATION_KEY = "Destination"
        private const val SOURCE_KEY = "Source"
        private const val COMMAND_KEY = "Command"
        private const val CHECKSUM_KEY = "Checksum"
        private const val DATA_FIELD_KEY_PREFIX = "Data_"

        fun createSetFieldCommand(
            source: String,
            destination: String,
            field: String,
            vararg data: String
        ): DSSCommand {
            val args = listOf(field) + data
            return DSSCommand(
                source = source,
                destination = destination,
                command = StandardRequest.ST.name,
                data = args
            )
        }

        fun createSetProtectedFieldCommand(
            source: String,
            destination: String,
            field: String,
            password: String,
            vararg data: String
        ): DSSCommand {
            val args = listOf(password, field) + data
            return DSSCommand(
                source = source,
                destination = destination,
                command = StandardRequest.SP.name,
                data = args
            )
        }

        fun createSetProtectedFieldCommand(
            source: String,
            destination: String,
            field: StandardDataField,
            password: String,
            vararg data: String
        ): DSSCommand {
            return createSetProtectedFieldCommand(
                source,
                destination,
                field.name,
                password,
                *data
            )
        }

        fun createGetFieldCommand(
            source: String,
            destination: String,
            field: StandardDataField
        ): DSSCommand {
            return createGetFieldCommand(source, destination, field.name)
        }

        fun createGetFieldCommand(
            source: String,
            destination: String,
            field: String,
            vararg data: String = emptyArray()
        ): DSSCommand {
            val args = listOf(field) + data
            return DSSCommand(
                source = source,
                destination = destination,
                command = StandardRequest.GT.name,
                data = args
            )
        }

        fun createBroadcastGetIDCommand(source: String): DSSCommand {
            return DSSCommand(
                source = source,
                destination = "B",
                command = StandardRequest.GI.name
            )
        }

        fun createRebootCommand(source: String, destination: String): DSSCommand {
            return DSSCommand(
                source = source,
                destination = destination,
                command = "RB"
            )
        }

        fun createOKResponse(source: String, destination: String): DSSCommand {
            return DSSCommand(
                source = source,
                destination = destination,
                command = StandardResponse.OK.name
            )
        }

        fun createRTResponse(
            source: String,
            destination: String,
            registerName: String,
            registerVaule: String
        ): DSSCommand {
            return DSSCommand(
                source = source,
                destination = destination,
                command = StandardResponse.RT.name,
                data = arrayListOf(registerName, registerVaule)
            )
        }

        fun createIDResponse(
            source: String,
            destination: String,
            serialNumber: String
        ): DSSCommand {
            return DSSCommand(
                source = source,
                destination = destination,
                command = StandardResponse.ID.name,
                data = listOf(serialNumber)
            )
        }

        fun createNOResponse(source: String, destination: String): DSSCommand {
            return DSSCommand(
                source = source,
                destination = destination,
                command = StandardResponse.NO.name
            )
        }
    }

    // Computed Checksum using CRC16
    private val computedChecksum: String
        get() = CRC16.compute(buildString {
            append("$\${destination},\${source},\${command}")
            data.forEach { datum ->
                append(",$datum")
            }
            append(",")
        }).toString()

    // Command Text with End
    val commandText: String
        get() = buildString {
            append("$\${destination},\${source},\${command}")
            if (data.isNotEmpty()) {
                append(",${data.joinToString(",")}")
            }
            append(",*\${checksum}\r\n")
        }

    // Command Text without End
    val commandTextNoEnd: String
        get() = buildString {
            append("$\${destination},\${source},\${command}")
            if (data.isNotEmpty()) {
                append(",${data.joinToString(",")}")
            }
            append(",*\${checksum}")
        }

    // Checksum Validation
    val isChecksumValid: Boolean
        get() = checksum == computedChecksum

    // Secondary Constructor from Map
    constructor(result: Map<String, String>) : this(
        source = result[SOURCE_KEY] ?: "",
        destination = result[DESTINATION_KEY] ?: "",
        command = result[COMMAND_KEY] ?: "",
        data = result.keys
            .filter { it.startsWith(DATA_FIELD_KEY_PREFIX) }
            .mapNotNull { key ->
                key.removePrefix(DATA_FIELD_KEY_PREFIX).toIntOrNull()?.let { index ->
                    result["$DATA_FIELD_KEY_PREFIX$index"]
                }
            }
            .sortedBy { it } // Ensure data is sorted if necessary
            .filter { it.isNotEmpty() },
        checksum = result[CHECKSUM_KEY] ?: ""
    )

    // Secondary Constructor from Sentence String
    constructor(sentence: String) : this() {
        val match = COMMAND_REGEX.matchEntire(sentence) ?: COMMAND_REGEX_NO_END.find(sentence)
        ?: throw IllegalArgumentException("Invalid Sentence")
        source = match.groups["Source"]?.value ?: ""
        destination = match.groups["Destination"]?.value ?: ""
        command = match.groups["Command"]?.value ?: ""
        checksum = match.groups["Checksum"]?.value ?: computedChecksum

        val dataGroup = match.groups["Data"]?.value ?: ""
        data = if (dataGroup.isNotEmpty()) {
            dataGroup.split(",").filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        if (checksum.isEmpty()) {
            checksum = computedChecksum
        }
    }
}
