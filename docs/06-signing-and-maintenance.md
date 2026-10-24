# 06 Signing, release delivery, and maintenance

## 2026-10-24 0.2.2-beta publication authorization

The user authorized committing/pushing the current changes and publishing the
new GitHub Release. Publish `v0.2.2-beta` as a prerelease with the signed
production APK and its SHA-256 sidecar, using the existing signing identity.
The corresponding source tag must identify the exact committed build source.
Rebuild after committing so the APK's revision and module self-check match
that source; do not upload the earlier local-worktree APK recorded below.
The release notes and uploaded checksum identify the publication artifact.
Keep the matching R8 mapping locally, not as a public release asset.

Publication checks cover package/version/minimum/target metadata, signature
and alignment, the source tag, branch synchronization and downloaded asset
bytes. Preserve the HyperPods attribution, pinned source provenance, GPL
license and inherited file-level notices. The unrelated local issue document,
signing credentials, system binaries and raw device logs remain excluded.
No additional device installation, host restart or earbud acceptance is part
of this publication request. Lenovo socket construction is verified; full
AACP communication on the tablet remains a collaborative acceptance step.

## 2026-10-24 local 0.2.2-beta hotfix build

The user requested the smallest application-version increment and a rebuilt
Release APK, not a higher minimum Android version. `0.2.2-beta` uses version
code 21, retaining `com.hxlh50k.pods`, Android 15 / API 35 minimum, target API 36,
and the existing production certificate. It includes the Lenovo Android 16
classic L2CAP constructor correction recorded in the architecture document.

- Signed APK: `dist/PODS-0.2.2-beta-release.apk`, 8,955,707 bytes.
- APK SHA-256: `62efb5e7098f2d05c9cda443a91099f87e6168faba9cac0133ea892b98509bb9`.
- The SHA-256 sidecar and matching R8 mapping are retained in the ignored
  delivery directory.
- APK Signature Scheme v3 verification passed with the existing certificate
  SHA-256 `EB479AF1A543727D00152C6BA41DB237D38703AF08173272ED21BCBF7AB09FC2`.
- Package/version/minimum/target metadata and the non-debuggable flag were
  checked on the signed APK. `zipalign -c -P 16 4` passed.
- The minified/resource-shrunk Release build, 27 focused Release JVM checks
  covering socket compatibility, system profiles/integrations and version-aware
  module reports, and Release Lint passed. Lint has zero errors, 130 warnings
  and two hints.

This is a local worktree build, not a tagged or published GitHub Release.
No Git commit, device installation or host restart was performed for this
version-bump request. The prior same-version hotfix deployment and Lenovo
socket-construction check do not establish full earbud acceptance.

## 2026-10-24 public 0.2.1-beta

The user explicitly authorized public repository visibility, pushing the
completed work, signed GitHub Release publication and updating the test phone.
GitHub confirmed administrator permission and the visibility change to public.
This supersedes earlier private-hosting language in historical checkpoints.
It does not authorize publication of private keys, passwords, local signing
configuration, captured system APKs, raw device logs or the local issue document.

Version `0.2.1-beta` has monotonically increased version code 20. It retains the
same production application ID and certificate recorded below. Publish only
the signed, minified APK and its SHA-256 sidecar as prerelease assets, attached
to a tag at the exact build-source commit. Source and existing license notices
are available at that tag; no upstream notice is removed or relicensed.

Publication preflight inspected 802 historical file blobs for high-confidence
private-key, GitHub-token, AWS-key and literal signing-password markers, and
checked current/history filenames for excluded key/binary/device-log material.
No matches were found. This is a targeted publication check, not a comprehensive
security or licensing certification.

The user accepted the liquid-glass visual result on the target phone. AP5 +
HyperOS 4 remains the only physical test baseline; known missing setting
readbacks and unfinished controls remain disclosed in the release notes.
Release build, 112 focused production-variant JVM checks and Lint passed for
this version bump; signature, package version, alignment and uploaded asset
bytes must also be checked before considering publication complete.

## 2026-10-24 beta release signing

The requested production version is `0.2.0-beta` (version code 19). The
production ID remains `com.hxlh50k.pods`; Debug uses the separate
`com.hxlh50k.pods.debug` ID, so Android cannot migrate its app data automatically.
Uninstalling Debug removes that installation's data and requires explicit user
authorization, which was provided for the final Release replacement.

Preflight found no signing configuration, signing environment inputs or key at
the approved P.O.D.S. path. The user subsequently authorized a new independent
key under their OneDrive keys root; no Cowpea or other project's key was read
or reused. A new RSA-3072 / SHA256withRSA PKCS12 key was generated on 2026-10-24,
with alias `pods-release` and validity 10000 days:

- Key: `C:\Users\25806\OneDrive\keys\PodsOnDroid\pods-release.p12`.
- Public certificate: `pods-release-cert.pem` in the same folder.
- Certificate SHA-256: `EB479AF1A543727D00152C6BA41DB237D38703AF08173272ED21BCBF7AB09FC2`.
- Non-secret local metadata: `signing-info.json` in that folder.
- Strong random password: Windows DPAPI encrypted at
  `C:\Users\25806\AppData\Local\PodsOnDroid\signing\release-password.dpapi`,
  outside OneDrive. No password was printed or committed.
- Local signing helper: `Sign-Release.ps1` in the same local signing directory.
  It decrypts only in memory, passes credentials via process environment, signs
  an already-built APK, verifies it, and clears credentials afterward.

**Custody limitation:** DPAPI depends on this Windows user's profile. Copying
the encrypted password file alone is not a portable password backup. The user
must separately export the password to a password manager and back up the key
to independent encrypted storage before losing or replacing the Windows profile.
OneDrive synchronization or an offline recovery rehearsal is not claimed here.
Newly created signing directories have restricted local ACLs; this does not
assert additional encryption of OneDrive content.

The Gradle release remains explicitly unsigned by default. Local release
delivery signs after minification/resource shrinking and verifies package,
version and certificate before removing Debug. Never substitute the Debug
certificate or uninstall Debug before a release-signed APK passes verification.

The beta's minified/resource-shrunk Release build and 94 focused Release JVM
regressions passed. Release Lint reports 0 errors, 130 warnings and 2 hints.
Installing the production package does not automatically activate its module
or migrate Debug's module scopes. The user must enable `com.hxlh50k.pods` and
select the relevant hosts in the framework. No automatic host restart is
authorized by package replacement.

### Beta delivery receipt

- Source revision: `3582eb6`; `versionCode=19`, `versionName=0.2.0-beta`.
- Artifact: `dist/PODS-0.2.0-beta-release.apk`, 8,996,724 bytes.
- APK SHA-256: `eabdcf1716dde5c983fbe312f2a2bf0973caee9cb546c372cff4cecb630aae1c`.
- Signature: APK Signature Scheme v3, one RSA-3072 P.O.D.S. signer with the
  certificate fingerprint recorded above. `apksigner verify` passed.
- Alignment: `zipalign -c -P 16 -v 4` passed.
- Production package: `com.hxlh50k.pods`; minimum API 35, target API 36;
  non-debuggable, minified, resources shrunk, all existing ABI libraries retained.
- Module audit: API 102 metadata and `HookEntry` preserved after R8, no legacy
  entry and no Debug preview activity. Existing static host scopes retained.
- R8 mapping and an APK checksum sidecar are retained next to the APK in the
  ignored delivery directory. Neither binaries nor signing material are committed.
- Device `ccab6460`: verified staged APK hash, uninstalled exactly
  `com.hxlh50k.pods.debug.test` and `com.hxlh50k.pods.debug`, then installed
  Release through the authorized root package-manager workflow. All returned
  `Success`. No other package was removed.
- Post-install: only production P.O.D.S. remains; installed version/code, APK
  bytes and signing certificate match the delivery artifact; launcher resolves.
- No phone reboot, host restart, radio capture, headphone command or acceptance
  automation was performed. Module activation and user-operated acceptance
  remain separate from successful package installation.

The older sections below describe historical planning checkpoints, not proof
that a release key has since been created.

Historical planning baseline: 2026-10-22. The older procedures below are plans;
completed key creation and beta validation are recorded in the section above.

**Historical privacy checkpoint:** [HXLH50K/PodsOnDroid](https://github.com/HXLH50K/PodsOnDroid) was initially private. The explicit public authorization above supersedes that policy. Public or private storage does not waive applicable GPL obligations when distributing a derivative.

## 1. Product and signing identity

- Exact label: **P.O.D.S.**
- Release application ID: `com.hxlh50k.pods`; isolated debug application ID: `com.hxlh50k.pods.debug`.
- Retain Kotlin/Android namespace `moe.chenxy.hyperpods` for now. Source namespace is not installation/signing identity.
- Independent release key: never reuse HyperPods, LibrePods, Android debug, Cowpea, or another project's certificate.
- Preserve source provenance, authorship, and applicable GPL while maintaining independent P.O.D.S. Git history. A new certificate must not pretend to update the original HyperPods package.
- Install under the new ID; isolate other controllers during authorized tests. Use explicit export/import for settings migration, not another app's private storage.
- Standalone APK, its LSP code, and any future KSU-bundled APK share the release identity.
- Increase `versionCode` monotonically; use an understandable `versionName`, for example `0.1.0-alpha.1`.
- Debug application-ID isolation is now implemented. Its hook/IPC identity behavior still requires integrated validation; release signing remains separate and unsigned at this checkpoint. Never use release secrets for routine experiments.
- Coding docs and commits are English; app resource scope is English and Simplified Chinese only.
- No early automatic updater or implicit upload authorization.

## 2. New-key storage proposal

The user authorized a **new folder/new key** beneath the following root. These candidate files have **not been created by this documentation work**:

```text
C:\Users\25806\OneDrive\keys\PodsOnDroid\
    pods-release.p12
    pods-release-cert.pem
    signing-info.txt
```

- At the authorized key-generation stage, check only the proposed new path for existence. If occupied, use a new name such as `PodsOnDroid-20261022` or the actual creation date; **never overwrite/reuse a key**.
- `pods-release.p12`: strong-password PKCS12 private-key container.
- `pods-release-cert.pem`: public certificate, not a private key.
- `signing-info.txt`: alias, creation time, algorithm, validity, certificate SHA-256, custody method; never the password.
- Proposed alias `pods-release`, RSA **3072**, SHA256withRSA, validity **10000 days**. These are compatibility-oriented proposals, adjustable before generation.
- Avoid a Windows directory name ending in `P.O.D.S.` because trailing dots are problematic; the APK label remains unchanged.
- Cowpea Release Key is only an organizational/non-sensitive-convention reference, not authorization to inspect, output, or reuse its secrets. Do not enumerate unrelated key folders.

## 3. Generation and custody

The root path/new-key requirement is settled. Before release-key generation, settle password custody/delivery and an independent encrypted backup. This does not block isolated M0/M1 test builds.

1. Verify the controlled JDK/keytool, clock, unused destination, and Windows file permissions.
2. Store a long random password in a password manager. Do not print it in chat/logs or expose it as a command-line argument.
3. Interactively generate the PKCS12 with `keytool -genkeypair`, explicitly selecting store type, algorithm, alias, and validity; refuse overwrite.
4. Verify type, alias, validity, and certificate fingerprint with `keytool -list`; export the public certificate.
5. Back up on separate encrypted offline media and rehearse verification from backup. Keep password custody separate from the keystore backup.
6. Confirm OneDrive account/sharing/sync boundaries with the user. Local ACLs are not an additional cloud-encryption guarantee.
7. Sign a controlled test artifact and verify its certificate before the first release. A successful keytool exit alone does not validate the full signing process.

No existing private key/password needs to be read for this design.

## 4. Build integration

- Never commit secret-bearing signing configuration or credentials to Gradle, versioned properties, CI YAML, or Markdown. The approved destination layout above is a plan, not executable secret configuration.
- Load release signing inputs from controlled local configuration or short-lived environment injection. Missing inputs must fail release signing, not silently fall back to a debug certificate.
- Audit ignore rules for keystore/p12/jks, private properties, raw diagnostics/captures, extracted system APKs, and decompilation directories. Public certificates and private keys are distinct.
- Load private keys only during the release-signing step. Initial CI should build/test without release secrets; sign locally on the user's machine.
- The 2026-10-22 local CI update targets `main` in the private repository with debug-only tests/artifact upload and `contents: read`, without secrets restoration or release publishing. The remote is still empty, so this is configured behavior, not a successful remote CI run; see the [baseline checkpoint](04-delivery-plan.md#9-measured-baseline-checkpoint).
- Do not upload private keys/passwords to third-party CI. Any future automated signing needs separate authorization, isolation, and revocation planning.
- Record dependency pinning, wrapper verification, source/toolchain versions, and artifact SHA-256; never dump the full environment into logs.

See [Android signing guidance](https://developer.android.com/studio/publish/app-signing). Labels can change, but package/certificate affect upgrade identity. Losing a sideloaded app's release key normally loses ordinary updates under that identity.

## 5. APK and module delivery

### Primary: modern API 102 LSP APK

- One APK provides settings UI and the modern module entry. Root/API 102 are prerequisites, not a rootless promise.
- Installation instructions must distinguish **APK installed**, **framework enabled**, **scope loaded**, and **control ready**.
- `minApiVersion=102`, `targetApiVersion=102`, no legacy compatibility; framework name/version is not proof.
- Explain scopes by responsibility: Bluetooth control, Xiaomi quick-connect, SystemUI/plugin, MiLink panel. Add only verified hosts, never `system_server` by default.
- For hook/native updates, use a validated disable/restart lifecycle, not hot reload as a correctness assumption.
- R01 handles exact verified host PIDs sequentially and confirms the current build from each restored host. Explain Bluetooth/UI interruption. Framework caches/environment upgrades may still require phone reboot; do not falsely report completion.

### Optional KSU package

Only after M5 demonstrates need:

- Bundle the same release-signed APK, package, and versionCode; no separate signing key.
- ZIP SHA-256/package signatures are not APK signing; verify each separately.
- Match system APK and privileged allowlist to the correct partition. Grant only demonstrated `signature|privileged` needs; no default phone/cross-user/local-MAC privileges.
- A self-signed privileged APK is not platform-signed and cannot receive every signature-only permission through allowlisting.
- Never replace the complete Bluetooth APK/APEX/library/configuration, disable SELinux, add broad allow rules, or globally spoof properties.
- Check actual KSU branch/version, mounting/metamodule ability, conflicts, existing certificate, and version relationship; refuse unsupported installation.
- Define one update rule for module-bundled and user-data APK versions. Avoid unrecorded combinations of an old module APK and newer user APK.

Proposed rule: each KSU release records its embedded APK version/hash. User updates must share the certificate, have a version no lower than the embedded APK, and pass that module version's compatibility checks. Before removal, ensure a usable user APK and verify safe loss of privileged capabilities. Test migrations on the controlled device first.

## 6. Release checklist

- [ ] The actual phone/ROM/framework/headphone matrix is tested; unknown combinations are labeled.
- [ ] Real AirPods 5 lid popup and dedicated panel work; notification/full-Activity substitutes do not pass.
- [ ] Final release APK has consistent API 102 metadata/R8 entry, no legacy/YukiHook runtime calls, and hot reload disabled.
- [ ] Required JVM, Lint, target-ABI, instrumentation, and physical-device cases pass.
- [ ] Native libraries support required **4 KB/16 KB** page sizes; ELF and APK packaging alignment checked per [Android guidance](https://developer.android.com/guide/practices/page-sizes).
- [ ] min/target/compile SDK match actual support claims, not copied assumptions.
- [ ] Debug export components, test entry points, raw logs, and unnecessary permissions are removed/restricted.
- [ ] Final `apksigner verify --verbose --print-certs` fingerprint matches the registered identity.
- [ ] Required signing schemes/install compatibility are tested; optimize/align before signing and never modify the signed APK afterward.
- [ ] Artifact SHA-256, release notes, source revision, known limitations, and recovery instructions recorded.
- [ ] Same-signature P.O.D.S. upgrades retain settings; HyperPods migration uses independent installation and explicit data transfer.
- [ ] R01 selected hosts report the current build; old/absent module, root denial, and timeouts are explicit; no system-core/automatic whole-phone restart.
- [ ] English/Simplified Chinese app resources and English documentation are consistent; no additional locale claim.
- [ ] License inventory, modification notices, corresponding source/build instructions, and asset authorization are ready.
- [ ] If KSU exists, embedded/standalone APK equality and install/disable/uninstall are tested.
- [ ] The exact delivery channel/upload and repository visibility are authorized. Never upload signing material or raw device logs.

## 7. Disable and rollback

### When the app is usable

1. Disable enhancement, releasing its own connection/policies.
2. Disable optional presentation adapters first to diagnose whether normal control recovers; this diagnostic fallback does not waive required release features.
3. Disable/un-scope this module in the framework, then use the verified restart procedure; do not repeatedly restart Bluetooth to conceal crashes.
4. If using KSU, disable/remove this package through the tested transition, without deleting unrelated modules.

### When the system cannot boot normally

- Verify the actual root/framework rescue method **before injection**, with device-matching recovery information available.
- [KernelSU rescue guidance](https://kernelsu.org/guide/rescue-from-bootloop.html) describes system safe mode and early-boot volume-key mechanisms, but branch/timing/device behavior varies.
- Prefer the manager or controlled ADB action targeting this module ID. No wholesale deletion of module directories, data wipe, or mismatched boot image flashing.
- Java fallback cannot prevent native crashes; logging alone is not a sufficient safety mechanism.

### Version rollback

- Android normally rejects a lower versionCode; installing an old APK is not a universal rollback method.
- Prefer a higher-versionCode recovery release containing the previously verified implementation with compatible data handling.
- Any necessary uninstall/reinstall requires user confirmation and backup of migratable settings; wiping data is not routine upgrading.
- Coordinate KSU and user APK versions so re-enabling a package cannot silently restore a different version.

## 8. Maintenance

File-level licensing still needs attention before wider distribution: the inherited `AACPManager.kt` header declares AGPL version 3, while the root license text is GPL version 3. Keep those notices intact and review the combined distribution obligations; neither private hosting nor the ancestry reset resolves that difference or permits silently relabeling the file.

- Track phone/ROM/Bluetooth components/framework/headphone firmware per release, not just “HyperOS 4 supported.”
- OTA, Bluetooth APEX, firmware, and framework updates trigger relevant regression; unknown native images are not patched.
- Issue templates request redacted environment, steps, and stage errors, not whole system logs, serials, or keys.
- Review protocol changes for source/license/fixtures before porting; do not auto-follow nightlies into production profiles.
- Prioritize connection, truthful state, power, and upgrade stability before new models/visual extras.
- Preserve HyperPods attribution and pinned provenance, not inherited commit ancestry. Review upstream patches individually without merging the old history or legacy hook registration. Track native-presentation changes separately from protocol fixes.
- Capture raw debug messages only during an authorized window and clean them afterward. Release diagnostics are fixed-field/local, never automatic remote collection.
- Record reused project/commit/file, license, copyright, changes, and asset grants. When distributing, satisfy applicable corresponding-source/build-instruction obligations even if hosting remains private.

Repository creation alone does not authorize a push, release, or visibility change.
The 2026-10-24 publication authorization above covers this beta. Future automated
signing/distribution still requires its own decision and must not expose local keys.
