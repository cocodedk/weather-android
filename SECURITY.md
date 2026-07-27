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

- It has **no backend**. The only network calls are `GET` requests to
  `api.open-meteo.com` and `geocoding-api.open-meteo.com`.
- It has **no accounts, no API keys and no analytics**. There are no credentials
  to leak.
- Location permission is optional and never leaves the device except as the
  latitude/longitude in the forecast request.
- Everything the app stores is a DataStore file in the app's private directory:
  your saved places, your unit preference, and the last forecast response per
  place.

Reports about the handling of location data, the cached responses, or the
permission flow are in scope and welcome.

## Supported Versions

| Version | Supported |
|---------|-----------|
| latest  | ✅ |
| older   | ❌ |
