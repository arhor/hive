## Goal

Replace the generic `esphome/config/esp32-cam.yaml` placeholder with a working ESP32-CAM configuration for the standard
`esp32cam` board using the known-good camera wiring and runtime settings already validated on the user's device.

## Scope

In scope:

- Rework `esphome/config/esp32-cam.yaml` into a usable ESP32-CAM device config
- Keep the repo-friendly generic device identity `esp32-cam`
- Preserve the existing API encryption key and OTA password already present in the repo file
- Use Wi-Fi credentials from ESPHome secrets
- Add the camera, snapshot/stream web endpoints, flash LED, and basic diagnostics

Out of scope:

- Adding motion detection, recording, or SD card storage
- Adding Home Assistant automations or dashboards
- Introducing device-specific naming like `parking-cam-1`
- Changing repository-wide Docker or Home Assistant configuration

## Recommended Approach

Replace the placeholder file with a reusable ESP32-CAM baseline derived from the user's previously working
configuration.

This is the best fit because it produces a config that is immediately useful on real hardware while keeping the file
generic enough to live in the repository as a template. A minimal camera-only variant would omit practical controls and
diagnostics that are already known to work, and a fully device-specific variant would leak one installation's naming
into shared repo config.

## Configuration Design

The updated ESPHome config should use:

- `esp32.board: esp32cam`
- `framework.type: arduino`
- `psram:`
- `esphome.name` and `friendly_name` set to `esp32-cam`

It should retain the current repo values for:

- `api.encryption.key`
- `ota` password
- Wi-Fi secret references
- fallback access point
- `logger`
- `captive_portal`

It should add:

- Home Assistant time source
- GPIO flash LED output on `GPIO4` exposed as a binary light
- `esp32_camera` using the known-good ESP32-CAM pin map
- Camera tuning values validated in the user's working config:
    - `resolution: 800x600`
    - `jpeg_quality: 12`
    - `max_framerate: 5 fps`
    - `idle_framerate: 0.2 fps`
    - `vertical_flip: false`
    - `horizontal_mirror: false`
- `esp32_camera_web_server` with:
    - stream on port `8080`
    - snapshot on port `8081`
- A Wi-Fi signal sensor
- A restart button

## Camera Wiring Assumption

The config assumes the standard ESP32-CAM board and the exact working pin mapping provided by the user:

- external clock: `GPIO0`, `20MHz`
- I2C:
    - SDA `GPIO26`
    - SCL `GPIO27`
- data pins:
    - `GPIO5`
    - `GPIO18`
    - `GPIO19`
    - `GPIO21`
    - `GPIO36`
    - `GPIO39`
    - `GPIO34`
    - `GPIO35`
- sync/control pins:
    - `vsync_pin: GPIO25`
    - `href_pin: GPIO23`
    - `pixel_clock_pin: GPIO22`
    - `power_down_pin: GPIO32`

## Data Flow

The device boots, connects to Wi-Fi, exposes the ESPHome native API to Home Assistant, initializes the OV2640 camera
with PSRAM enabled, and provides two lightweight HTTP endpoints through the ESPHome camera web server:

- `:8080` for stream access
- `:8081` for snapshot access

The flash LED can be toggled through Home Assistant, while diagnostic entities provide signal strength visibility and a
restart control.

## Error Handling And Operational Expectations

The config should favor stability over high frame rate. The selected resolution and frame rate values come from a
previously working deployment and are intended as safe defaults for initial bring-up.

If the image orientation is wrong on a physical device, operators should only need to adjust:

- `vertical_flip`
- `horizontal_mirror`

If camera initialization fails on a different board revision, the likely cause is a pin-map mismatch rather than a
broader ESPHome problem.

## Verification

After the YAML update, validation should be limited to config-level checks appropriate for this repository:

- Review the rendered YAML for correctness and consistency
- If ESPHome validation is available later in the operator environment, compile/validate this file there before flashing

No automated repository tests are required because this change only affects device configuration data.
