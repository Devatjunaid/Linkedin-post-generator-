# Store & brand assets

| File | Use |
| --- | --- |
| `play-store-icon-512.png` | Play Console → App icon (512×512) |
| `feature-graphic-1024x500.png` | Play Console → Feature graphic (1024×500) |
| `screenshots/screenshot-1…5-1080x1920.png` | Play Console → Phone screenshots (9:16) |
| `logo-transparent-2000x720.png` | Horizontal logo, transparent background (website, social, docs) |
| `logo-square-1024.png` | Square logo on cream (social profile picture, promo) |

The screenshots are designed renderings of the app's real phone layouts, using the same
animal artwork the app draws. They were not captured on a device. Once the app runs, compare
them against real screens before publishing.

`source/` holds the HTML that generates every asset. To regenerate after edits:
`node source/render.js` (needs Playwright + Chromium); the PNGs land in `source/out/`.
