package com.cawfee.bluetooth.protocol

/**
 * GATT service / characteristic map and protocol timing constants for the Jura Smart
 * Connect ("BlueFrog") dongle (§4, §5, §7, §20 of the protocol spec).
 *
 * Handles are intentionally NOT hard-coded — always discover characteristics by UUID,
 * as the spec warns handles are not stable across firmware revisions.
 */
object JuraGatt {

    /** BLE company identifier under which the key + model id are advertised (§4). */
    const val COMPANY_ID = 0x00AB

    /** Advertised device name of the Smart Connect dongle. */
    const val ADVERTISED_NAME = "TT214H BlueFrog"

    /** Article number / model id advertised by a Jura E8 (advert bytes 4–5, LE16). */
    const val MODEL_ID_E8 = 15057 // 0x3AD1

    private const val SUFFIX = "-ab2e-2548-c435-08c300000710"

    /** Default / control service. */
    const val SERVICE_CONTROL = "5a401523$SUFFIX"

    /** UART pass-through service. */
    const val SERVICE_UART = "5a401623$SUFFIX"

    // --- Characteristics (control service) ---
    /** Plaintext device identity / version. Read. */
    const val CHAR_ABOUT_MACHINE = "5a401531$SUFFIX"

    /** Machine status / alert bitfield. Read. Obfuscated. */
    const val CHAR_MACHINE_STATUS = "5a401524$SUFFIX"

    /** Start product (18-byte frame). Write. Obfuscated. */
    const val CHAR_START_PRODUCT = "5a401525$SUFFIX"

    /** Brew progress. Read/Notify. Obfuscated. */
    const val CHAR_PRODUCT_PROGRESS = "5a401527$SUFFIX"

    /** P-Mode — heartbeat write (`00 7F 80`). Write. Obfuscated. */
    const val CHAR_PMODE = "5a401529$SUFFIX"

    /** Barista mode lock/unlock. Write. Obfuscated but WITHOUT the byte-0=key step. */
    const val CHAR_BARISTA = "5a401530$SUFFIX"

    /** Statistics command. Write/Read. Obfuscated. */
    const val CHAR_STATISTICS_CMD = "5a401533$SUFFIX"

    /** Statistics data. Read. Obfuscated. */
    const val CHAR_STATISTICS_DATA = "5a401534$SUFFIX"

    // --- UART service ---
    /** UART RX. Write/Notify. (TX/RX labelling is reversed between some references.) */
    const val CHAR_UART_RX = "5a401624$SUFFIX"

    /** UART TX. Read/Notify. */
    const val CHAR_UART_TX = "5a401625$SUFFIX"

    /** Client Characteristic Configuration Descriptor (for enabling notifications). */
    const val CCCD = "00002902-0000-1000-8000-00805f9b34fb"

    /** Protocol timing constants (§7.1). All values in milliseconds unless noted. */
    object Timing {
        /** Machine drops the link after ~20 s without traffic. */
        const val IDLE_DISCONNECT_MS = 20_000L

        /** Heartbeat must be sent at least this often ("10 is too late, 9 is ok"). */
        const val HEARTBEAT_INTERVAL_MS = 9_000L

        /** Keep the link warm for ~120 s after the last user activity. */
        const val ACTIVE_WINDOW_MS = 120_000L

        /** A queued command is valid for ~15 s before it is dropped. */
        const val COMMAND_TTL_MS = 15_000L

        /** Initial wait before polling for statistics readiness. */
        const val STATS_INITIAL_WAIT_MS = 1_200L

        /** Statistics poll interval and max attempts. */
        const val STATS_POLL_INTERVAL_MS = 800L
        const val STATS_MAX_POLLS = 30

        /** Recommended GATT connection retry settings. */
        const val CONNECT_RETRY_DELAY_MS = 1_000L
        const val CONNECT_MAX_RETRIES = 3
    }
}
