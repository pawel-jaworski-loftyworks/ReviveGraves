# ReviveGraves — Lessons Learned

## 2026-03-24 — Initial Claude Setup
- Repo hat mehrere MC-Version-Branches (1.21.1, 1.21.5, 1.21.6, 1.21.9) — bei Aenderungen immer pruefen welcher Branch aktiv ist
- Fabric Loom nutzt Yarn Mappings — bei API-Aenderungen zwischen MC-Versionen die Mapping-Differenzen beachten
- Log-Datei war versehentlich im Source-Tree committed — generierte Dateien gehoeren in .gitignore

## 2026-03-24 — v1.0.1 Code Quality + Security
- Nicht pro Iteration commiten — erst alle Aenderungen sammeln, Security Audit, dann ein Commit mit Versionsbump
- Immer erst Minecraft starten + manuell testen bevor committed wird
- Template-Ueberbleibsel (ExampleMixin, pink_garnet_blocks ID, placeholder Texte) frueh entfernen
- Gravestone darf kein BlockItem haben — verhindert Grief durch platzierbare unzerstoerbare Bloecke
- NBT-Parsing immer crash-safe mit try-catch — verhindert Chunk-Corruption bei defekten Saves

## 2026-03-24 — v1.1.0 Gravestone Redesign
- Blockbench JSON Modelle: UV-Koordinaten exakt analysieren bevor Texturen gemalt werden (UV-Units != Pixel)
- Z-Fighting bei Block-Modellen: Keine zwei Faces auf derselben Ebene im selben Bereich. Ueberlappende up/down/east/west Faces zwischen Elementen entfernen
- SkullBlockEntityRenderer.render() mit direction=null macht intern translate(0.5,0,0.5) + scale(-1,-1,1) — Offset-Kompensation noetig
- Der scale(-1,-1,1) Spiegel invertiert die effektive Yaw-Richtung — yRot-Werte umkehren
- Skull-Position knapp VOR der Backwall platzieren (nicht genau drauf) um Z-Fighting zu vermeiden
- ownerName im BlockEntity validieren (max 16 chars) und toInitialChunkDataNbt nur noetige Felder senden
- Texture-Moos: Als zusammenhaengende Cluster an Fugen/Uebergaengen platzieren, nicht als isolierte Einzelpixel
- .claude/ Verzeichnis gehoert in .gitignore — keine lokalen Konfigurationsdateien committen
