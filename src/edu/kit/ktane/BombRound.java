package edu.kit.ktane;

/**
 * Created by maxis on 15.05.2017.
 */
public class BombRound {

    private int number, strikes;
    private double duration;

    public BombRound(int nr, long dur) {
        number = nr;
        strikes = 0;
        duration = dur;
    }
}
