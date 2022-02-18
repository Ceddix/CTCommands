package de.crafttogether.ctcommands.events;

import net.md_5.bungee.api.plugin.Plugin;

import java.io.*;
import java.sql.Array;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LogFile {
    private Plugin plugin;

    private File file;
    private FileWriter fileWriter;

    private String filePath;
    private String fileName;
    private String fileDate;

    public LogFile (Plugin plugin, String path) {
        this.plugin = plugin;

        File dir = new File(path);
        if (!dir.exists()) dir.mkdir();

        filePath = path;
        createFile();
    }

    private void createFile() {
        fileDate = getDate();
        fileName = getLogFileName(filePath);

        file = new File(filePath + File.separator + fileName);

        if (file.exists())
            plugin.getLogger().warning("File '" + file.getPath() + File.separator + file.getName()  + "' already exists.");

        else {
            try {
                file.createNewFile();
                fileWriter = new FileWriter(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(String text) {
        if (!getDate().equals(fileDate)) {
            plugin.getLogger().info("Log Rotation...");
            close();
            createFile();
        }

        if (fileWriter != null) {
            try {
                String date = "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] ";
                fileWriter.append(date + text + "\r\n");
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            fileWriter = null;
        }
    }

    private String getDate() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    private String getLogFileName(String directory) {
        File folder = new File(directory);
        List<File> files = Arrays.asList(folder.listFiles());

        String date = getDate();
        List<File> found = files.stream().filter(file -> file.getName().startsWith(date)).sorted().collect(Collectors.toList());

        if (found.size() == 0)
            return date + "-1.log";

        else {
            Matcher m = Pattern.compile(date + "-(\\d+).log").matcher(found.get(found.size() - 1).getName());

            if (!m.matches()) {
                System.out.println("ERROR: Can't Match last File!");
                return null;
            }

            System.out.println("Last-File-ID => " + (Integer.parseInt(m.group(1)) + 1));
            return date + "-" + (Integer.parseInt(m.group(1)) + 1) + ".log";
        }
    }
}
