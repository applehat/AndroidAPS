# twiist (Sequel Med Tech) driver - EXPERIMENTAL SKELETON

Personal-use interoperability work. **Insulin delivery is locked** (`TwiistSafety.DELIVERY_ENABLED = false`).

## Modules
| Module | Purpose |
|---|---|
| `:pump:twiist` | AAPS plugin (`TwiistPumpPlugin`, Metro key 1140), state (`TwiistPump`), DI transport selection, Compose overview |
| `:pump:twiist:protocol` | Semantic command/response model, `TwiistBleTransport`, `TwiistCodec`. `EmulatorOnlyCodec` is INVENTED framing, not the wire protocol |
| `:pump:twiist:emulator` | `TwiistPumpEmulator` + `TwiistEmulatorBleTransport` with fault injection, for hardware-free testing |

Emulator is selected only when the `emulate_twiist` marker file (ExternalOptions.EMULATE_TWIIST) is present; otherwise the
radio-less `TwiistNoRadioBleTransport` refuses every connection.

## Before delivery can ever be unlocked (safety checklist)
- [ ] Real codec derived from BLE captures; byte-level tests from captured frames
- [ ] Bolus: pump-reported delivered amount drives PumpSync (never the request); partial delivery + cancel handled
- [ ] Lost link mid-command => "unknown outcome": query status before any retry, never blind-resend a bolus
- [ ] TBR / profile write verified by readback; `isThisProfileSet` compares pump-reported values
- [ ] Alarms (occlusion, reservoir, battery, pump error) surfaced via AAPS notifications/alarm (screenless pump)
- [ ] Pump behaviour when AAPS goes silent is understood; unreachable alert forced on
- [ ] Pairing exclusivity vs the official twiist app; no two controllers
- [ ] Time/timezone/DST handling; pump-unique ids for history idempotency
- [ ] PumpType limits (SEQUEL_TWIIST) replaced with measured values
- [ ] Every emulator fault case covered by driver tests
- [ ] Saline-only bench testing on real hardware with logging, before any insulin
