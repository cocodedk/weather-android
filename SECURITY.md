# Security Policy

## Reporting a Vulnerability

Do **not** open a public GitHub issue for security vulnerabilities.

To report a vulnerability:
- Use the **"Report a vulnerability"** button on the Security tab of this repository (GitHub private advisory)
- Or email: babak@cocode.dk

We will acknowledge within 5 business days and aim to release a fix within 30 days of confirmation.

## Scope notes

This app has a deliberately small attack surface, which is worth knowing before
you report:

- It has **no server of its own**. Its own network calls are `GET` requests to
  `api.open-meteo.com` and `geocoding-api.open-meteo.com`. Android's location and
  place-naming services, which the app asks for help, may contact other providers.
- It has **no accounts, no API keys and no analytics**. There are no credentials
  to leak.
- Location permission is optional. The app sends the coordinates to Open-Meteo in the
  forecast request, and may also pass them to Android's place-naming service to
  get a place name.
- The About screen's buttons hand a web address to your browser. The app itself does
  not open those pages.
- Everything the app stores is a DataStore file in the app's private directory:
  your saved places, your unit and theme preferences, and the last forecast
  per place. Android may include these files in a phone backup.

Reports about the handling of location data, the cached responses, or the
permission flow are in scope and welcome.

## Supported Versions

| Version | Supported |
|---------|-----------|
| latest  | ✅ |
| older   | ❌ |
