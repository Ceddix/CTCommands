package de.crafttogether.ctcommands.events;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tägliche Logrotation: schreibt in <yyyy-MM-dd>-N.log (N hochzählend).
 * Velocity-ready: verwendet SLF4J-Logger statt Bungee-Plugin.
 */
public class LogFile {
    private final Logger logger;

    private File file;
    private FileWriter fileWriter;

    private String filePath;
    private String fileName;
    private String fileDate;

    public LogFile(Logger logger, String path) {
        this.logger = logger;

        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            // wenn der Ordner nicht erstellt werden kann, trotzdem weiter versuchen – aber warnen
            this.logger.warn("Konnte Log-Ordner nicht erstellen: {}", path);
        }

        this.filePath = path;
        createFile(); // initial öffnet Writer
    }

    private void createFile() {
        this.fileDate = getDate();
        this.fileName = getLogFileName(this.filePath);
        this.file = new File(this.filePath + File.separator + this.fileName);

        try {
            if (!this.file.exists() && !this.file.createNewFile()) {
                logger.warn("Konnte Logdatei nicht anlegen: {}", this.file.getAbsolutePath());
            }

            // IMMER mit append=true öffnen (auch falls Datei schon existiert)
            this.fileWriter = new FileWriter(this.file, true);
        } catch (IOException e) {
            logger.error("Fehler beim Erstellen/Öffnen der Logdatei {}", this.file.getAbsolutePath(), e);
            this.fileWriter = null;
        }
    }

    /** schreibt eine Zeile ins Log; rotiert automatisch bei Tageswechsel */
    public synchronized void write(String text) {
        // Tageswechsel -> rotieren
        if (!getDate().equals(this.fileDate)) {
            logger.info("Log Rotation...");
            close();
            createFile();
        }

        if (this.fileWriter != null) {
            try {
                String ts = "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] ";
                this.fileWriter.append(ts).append(text).append(System.lineSeparator());
                this.fileWriter.flush();
            } catch (IOException e) {
                logger.error("Fehler beim Schreiben in {}", this.file.getAbsolutePath(), e);
            }
        } else {
            logger.warn("LogWriter ist null – Eintrag wurde verworfen: {}", text);
        }
    }

    public synchronized void close() {
        if (this.fileWriter != null) {
            try {
                this.fileWriter.close();
            } catch (IOException e) {
                logger.error("Fehler beim Schließen der Logdatei {}", this.file.getAbsolutePath(), e);
            } finally {
                this.fileWriter = null;
            }
        }
    }

    private String getDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    /**
     * Sucht die höchste heutige Datei-ID und gibt den nächsten Namen zurück.
     * Beispiel: 2025-08-15-1.log, 2025-08-15-2.log, ...
     */
    private String getLogFileName(String directory) {
        File folder = new File(directory);
        File[] listed = folder.listFiles();
        if (listed == null || listed.length == 0) {
            return getDate() + "-1.log";
        }

        List<File> files = Arrays.asList(Objects.requireNonNull(listed));
        String date = getDate();

        List<File> found = files.stream()
                .map(File::getName)
                .filter(name -> name.startsWith(date + "-") && name.endsWith(".log"))
                .sorted()
                .map(n -> new File(folder, n))
                .collect(Collectors.toList());

        if (found.isEmpty()) {
            return date + "-1.log";
        }

        String lastName = found.get(found.size() - 1).getName();
        Matcher m = Pattern.compile(Pattern.quote(date) + "-(\\d+)\\.log").matcher(lastName);

        if (!m.matches()) {
            logger.error("Unerwarteter Dateiname: {} – starte bei -1 neu", lastName);
            return date + "-1.log";
        }

        int next = Integer.parseInt(m.group(1)) + 1;
        return date + "-" + next + ".log";
    }
}
