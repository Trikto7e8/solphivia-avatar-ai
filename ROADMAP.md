# Roadmap

These are project goals, not promises about the current release.

## Community alpha

- Retest the Home shortcut icon refresh and continuous run animation on multiple Android devices.
- Add focused Android tests for animation state changes, chat parsing, and provider error handling.
- Add an in-app report action for problematic AI responses.
- Prepare privacy disclosures and Play Console foreground-service declarations.

## Model connections

- Extend the Stage A provider interface delivered in alpha11 with separately reviewed adapters and capability settings.
- Keep the documented open-source Sign in with ChatGPT connection as an optional adapter.
- Add user-configured connections to local LM Studio/Ollama servers.
- Add cloud provider adapters using each provider's supported authentication and billing model.
- Evaluate on-device inference separately; performance, storage, and model licenses vary by device and model.

## Avatar and voice

- Build a dedicated voice conversation screen inside Solphivia with a face or full-body avatar.
- Keep speech recognition, text-to-speech, and model selection as separate components so providers can vary independently.
- Keep the existing Android Home overlay user-controlled, with a visible stop action.

## Release

- Migrate the Android app to the current Google Play target API and test on-device.
- Complete the privacy policy, Data safety declaration, content rating, and foreground-service declarations.
- Prepare a signed Android App Bundle and use internal/closed testing before any production listing.

## iOS and community collaboration

Start with an iOS feasibility study and an in-app avatar prototype, with portable encrypted memory and independent provider adapters. Document platform limitations before committing to feature parity. See [COLLABORATE.md](COLLABORATE.md) for contribution areas.
