package edu.kit.ktane;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.awt.*;

/**
 * Created by maxis on 17.05.2017.
 * TODO: Log Ausgabe nur bei Events: - Modul entschärft, Strike erhalten, Bombe entschärft
 * TODO: Zeit, die pro Modul gebraucht wird (log wann Modul aktiviert wird)
 */
public class KtaneHandler {

    private String logFile = "logs/ktane.log";
    private String ktaneStarter = "D:/workspace/IISM/ktane_start.vbs";
    private String ktaneExeLocation = "C:/Program Files (x86)/Steam/steamapps/common/Keep Talking and Nobody Explodes/ktane.exe";
    private String[] ktaneFromSteam = {"C:/Program Files (x86)/Steam/Steam.exe", "-applaunch", "341800"};
    private String[] missionidslgbfpt1 = {"mod_lgbfpt1_1bomb1", "mod_lgbfpt1_1bomb2", "mod_lgbfpt1_1bomb3",
            "mod_lgbfpt1_1bomb4", "mod_lgbfpt1_1bomb5", "mod_lgbfpt1_1bomb6",
            "mod_lgbfpt1_1bomb7", "mod_lgbfpt1_1bomb8", "mod_lgbfpt1_1bomb9",
            "mod_lgbfpt1_1bomb10"};
    private String[] missionidslgbfpt2 = {"mod_lgbfpt2_2bomb1", "mod_lgbfpt2_2bomb2", "mod_lgbfpt2_2bomb3",
            "mod_lgbfpt2_2bomb4", "mod_lgbfpt2_2bomb5", "mod_lgbfpt2_2bomb6",
            "mod_lgbfpt2_2bomb7", "mod_lgbfpt2_2bomb8", "mod_lgbfpt2_2bomb9",
            "mod_lgbfpt2_2bomb10"};
    private String[] missionidsgfpt2 = {"mod_gfpt2_3bomb1", "mod_gfpt2_3bomb2", "mod_gfpt2_3bomb3",
            "mod_gfpt2_3bomb4", "mod_gfpt2_3bomb5", "mod_gfpt2_3bomb6",
            "mod_gfpt2_3bomb7", "mod_gfpt2_3bomb8", "mod_gfpt2_3bomb9",
            "mod_gfpt2_3bomb10", "mod_gfpt2_3bomb11", "mod_gfpt2_3bomb12"};
    private String[] seeds = {"11779", "13718", "49060", "59085", "27392", "1120", "59549", "44791", "643", "41167", "76218", "6863"};

    private String runningGame;
    private boolean isRoundRunning = false;
    public boolean experiment;
    private int numberOfRounds = 0;

    // Some data storage variables.
    private LocalDateTime gameRoundInitiationTimestamp;
    private Timestamp latestTs = new Timestamp(new Date().getTime());
//    private LinkedList<GameData> gameDataEvents = new LinkedList();
    private ArrayList<String> gameEventList;
    private String solvableModules, modules, solvedModules;
    private String timeLeft, bombState;
    private long strikes;
    private String tmpSolved= "";
    private long tmpStrikes = 0;

    WindowHandler windowHandler;
    KtaneJsonHandler ktjshandler;
    Process ktaneProcess;

    public KtaneHandler(LocalDateTime experimentInitiationTimestamp, LocalDateTime gameRoundInitiationTimestamp) {

        // Time the game was started in brownie (this is not the TS for when users start defusing).
        this.gameRoundInitiationTimestamp = gameRoundInitiationTimestamp;

        // Prepare the file writer
        GameData.prepareGameDataFile(experimentInitiationTimestamp);

        // Start the game and initialize the JSON game parser.
        initializeJSONHandler();
        // TODO: The game should be running already.
        runningGame = startGame(ktaneFromSteam);
//        System.out.println("Starting Game from " + Arrays.toString(ktaneFromSteam));
        windowHandler = new WindowHandler();
        schlafen(10000);    // TODO: Not sure if needed...
        try {
            Robot robot = new Robot();
            robot.mouseMove(300, 300);
            robot.mousePress( InputEvent.BUTTON1_MASK );
            robot.mouseRelease( InputEvent.BUTTON1_MASK );
            // System.out.println("Mouse press bei 300, 300");
        } catch (AWTException e) {
            System.out.println("Mouse click failed!");
        }
        schlafen(1500);
        windowHandler.toBackground(false);
        try {
            Robot robot = new Robot();
            robot.mouseMove(750, 820
            );
            robot.mousePress( InputEvent.BUTTON1_MASK );
            robot.mouseRelease( InputEvent.BUTTON1_MASK );
            // System.out.println("Mouse press bei 589, 668");
        } catch (AWTException e) {
            System.out.println("Mouse click failed!");
        }
        schlafen(500);

    }

    /**
     * Takes number of games which should be played and executes from 1 to numberOfGames until finished
     * @param numberOfGames Number of games which should be played
     */
    public void goStandalone(int numberOfGames) {
        while (numberOfGames > numberOfRounds) {
            System.out.println("Start game Nr. " + numberOfRounds + " of " + numberOfGames);
            go(numberOfRounds);
            numberOfRounds++;
        }

        System.out.println("Experiment zu Ende!");
    }

    /**
     * Takes one bomb-id (from 1-10/12) and executes this one only
     * @param gameId Which predefined bomb should be played
     */
    public Boolean go(int gameId) {
        System.out.println("Aktuellster TS: " + latestTs);

        // Set flag that round is starting.
        isRoundRunning = true;

        // Move window to the foreground
        windowHandler.toBackground(false);
        System.out.println("Moving to Foreground");
        schlafen(2000);     // Short time to give the user time to react to the window now being in the foreground.

        // Start the mission
        while (!startMission(gameId)) {
            schlafen(200);
            System.out.println("Konnte mission nicht starten.");
        }
        schlafen(5000);     // Short break until mission has started.

        // Analyze bomb state continuously
        while (bombState == null) {
            parseJson(ktjshandler.fetchBombInfos());
            schlafen(200);      // TODO: Consider removing this, if events are not parsed directly from log.
            System.out.println("Mission noch nicht fertig geladen.");
        }
        
        // TODO: What happens if bomb is solved? Does the loop end then too?
        while (!(bombState.equals("Exploded"))) {
            parseJson(ktjshandler.fetchBombInfos());
            schlafen(500);
        }
        
        // Letztes Mal ausführen, um alles zu loggen
        parseJson(ktjshandler.fetchBombInfos());
        schlafen(1000);
        System.out.println("Runde zu ende");
        gameEventList.add("Bomb exploded with " + timeLeft + " seconds remaining");
        System.out.println(gameEventList.get(gameEventList.size()-1));
        // TODO: Write game data to file
        GameData.writeGameDataToCSVFromMaxisLog(gameEventList);

        bombState = null;
        System.out.println("Waiting for Process: " + runningGame);
        schlafen(3000);

        // ktaneProcess.destroyForcibly();
        // stopGame();

        // Move window to the background
        windowHandler.toBackground(true);
        System.out.println("Moving to Background");

        // Set flag that round is over.
        isRoundRunning = false;

        // Reset game metrics.
        strikes = 0;
        timeLeft = bombState = runningGame = solvableModules = modules = solvedModules = null;

        return isRoundRunning;
    }

    public void logDiffs() {
        try {
            String diff = StringUtils.difference(tmpSolved, solvedModules.replace("\"", "").replace("[", "").replace("]", ""));
            if(!diff.equals("")) {
                gameEventList.add("Solved Module " + diff + " with " + timeLeft + " seconds remaining");
                System.out.println(gameEventList.get(gameEventList.size()-1));
                tmpSolved = solvedModules.replace("\"", "").replace("[", "").replace("]", "");
            }

            if(tmpStrikes != strikes) {
                gameEventList.add("Got strike nr " + strikes + " with " + timeLeft + " seconds remaining");
                System.out.println(gameEventList.get(gameEventList.size()-1));
                tmpStrikes = strikes;
            }

            // System.out.println(Arrays.toString(gameEventList.toArray()));
        }
        catch(NullPointerException npe) {
            System.out.println("NullPointer in logDiffs: " + npe);
        }
    }

    public void parseJson(JSONObject jobj) {
        try {
            solvableModules = ((JSONArray) jobj.get("SolvableModules")).toString(); // .replace("\"", "").replace("[", "").replace("]", "").split(",");
            modules = ((JSONArray) jobj.get("Modules")).toString(); //.replace("\"", "").replace("[", "").replace("]", "").split(",");
            solvedModules = ((JSONArray) jobj.get("SolvedModules")).toString(); //.replace("\"", "").replace("[", "").replace("]", "").split(",");
            timeLeft = (String) jobj.get("Time");
            strikes = ((Long) jobj.get("Strikes"));
            bombState = (String) jobj.get("BombState");

            logDiffs();

            // printBombStatus();
        } catch (NullPointerException npe) {
            System.out.println("Cannot parse Json Object, is the game running?");
            System.out.println(npe.getLocalizedMessage());
        }
    }

    public void printBombStatus() {
        System.out.println("sm: " + solvableModules + " " + "sdm: " + solvedModules + " " +
                "all: " + modules + " " + "t: " +
                timeLeft + " " + "st: " + strikes +
                " " + "s: " + bombState);
    }

    public boolean schlafen(long mil) {
        try {
            Thread.sleep(mil);
            return true;
        } catch (InterruptedException ie) {
            System.out.println("Sleep Fehler");
            return false;
        }
    }

    public void initializeJSONHandler() {
        ktjshandler = new KtaneJsonHandler(8085, "http://localhost:8085/");
        gameEventList = new ArrayList<>();
    }

    public boolean stopGame(String pid) {
        String cmd = "taskkill /F /PID " + pid;
        try {
            Runtime.getRuntime().exec(cmd);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public String startGame(String[] ktaneLocation) {
        try {
            // Runtime.getRuntime().exec(ktaneLocation);
            ktaneProcess = new ProcessBuilder(ktaneLocation).start();

            System.out.println("Process started");
            boolean started = false;
            String pid = "";
            while (!started) {
                try {
                    String line;
                    Process p = Runtime.getRuntime().exec
                            (System.getenv("windir") + "\\system32\\" + "tasklist.exe");
                    BufferedReader input =
                            new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while ((line = input.readLine()) != null) {
                        if (line.contains("ktane.exe")) {
                            String[] tmparray = line.split(" ");
                            System.out.println(Arrays.toString(tmparray));
                            ArrayList<String> regline = new ArrayList<>();
                            for (String s : tmparray) {
                                if (s.length() > 1) {
                                    regline.add(s.replace(" ", ""));
                                }
                            }
                            System.out.println("Anzahl Elemente: " + regline.size());
                            System.out.println(Arrays.toString(regline.toArray()));
                            if (regline.size() > 2) {
                                System.out.println(regline.get(1));
                                pid = regline.get(1);
                            }
                            started = true;
                            windowHandler = new WindowHandler();
                            windowHandler.resizeWindow();
                            break;
                        }
                    }
                    input.close();
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }

            return pid;
        } catch (Exception e) {
            e.printStackTrace();
            schlafen(200);
            startGame(ktaneLocation);
            return null;
        }
    }

    public boolean startMission(int missionId) {
        boolean state = ktjshandler.startMission(missionidsgfpt2[missionId], "" + seeds[missionId]);
        return state;
    }

    /**
     * Check if the Ktane game is running in the background.
     * @return
     */
    public boolean isGameStarted() {
        return ktaneProcess.isAlive();
    }

    /**
     * Check if a bomb round is running in the background.
     * @return
     */
    public boolean isRoundRunning() {
        return isRoundRunning;
    }

    //    /**
//     * @return
//     */
//    public boolean processFile() {
//
//        try {
//
//            /*File source = new File(logFile);
//            File dest = new File(logFile+"tmp");
//            try {
//                Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }*/
//
//            List<String> tmp = new ArrayList<>();
//            FileReader fr = new FileReader(logFile);
//            BufferedReader br = new BufferedReader(fr);
//            String ch;
//
//            while ((ch = br.readLine()) != null) {
//                // Throw out non-info or non-debug (such as tables, html code, etc)
//                if (ch.startsWith("DEBUG") || ch.startsWith(" INFO")) {
//                    ch = ch.replace("DEBUG ", "");
//                    ch = ch.replace(" INFO ", "");
//                    ch = ch.replaceFirst(",", ".");
//                    tmp.add(ch);
//                    // System.out.println(ch);
//                }
//            }
//            br.close();
//            // System.out.println("Checking " + tmp.size() + " Log entries");
//            int counter = 0;
//            boolean abbruch = false;
//            outerloop:
//            for (int i = tmp.size() - 1; i >= 0; i--) {
//                // Prüfung durchführen, solange die Daten aktueller als die letze Prüfung sind
//                // System.out.println("Gelesener TS: " + tmp.get(i).substring(0, 23));
//                long duration = (2 * 60 * 60 * 1000);
//                Timestamp ts = Timestamp.valueOf(tmp.get(i).substring(0, 23));
//                ts.setTime(ts.getTime() + duration);
//                // System.out.println("Vergleichender TS: " + ts);
//                if (ts.equals(latestTs) || ts.before(latestTs)) {
//                    // System.out.println("Hier waren wir schon!");
//                    Timestamp tsmid = Timestamp.valueOf(tmp.get(tmp.size() - 1).substring(0, 23));
//                    tsmid.setTime(tsmid.getTime() + duration);
//                    latestTs = tsmid;
//                    // System.out.println("Neuer latest TS: " + latestTs);
//                    abbruch = true;
//                    break outerloop;
//                } else {
//                    counter++;
//                    // Wenn OnRoundEnd() in der Log-Datei steht wird true zurück gegeben, da Spiel vorbei
//                    if (tmp.get(i).contains("OnRoundEnd()")) {
//                        System.out.println("Runde zu Ende!");
//                        tmp.clear();
//                        return true;
//                    }
//                }
//            }
//            if (!abbruch) {
//                long duration = (2 * 60 * 60 * 1000);
//                Timestamp tsend = Timestamp.valueOf(tmp.get(tmp.size() - 1).substring(0, 23));
//                tsend.setTime(tsend.getTime() + duration);
//
//                latestTs = tsend;
//                System.out.println("Neuer latest TS: " + latestTs);
//            }
//            if (counter > 0) {
//                System.out.println(counter + " Einträge neu gelesen");
//            }
//        } catch (FileNotFoundException fnfe) {
//            System.out.println(logFile);
//            System.out.println("Dateipfad falsch!");
//            return false;
//        } catch (IOException ioe) {
//            System.out.println("IO-Fehler!");
//            return false;
//        }
//        return false;
//    }
}
