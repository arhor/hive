# ESP32-CAM ESPHome Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the placeholder `esphome/config/esp32-cam.yaml` with a working generic ESP32-CAM configuration based
on the approved design and the user's previously working camera setup.

**Architecture:** Keep the change isolated to one ESPHome YAML file. Reuse the existing repo API and OTA settings,
switch the board/framework to the known-good ESP32-CAM values, and add the camera, flash LED, web server endpoints, and
basic diagnostic entities directly in the same config.

**Tech Stack:** ESPHome YAML, ESP32-CAM (`esp32cam` board), Arduino framework, Home Assistant API integration

---

### Task 1: Replace The Placeholder ESPHome Config

**Files:**

- Modify: `esphome/config/esp32-cam.yaml`
- Reference: `docs/superpowers/specs/2026-06-09-esp32-cam-esphome-design.md`

- [ ] **Step 1: Review the current placeholder file and the approved spec**

Run: `sed -n '1,240p' esphome/config/esp32-cam.yaml`
Expected: the file shows the current generic ESP32 placeholder without `psram`, `esp32_camera`, flash light, or camera
web server sections.

Run: `sed -n '1,260p' docs/superpowers/specs/2026-06-09-esp32-cam-esphome-design.md`
Expected: the spec confirms `board: esp32cam`, Arduino framework, PSRAM, the provided camera pin map, generic
`esp32-cam` naming, and stream/snapshot endpoints on ports `8080` and `8081`.

- [ ] **Step 2: Replace the placeholder YAML with the approved ESP32-CAM config**

Update `esphome/config/esp32-cam.yaml` so it contains this structure:

```yaml
esphome:
    name: esp32-cam
    friendly_name: esp32-cam

esp32:
    board: esp32cam
    framework:
        type: arduino

psram:

logger:

api:
    encryption:
        key: "GuGRKDKQSBsdvkn0mOHs2UDAW4ecvhk7n470jQQ5q0Y="

ota:
    -   platform: esphome
        password: "d14bcfccf4319f9cda8864f3110bb508"

wifi:
    ssid: !secret wifi_ssid
    password: !secret wifi_password
    ap:
        ssid: "Esp32-Cam Fallback Hotspot"
        password: "ydWSWhBWr5gx"

captive_portal:

time:
    -   platform: homeassistant
        id: homeassistant_time

output:
    -   platform: gpio
        pin: GPIO4
        id: camera_flash

light:
    -   platform: binary
        name: "ESP32-CAM Flash"
        output: camera_flash
        restore_mode: ALWAYS_OFF

esp32_camera:
    name: "ESP32-CAM Camera"
    external_clock:
        pin: GPIO0
        frequency: 20MHz
    i2c_pins:
        sda: GPIO26
        scl: GPIO27
    data_pins:
        - GPIO5
        - GPIO18
        - GPIO19
        - GPIO21
        - GPIO36
        - GPIO39
        - GPIO34
        - GPIO35
    vsync_pin: GPIO25
    href_pin: GPIO23
    pixel_clock_pin: GPIO22
    power_down_pin: GPIO32
    resolution: 800x600
    jpeg_quality: 12
    max_framerate: 5 fps
    idle_framerate: 0.2 fps
    vertical_flip: false
    horizontal_mirror: false

esp32_camera_web_server:
    -   port: 8080
        mode: stream
    -   port: 8081
        mode: snapshot

sensor:
    -   platform: wifi_signal
        name: "ESP32-CAM WiFi Signal"
        update_interval: 60s

button:
    -   platform: restart
        name: "ESP32-CAM Restart"
```

- [ ] **Step 3: Review the resulting YAML for correctness**

Run: `sed -n '1,240p' esphome/config/esp32-cam.yaml`
Expected: the file contains the camera config, flash light, camera web server, diagnostics, and preserves the existing
API key, OTA password, Wi-Fi secrets, and fallback hotspot.

Run: `git diff -- esphome/config/esp32-cam.yaml`
Expected: the diff shows a single-file replacement from the generic placeholder to the approved ESP32-CAM configuration.

- [ ] **Step 4: Record verification limits**

Document in the final handoff that this repository does not include an automated ESPHome validation step, so
verification is limited to YAML review unless the operator later runs ESPHome compile/validate in their own flashing
environment.
