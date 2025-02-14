[![Curse Forge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)](https://www.curseforge.com/minecraft/mc-mods/alfheim-lighting-engine)
[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/mod/alfheim-lighting-engine)

[![Buy Me a Coffee](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/buymeacoffee-singular_vector.svg)](https://www.buymeacoffee.com/desoroxxx)
[![Discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.gg/hKpUYx7VwS)

[![Java 8](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/java8_vector.svg)](https://adoptium.net/temurin/releases/?version=8)
[![Gradle](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/gradle_vector.svg)](https://gradle.org/)
[![Forge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/forge_vector.svg)](http://files.minecraftforge.net/maven/net/minecraftforge/forge/index_1.12.2.html)

# Alfheim

Alfheim is a fork *(or a rewrite depending on your views on the [ship of Theseus](https://en.wikipedia.org/wiki/Ship_of_Theseus))* of [Hesperus] which is a fork of [Phosphor].

## What benefit does this have over [Phosphor]/[Hesperus]?

Alfheim aims to be compatible in all scenarios, and if for any reasons something is fundamentally incompatible, a warning will tell you what is happening instead of crashing with hardly any information.

Alfheim also has a lot of optimizations of all sizes over [Phosphor]/[Hesperus].
For example the lighting engine deduplicates the lighting updates, meaning that if multiple updates are made to the same block, only one calculation is performed.

There is also client side optimizations, [Phosphor]/[Hesperus] unnecessarily forces lights to be updated before each frame which Alfheim doesn't.
On the same note, Alfheim also stop processing light updates when the game is paused.
The processing of light updates is also much faster by using the same high speed deduplicated queue as the lighting engine.

There is probably a lot more optimizations that I haven't listed here, but these are great examples.

Alfheim also fixes more vanilla lighting issues than [Phosphor]/[Hesperus], here is a list of all fixes and where they come from:

Fixed in [Phosphor]/[Hesperus]:
- [MC-3329](https://bugs.mojang.com/browse/MC-3329)
- [MC-102162](https://bugs.mojang.com/browse/MC-102162)
- [MC-116690](https://bugs.mojang.com/browse/MC-116690)
- [MC-117067](https://bugs.mojang.com/browse/MC-117067)
- [MC-117094](https://bugs.mojang.com/browse/MC-117094)

Fixed in Alfheim:
- [MC-92](https://bugs.mojang.com/browse/MC-92)
- [MC-50734](https://bugs.mojang.com/browse/MC-50734)
- [MC-80966](https://bugs.mojang.com/browse/MC-80966)
- [MC-95515](https://bugs.mojang.com/browse/MC-95515)
- [MC-104532](https://bugs.mojang.com/browse/MC-104532)
- [MC-249343](https://bugs.mojang.com/browse/MC-249343)

### Why not just PR to [Hesperus]? 

Fair question, the reason is simple, Alfheim compared to [Hesperus] isn't just a fork to fix bugs, it is my continuation of it.
And as seen in the past, my continuations of mods aren't just modifying some things, they are pretty invasive and basically change everything internally.

It wasn't this way at first, I wanted to PR some stuff to [Hesperus] but after doing parts of it, it became clear that it had grown bigger than a PR.
So then I decided to do Alfheim, as of now it focuses on optimizations, bug fixes and compatibility improvements, but I have more plans.

[Hesperus]: https://www.curseforge.com/minecraft/mc-mods/hesperus
[Phosphor]: https://www.curseforge.com/minecraft/mc-mods/phosphor-forge

---

[![BisectHostingPromoBanner](https://github.com/user-attachments/assets/8e66200c-1a7c-4f0a-a12a-387bf7d7f0f6)](https://bisecthosting.com/Desoroxxx?r=Alfheim+GitHub)

# Want to have your own mod or support me?

If you're looking for a mod but don't have the development skills or time, consider commissioning me!
My commissions are currently open and I would be happy to create a custom mod to fit your needs as long as you provide assets.

[Commissions]

You can also support me on a monthly basis by becoming a member.
To thank you will have the possibility to access exclusive post and messages, Discord channel for WIP content, and even access to unreleased Prototypes or WIP Projects.

[Membership]

You can also [buy me a hot chocolate].

[Commissions]: https://www.buymeacoffee.com/desoroxxx/commissions
[Membership]: https://www.buymeacoffee.com/desoroxxx/membership
[buy me a hot chocolate]: https://www.buymeacoffee.com/desoroxxx
