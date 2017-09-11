package edu.kit.ktane;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by michaelknierim on 18.07.17.
 */
public class GameData {
    private String[] gameDataString = new String[6];
    // static DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss-SSS");
    private static final String fileSeparator = File.separator;
    private static final String CSV_SEPARATOR = ",";
    private static String dataLogFile;

    /**
     * List of game events that should be logged.
     */
    public enum GameEvent {
        ROUND_START,
        ROUND_END,
        MODULE_SELECTED,
        MODULE_SOLVED,
        STRIKE,
        BOMB_SOLVED,
        BOMB_EXPLODED
    }

    /**
     * List of all modules that can currently be active.
     */
    public enum ActiveModule {
        NONE,
        WIRES_VERKABELUNGEN,
        BUTTONS_KNOEPFE,
        KEYPADS,
        SENSO,
        WHOS_ON_FIRST,
        REMEMBER_ERINNERN
    }

    /**
     * This constructs an instance of a game date event (e.g. a strike happing at a point in time).
     */
    public GameData(Long gameRoundInitiationTimestamp, Long gameRoundStartTimestamp, Long
            eventTimestamp, String bombID, ActiveModule activeModule, GameEvent gameEvent) {

        gameDataString[0] = gameRoundInitiationTimestamp.toString();
        gameDataString[1] = gameRoundStartTimestamp.toString();
        gameDataString[2] = eventTimestamp.toString();
        gameDataString[3] = bombID;
        gameDataString[4] = activeModule.name();
        gameDataString[5] = gameEvent.name();
    }

    public String[] getGameDataString() {
        return gameDataString;
    }

    /**
     * Concatenate all values in the String Array to a string to store in CSV.
     *
     * @return
     */
    private static String createDataString(String[] splitLine) {
        String dataString = "";

        // Loop through array and add all entries to string
        for (String element : splitLine) {
            dataString = dataString + element + CSV_SEPARATOR;
        }

        return dataString;
    }

    /**
     * ...
     */
    public static void prepareGameDataFile(Long sessionInitiationTime) {
        dataLogFile = "data/game" + fileSeparator + sessionInitiationTime.toString() + "_expgamedata_log.csv";

        // Create subfolder if necessary
        File targetFile = new File(dataLogFile);
        File parent = targetFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Couldn't create dir: " + parent);
        }
    }

    /**
     * ...
     *
     * @param gameDataList
     */
    public static void writeGameDataToCSV(List<GameData> gameDataList) {
        try {
            // Write to file
            try (BufferedWriter bw = new BufferedWriter((new FileWriter(new File(dataLogFile), true)))) {
                for (GameData gameDate : gameDataList) {
                    bw.write(createDataString(gameDate.getGameDataString()));
                    bw.newLine();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ...
     *
     * @param gameDataList
     */
    public static void writeGameDataToFile(ArrayList<String> gameDataList, String variant, Long sessionInitiationTime) {
        try {
            // Write to file
            String filename = "data/game/" + sessionInitiationTime.toString() + variant + ".csv";
            try (BufferedWriter bw = new BufferedWriter((new FileWriter(new File(filename), true)))) {
                for (String gameDate : gameDataList) {
                    bw.write(gameDate);
                    bw.newLine();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
