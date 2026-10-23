# Download transfer diagnostics (4.1.3)

Open **Settings → Log** to view translated transfer events. **Open logs folder** opens the active data directory's logs.

`logs/download-transfers.jsonl` is a UTF-8 journal with one JSON record per event. It retains a 4 MiB current file and three rotated files (`.1` is newest). It records:

- UTC time, job ID, gateway IP and attempt ID;
- requested byte range and elapsed milliseconds;
- `attempt_bytes`: payload bytes received in this attempt, including unsuccessful attempts;
- `persisted_bytes`: useful saved job progress at the observation time;
- `wire_total_bytes`: cumulative received payload across the job, including repeat traffic;
- completion, interruption, failure reason and preservation of saved data.

Progress summaries are limited to one per attempt every 12 seconds. Attempt boundaries are recorded separately. `prefix_restarted` denotes an explicitly requested discard; `discarded_bytes` distinguishes it from a retry that preserves data. This patch prevents automatic whole-file resets. Source URLs and authentication secrets are excluded from this journal.

## Resume and source behavior

Some CDNs ignore ranges starting at zero while supporting nonzero ranges. The application checks a nonzero range before selecting whole-file mode. A first segment returned as HTTP 200 is read only to the segment boundary after validating the response. Other ranges must satisfy HTTP 206 and Content-Range validation. The final configured or discovered checksums remain enforced.

If an already ranged download temporarily receives HTTP 200 for a nonzero range, the response is rejected before writing. The selected gateway retries with a delay of 2–30 seconds instead of being permanently disabled after that single response.

An older sequential partial file can be reused as complete segments when source identity checks allow it. Its incomplete final segment is fetched again, so the displayed progress can decrease by less than one segment. This does not discard the complete saved segments.

If a source genuinely cannot resume, the application preserves the partial file, pauses the job and sets `restart_required=true`. Calling `POST /api/v1/jobs/{id}/resume` rechecks the source and does not authorize deletion of the partial file. Inspect `status`, `error`, `resumable`, `distribution_supported` and `restart_required` before scheduling repeated retries.
