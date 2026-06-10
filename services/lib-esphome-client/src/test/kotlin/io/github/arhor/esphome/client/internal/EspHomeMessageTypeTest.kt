package io.github.arhor.esphome.client.internal

import kotlin.test.Test
import kotlin.test.assertEquals

class EspHomeMessageTypeTest {

    @Test
    fun `contains discovery message ids from api proto`() {
        assertEquals(11, EspHomeMessageType.LIST_ENTITIES_REQUEST)
        assertEquals(12, EspHomeMessageType.LIST_ENTITIES_BINARY_SENSOR_RESPONSE)
        assertEquals(13, EspHomeMessageType.LIST_ENTITIES_COVER_RESPONSE)
        assertEquals(14, EspHomeMessageType.LIST_ENTITIES_FAN_RESPONSE)
        assertEquals(15, EspHomeMessageType.LIST_ENTITIES_LIGHT_RESPONSE)
        assertEquals(16, EspHomeMessageType.LIST_ENTITIES_SENSOR_RESPONSE)
        assertEquals(17, EspHomeMessageType.LIST_ENTITIES_SWITCH_RESPONSE)
        assertEquals(18, EspHomeMessageType.LIST_ENTITIES_TEXT_SENSOR_RESPONSE)
        assertEquals(19, EspHomeMessageType.LIST_ENTITIES_DONE_RESPONSE)
        assertEquals(41, EspHomeMessageType.LIST_ENTITIES_SERVICES_RESPONSE)
        assertEquals(43, EspHomeMessageType.LIST_ENTITIES_CAMERA_RESPONSE)
        assertEquals(46, EspHomeMessageType.LIST_ENTITIES_CLIMATE_RESPONSE)
        assertEquals(49, EspHomeMessageType.LIST_ENTITIES_NUMBER_RESPONSE)
        assertEquals(52, EspHomeMessageType.LIST_ENTITIES_SELECT_RESPONSE)
        assertEquals(55, EspHomeMessageType.LIST_ENTITIES_SIREN_RESPONSE)
        assertEquals(58, EspHomeMessageType.LIST_ENTITIES_LOCK_RESPONSE)
        assertEquals(61, EspHomeMessageType.LIST_ENTITIES_BUTTON_RESPONSE)
        assertEquals(63, EspHomeMessageType.LIST_ENTITIES_MEDIA_PLAYER_RESPONSE)
        assertEquals(94, EspHomeMessageType.LIST_ENTITIES_ALARM_CONTROL_PANEL_RESPONSE)
        assertEquals(97, EspHomeMessageType.LIST_ENTITIES_TEXT_RESPONSE)
        assertEquals(100, EspHomeMessageType.LIST_ENTITIES_DATE_RESPONSE)
        assertEquals(103, EspHomeMessageType.LIST_ENTITIES_TIME_RESPONSE)
        assertEquals(107, EspHomeMessageType.LIST_ENTITIES_EVENT_RESPONSE)
        assertEquals(109, EspHomeMessageType.LIST_ENTITIES_VALVE_RESPONSE)
        assertEquals(112, EspHomeMessageType.LIST_ENTITIES_DATETIME_RESPONSE)
        assertEquals(116, EspHomeMessageType.LIST_ENTITIES_UPDATE_RESPONSE)
    }

    @Test
    fun `contains entity state message ids from api proto`() {
        assertEquals(20, EspHomeMessageType.SUBSCRIBE_STATES_REQUEST)
        assertEquals(21, EspHomeMessageType.BINARY_SENSOR_STATE_RESPONSE)
        assertEquals(22, EspHomeMessageType.COVER_STATE_RESPONSE)
        assertEquals(23, EspHomeMessageType.FAN_STATE_RESPONSE)
        assertEquals(24, EspHomeMessageType.LIGHT_STATE_RESPONSE)
        assertEquals(25, EspHomeMessageType.SENSOR_STATE_RESPONSE)
        assertEquals(26, EspHomeMessageType.SWITCH_STATE_RESPONSE)
        assertEquals(27, EspHomeMessageType.TEXT_SENSOR_STATE_RESPONSE)
        assertEquals(47, EspHomeMessageType.CLIMATE_STATE_RESPONSE)
        assertEquals(50, EspHomeMessageType.NUMBER_STATE_RESPONSE)
        assertEquals(53, EspHomeMessageType.SELECT_STATE_RESPONSE)
        assertEquals(56, EspHomeMessageType.SIREN_STATE_RESPONSE)
        assertEquals(59, EspHomeMessageType.LOCK_STATE_RESPONSE)
        assertEquals(64, EspHomeMessageType.MEDIA_PLAYER_STATE_RESPONSE)
        assertEquals(95, EspHomeMessageType.ALARM_CONTROL_PANEL_STATE_RESPONSE)
        assertEquals(98, EspHomeMessageType.TEXT_STATE_RESPONSE)
        assertEquals(101, EspHomeMessageType.DATE_STATE_RESPONSE)
        assertEquals(104, EspHomeMessageType.TIME_STATE_RESPONSE)
        assertEquals(108, EspHomeMessageType.EVENT_RESPONSE)
        assertEquals(110, EspHomeMessageType.VALVE_STATE_RESPONSE)
        assertEquals(113, EspHomeMessageType.DATETIME_STATE_RESPONSE)
        assertEquals(117, EspHomeMessageType.UPDATE_STATE_RESPONSE)
    }
}
