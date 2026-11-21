# InvSee Summary – Minecraft 26.1.x und 26.2

Fabric-Client-Mod: zeigt beim `/invsee` (auch `/openinv`, `/inv`, `/isee` …) eine Zusammenfassung ausgewählter Items
(Totems, Kristalle, verzauberte Gapples …) und die Rüstung samt Haltbarkeit und Protection-Art. Optional als HUD
außerhalb des Inventars, frei verschiebbar (im Chat ziehen), Taste zum Wiederholen des letzten `/invsee`.

Ursprünglich von **Noah** (Version 1.0.0 für 1.21.11, MIT-Lizenz). Diese Fassung ist ein 1:1-Port auf Minecraft 26.1.x und 26.2
(unverschleierte Namen, neue Zeichen-API `GuiGraphicsExtractor`).

Bauen (Java 25): `./gradlew build -Pmc=26.1.2` → `build/libs/invsee-summary-1.0.0+26.1.2.jar` (je Version ein eigenes Jar: 26.1, 26.1.1, 26.1.2, 26.2)

Fertige Jars: unter *Actions* beim letzten Lauf als Artefakt, bei Versionen (Tag `vX.Y.Z`) als Release.

Lizenz: MIT (siehe `LICENSE`).
