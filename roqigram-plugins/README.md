# RoqiGram plugins

Plugins for the exteraGram plugin SDK 1.4.x (Chaquopy). Install any `.plugin` file
from Settings → RoqiGram → Plugins.

| File | Tier | What it does |
|------|------|--------------|
| `RoqiNewYearPlugin.plugin` | Free | Snowfall toggles, holiday header, rotating icons, snow in nick for Premium |
| `RoqiFakeNFTPlugin.plugin` | Premium | Local decorative NFT-style badge and image selector; explicitly not an NFT |
| `RoqiNickFXPlugin.plugin` | Free + Premium | 12 nick colors, 8 gradients, animated gradient, 6 Premium effects |

Premium checks call `com.exteragram.messenger.roqi.RoqiPremium.isPremium()` in the app.
If the class is missing (older build), plugins degrade to free behavior.

GPLv3, like RoqiGram and exteraGram.
