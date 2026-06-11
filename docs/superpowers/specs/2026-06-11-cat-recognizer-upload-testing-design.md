# Cat Recognizer Upload Testing Design

## Goal

Allow a developer or operator to upload a local image through the cat recognizer UI and run the existing OpenCV cat detector against it. This is for testing detector behavior without needing to change the live ESPHome camera feed.

## Scope

The feature adds an opt-in debug upload flow to the existing static UI and recognition API. Uploaded images are evaluated as one-off inputs and do not replace the latest camera frame, worker state, or live recognition snapshot.

## Configuration

Add `cat-recognizer.debug.upload-enabled` to the existing debug config group. The default value is disabled, while the Quarkus dev profile enables it with `%dev.cat-recognizer.debug.upload-enabled=true`.

The UI checks the safe debug config endpoint before showing upload controls. The upload endpoint also enforces the flag and returns HTTP 403 when disabled.

## API

Add `POST /api/recognition/upload` accepting `multipart/form-data` with an image file part. The endpoint reads the uploaded bytes, wraps them in a `FramePayload`, calls `OpenCvCatDetector`, and returns a `RecognitionResult` with `source = "upload"`.

Detector failures produce a normal `RecognitionResult` with `status = UNKNOWN`, `code = DETECTOR_FAILED`, and the detector error message. Empty or missing upload data is rejected as a bad request.

## UI

Extend `index.html` with a small "Upload test image" section. When enabled, the user can choose an image, preview it locally, submit it, and see the same status and bounding-box overlay behavior used by the live view.

The live polling view remains unchanged. Upload results render in their own preview area so test images do not interfere with the latest camera frame.

## Tests

Add controller coverage for enabled upload, disabled upload, and invalid image data. Existing detector unit tests continue to cover OpenCV decoding behavior, while the upload endpoint tests verify request handling, config gating, and result mapping.
