# CTCommands

CTCommands ist ein leistungsorientiertes **Velocity-Proxy-Plugin** zur **zentralen Steuerung, Absicherung und Verwaltung von Commands** in einem Minecraft-Netzwerk. Das Plugin agiert als **Governance- und Kontrollschicht auf Proxy-Ebene** und stellt sicher, dass Spieler ausschließlich die vorgesehenen Befehle sehen und ausführen können.

Der Fokus liegt auf **Command-Sicherheit, zentraler Administration, konsistenter Kommunikation** sowie einer klar strukturierten, wartbaren Architektur.

---

## 🎯 Zielsetzung

CTCommands adressiert typische Herausforderungen größerer Minecraft-Netzwerke:

* Zentrale Kontrolle über **sichtbare und ausführbare Commands**
* Vermeidung von Command-Missbrauch und Informationslecks
* Einheitliche Proxy-weite Kommunikation
* Saubere Trennung zwischen Proxy-Logik und Backend-Servern

---

## 🚀 Hauptfunktionen

### 🔐 Command-Governance (Whitelist / Blacklist)

CTCommands kontrolliert sowohl die **Ausführung** als auch die **Tab-Vervollständigung** von Commands:

* **Whitelist-basiertes Command-System**

  * Spieler dürfen nur Commands ausführen, die explizit erlaubt sind
  * Gruppenzuweisung erfolgt über Permissions (z. B. `ct.group.spieler`)
  * Eine Default-Gruppe ist immer aktiv

* **Blacklist-System**

  * Kritische oder unerwünschte Commands werden global blockiert
  * Beispiel: `worldedit`, `worldguard`, `multiverse-core`

* **Bypass-Permissions**

  * `ctcommands.bypass.whitelist`
  * `ctcommands.bypass.blacklist`
  * `ctcommands.bypass.all`

* Blockierte Command-Versuche werden **protokolliert**

---

### 🧾 Command: `/ctcommands`

Administrativer Service-Command für Betrieb und Wartung.

**Subcommands:**

* `/ctcommands reload`

  * Lädt alle Konfigurationsdateien neu
  * Kein Proxy-Neustart erforderlich
  * Permission: `ctcommands.reload`

* `/ctcommands version`

  * Zeigt Plugin-Version und Autoren an
  * Permission: `ctcommands.version`

Tab-Completion berücksichtigt ausschließlich Subcommands, für die der Spieler berechtigt ist.

---

### 📢 Command: `/ctext`

Proxy-weites Messaging-System zum Versenden vordefinierter Texte aus Dateien.

**Syntax:**

```
/ctext <dateiname> [player|all]
```

**Funktionsweise:**

* Texte werden aus `<pluginDir>/ctext/<dateiname>.txt` geladen
* Unterstützung für:

  * MiniMessage (`<gradient>`, `<bold>`, etc.)
  * Legacy-Farbcodes (`&a`, `&l`, ...)
* Platzhalter:

  * `%NAME%` → Spielername

**Zieloptionen:**

* kein Ziel → an den ausführenden Spieler
* `all` → an alle Online-Spieler
* `<player>` → an einen spezifischen Online-Spieler

Tab-Completion schlägt automatisch verfügbare Textdateien und Online-Spieler vor.

---

### 🗂️ Konfigurationsdateien

Beim ersten Start werden folgende YAML-Dateien automatisch erstellt:

* `whitelist.yml` – erlaubte Commands pro Gruppe
* `blacklist.yml` – global blockierte Commands
* `joinmessages.yml` – Join-/Leave-Nachrichten
* `uuids.yml` – Tracking bekannter Spieler-UUIDs

Alle Konfigurationen können über `/ctcommands reload` zur Laufzeit neu geladen werden.

---

### 📝 Logging

CTCommands legt eine strukturierte Log-Hierarchie im Plugin-Verzeichnis an:

* `logs/chat/` – Chat-Logs (vorbereitet)
* `logs/commands/` – Command-Logs

Blockierte Command-Ausführungen werden aktiv protokolliert.

---

### 🧩 Erweiterte Funktionen (implementiert, optional aktivierbar)

Im Projekt enthalten, derzeit jedoch nicht standardmäßig registriert:

* Join-/Leave-Nachrichten (inkl. Silent-Join für Staff)
* First-Join-Erkennung
* Welcome-Text aus Textdateien
* Chat- und vollständiges Command-Logging
* LiteBans-Kompatibilitätsprüfung

Diese Funktionen können durch einfache Listener-Registrierung aktiviert werden.

---

## ⚙️ Technische Details

* Plattform: **Velocity Proxy**
* Java-Version: **17** (empfohlen)
* Build-Tool: **Maven**
* Konfiguration: **YAML (Configurate / SnakeYAML)**
* Messaging: **MiniMessage & Legacy Color Codes**

---

## 📦 Build

```bash
mvn clean package
```

Die fertige JAR befindet sich anschließend unter:

```
target/CTCommands-<version>.jar
```

Das Projekt nutzt den `maven-shade-plugin` zur kontrollierten Bündelung von Abhängigkeiten.

---

## 📄 Lizenz

MIT License

---

## 🤝 Mitwirkung & Support

Pull Requests, Code-Reviews und strukturelle Verbesserungen sind willkommen.

CTCommands versteht sich als **zentrale Steuerungskomponente** für professionelle Velocity-Netzwerke.

---

**CTCommands** – Kontrolle, Struktur und Sicherheit auf Proxy-Ebene.
