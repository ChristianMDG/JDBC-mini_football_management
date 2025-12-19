package org.example;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws SQLException {
        DataRetriever dataRetriever = new DataRetriever();
        System.out.println(dataRetriever.findTeamById(1));
        System.out.println(dataRetriever.findTeamById(5));
        System.out.println(dataRetriever.findPlayers(1,2));
        System.out.println(dataRetriever.findPlayers(3,5));
        System.out.println(dataRetriever.findTeamsByPlayerName("an"));
        System.out.println(dataRetriever.findPlayersByCriteria("ud",PlayerPositionEnum.MIDF,"Madrid",ContinentEnum.EUROPA,1,10));

    }
}