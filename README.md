# CTCommands

CTCommands ist ein schlankes, performantes Command-Framework für Minecraft-Proxys auf Basis von **Velocity**. Das Projekt wurde mit dem Ziel entwickelt, eine saubere, wartbare und zukunftssichere Grundlage für serverübergreifende Commands bereitzustellen.

Der Fokus liegt auf klarer Architektur, einfacher Erweiterbarkeit und stabiler Integration in bestehende Velocity-Setups.

---

## 🚀 Features

* Native Unterstützung für **Velocity**
* Zentrale Verwaltung von Proxy-Commands
* Saubere Trennung von Command-Logik und Infrastruktur
* Erweiterbar und modular aufgebaut
* Maven-basiertes Build-Setup
* Einsatz von bewährten Libraries (z. B. Configurate, SnakeYAML)

---

## 📦 Installation

1. Lade die aktuelle JAR-Datei aus den **Releases** herunter.
2. Kopiere die JAR in das `plugins/`-Verzeichnis deines Velocity-Proxys.
3. Starte den Proxy neu.

Nach dem ersten Start werden ggf. benötigte Konfigurationsdateien automatisch generiert.

---

## ⚙️ Build (Maven)

Das Projekt verwendet **Maven** und kann lokal wie folgt gebaut werden:

```bash
mvn clean package
```

Das fertige Plugin befindet sich anschließend im Verzeichnis:

```
target/CTCommands-<version>.jar
```

> Hinweis: Das Projekt nutzt den `maven-shade-plugin`, um Abhängigkeiten kontrolliert zu bündeln.

---

## 🧩 Abhängigkeiten

CTCommands setzt u. a. auf folgende Libraries:

* Velocity API
* Configurate (Core & YAML)
* SnakeYAML

Alle Abhängigkeiten werden über Maven verwaltet.

---

## 🛠️ Entwicklung

* Java-Version: **17** (empfohlen)
* Build-Tool: **Maven**
* Zielplattform: **Velocity Proxy**

Pull Requests, Code-Reviews und strukturelle Verbesserungen sind ausdrücklich willkommen.

---

## 📄 Lizenz

Dieses Projekt steht unter der **MIT License**.

Weitere Details findest du in der Datei `LICENSE`.

---

## 📬 Support & Mitwirkung

Bei Fragen, Verbesserungsvorschlägen oder Issues bitte die GitHub-Issue-Funktion nutzen.

Gemeinsam schaffen wir eine robuste und nachhaltige Command-Lösung für Velocity.

---

**CTCommands** – strukturiert, performant, zukunftssicher.
