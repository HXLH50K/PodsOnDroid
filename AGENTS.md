# Project conventions

- Write new commit subjects, commit bodies, coding documentation, and code comments in English. Do not rewrite upstream history to change its language.
- Support English and Simplified Chinese only. Keep user-facing strings in Android resources.
- Keep independent P.O.D.S. Git history. Preserve the HyperPods license, attribution, and pinned source provenance without importing its commit ancestry.
- Use modern libxposed API 102 only; do not introduce legacy Xposed or YukiHook runtime calls.
- Keep the GitHub repository private unless the user explicitly changes its visibility.
- Never commit signing keys, passwords, captured system APKs, raw device logs, or local environment configuration.
- Target the current HyperOS 4 test device first. Do not claim earbud functionality without testing the user's AirPods 5.
- When physical earbud interaction is required, finish and verify the independent milestone, report the exact setup needed, and wait for the user.
