# Flex (Medtronic Instinct / Flex) driver - EXPERIMENTAL SKELETON

Personal-use interoperability work (`#WeAreNotWaiting`). **Insulin delivery is locked**
(`FlexSafety.DELIVERY_ENABLED = false`).

Protocol opcodes, byte layouts and GATT UUID bases were recovered from the official MiniMed
Instinct US 1.3.1 APK (`libapp.so` via blutter) and live in `:pump:flex:protocol`. See
[applehat/reversing-flex](https://github.com/applehat/reversing-flex) / local
`/workspace/medtronic-flex/FINDINGS-opcodes.md`.

## Modules
| Module | Purpose |
|---|---|
| `:pump:flex` | AAPS plugin (`FlexPumpPlugin`, Metro key 1150), state (`FlexPump`), DI transport selection, Compose overview |
| `:pump:flex:protocol` | Semantic command/response model, recovered IDD opcode enums + `IddEncoders` (plaintext layouts), `FlexBleTransport`, `FlexCodec`. `EmulatorOnlyCodec` is INVENTED framing for the emulator only |
| `:pump:flex:emulator` | `FlexPumpEmulator` + `FlexEmulatorBleTransport` with fault injection, for hardware-free testing |

Emulator is selected only when the `emulate_flex` marker file (`ExternalOptions.EMULATE_FLEX`) is
present; otherwise the radio-less `FlexNoRadioBleTransport` refuses every connection.

## Authenticated channel (not implemented)
Live pumps wrap IDD PDUs inside **TLS 1.3 over BLE** (wolfSSL) + MTS, gated by Android attestation,
cloud enrollment and a post-TLS passkey. UUID→role mapping for the custom IDD characteristics is
still unconfirmed. This skeleton documents the plaintext IDD surface only.

## Before delivery can ever be unlocked (safety checklist)
- [ ] On-device GATT discovery log: confirm which of `0101/0102/0103/0105/0108/0110/0112/0113` is ICP / SRCP / status / history
- [ ] TLS 1.3 + MTS transport over BLE; enrollment / attestation / passkey flow understood (no secret material in-tree)
- [ ] Real codec derived from BLE captures inside the encrypted session; byte-level tests from captured frames
- [ ] Measured insulin unit scale (replace milli-unit stub in `IddEncoders.packInsulinUnits`)
- [ ] Bolus: pump-reported delivered amount drives PumpSync (never the request); partial delivery + cancel handled
- [ ] Lost link mid-command => "unknown outcome": query status before any retry, never blind-resend a bolus
- [ ] TBR / profile write verified by readback; `isThisProfileSet` compares pump-reported values
- [ ] Alarms (occlusion, reservoir, battery, pump error, SmartGuard) surfaced via AAPS notifications
- [ ] Pump behaviour when AAPS goes silent is understood; unreachable alert forced on
- [ ] No residual on-pump autonomy fights AAPS (SmartGuard / sensor-driven suspends): confirm dumb-pump mode
- [ ] Pairing exclusivity vs the official MiniMed Instinct app; no two controllers
- [ ] Time/timezone/DST handling; pump-unique ids for history idempotency
- [ ] PumpType limits (`MEDTRONIC_FLEX`) replaced with measured values
- [ ] Every emulator fault case covered by driver tests
- [ ] Saline-only bench testing on real hardware with logging, before any insulin
