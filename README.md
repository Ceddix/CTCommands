> [!IMPORTANT]
> **In the future, this plugin will be integrated into the [CT-Utils](https://github.com/CraftTogetherMC/CT-Utils/) plugin.**

# CTCommands

CTCommands ist ein **Velocity Utility-Plugin** für CraftTogetherMC.

---

## 🚀 Hauptfunktionen

### 🔐 Command-Whitelist / Command-Blacklist

CTCommands kontrolliert die **Tab-Vervollständigung** von Commands:

* **Whitelist-basiertes Command-System**

  * Spieler können nur Commands sehen, die explizit erlaubt sind
  * Gruppenzuweisung erfolgt über Permissions (z. B. `ct.group.spieler`)
  * Eine Default-Gruppe ist immer aktiv

* **Blacklist-System**

  * Kritische oder unerwünschte Commands werden global versteckt
  * Beispiel: `worldedit`, `worldguard`, `multiverse-core`

* **Bypass-Permissions**

  * `ctcommands.bypass.whitelist`
  * `ctcommands.bypass.blacklist`
  * `ctcommands.bypass.all`

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

---

### 📢 Command: `/ctext`

Proxy-weites Messaging-System zum Versenden vordefinierter Texte aus Dateien.

**Syntax:**

```
/ctext <dateiname> [player|all]
```

**Funktionsweise:**

* Texte werden aus `<pluginDir>/ctext/<dateiname>.txt` geladen
* Unterstützt [MineDown](https://github.com/Phoenix616/MineDown)-Formattierung
* Platzhalter:

  * `%NAME%` → Spielername

---

### 📝 Logging

CTCommands legt eine strukturierte Log-Hierarchie im Plugin-Verzeichnis an:

* `logs/chat/` – Chat-Logs (vorbereitet)
* `logs/commands/` – Command-Logs

Blockierte Command-Ausführungen werden aktiv protokolliert.

---

### 🧩 Erweiterte Funktionen (implementiert, optional aktivierbar)

* Join-/Leave-Nachrichten (inkl. Silent-Join für Staff)
* Private Join Message oder CText
* First-Join-Erkennung
* Welcome-Text aus Textdateien
* Chat- und vollständiges Command-Logging
* LiteBans-Kompatibilitätsprüfung
