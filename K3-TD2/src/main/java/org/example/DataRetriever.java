package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataRetriever {
    DBConnection dbConnection =  new DBConnection();
    public Team findTeamById(Integer id) throws SQLException {

        Team team = null;
        List<Player> players = new ArrayList<>();

        String findTeamByIdQuery = """
        select team.id as team_id, team.name as team_name,team.continent as continent,player.id as player_id, player.name as player_name,player.age as age,player.position as position  from team
        left join player on team.id = player.id_team
        where team.id = ?;
        """;

        try (Connection connection = dbConnection.getDBConnection();
                PreparedStatement statement = connection.prepareStatement(findTeamByIdQuery)) {

            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                if (team == null) {
                    team = new Team();
                    team.setId(rs.getInt("team_id"));
                    team.setName(rs.getString("team_name"));
                    team.setContinent(
                    ContinentEnum.valueOf(rs.getString("continent"))
                    );
                    team.setPlayers(players);
                }
                if (rs.getInt("player_id") != 0) {
                    Player player = new Player();
                    player.setId(rs.getInt("player_id"));
                    player.setName(rs.getString("player_name"));
                    player.setAge(rs.getInt("age"));
                    player.setPosition(PlayerPositionEnum.valueOf(rs.getString("position")));
                    player.setTeam(team);
                    players.add(player);
                }
            }
        }
        return team;
    }

    List<Player> findPlayers(int page, int size) throws SQLException {
        List<Player> players = new ArrayList<>();
        Team team = new Team();
        int offset = (page - 1) * size;
        String findPlayerQuery = """
select player.id as player_id, player.name as player_name, player.age as age, player.position as position, team.name as team from player
            left join team on player.id_team = team.id
            limit ? offset ?
""";
        try (Connection connection = dbConnection.getDBConnection();
        PreparedStatement statement = connection.prepareStatement(findPlayerQuery))
            {
            statement.setInt(1, size);
            statement.setInt(2, offset);
            ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    if (rs.getInt("player_id") != 0) {
                        Player player = new Player();
                        player.setId(rs.getInt("player_id"));
                        player.setName(rs.getString("player_name"));
                        player.setAge(rs.getInt("age"));
                        player.setPosition(PlayerPositionEnum.valueOf(rs.getString("position")));
                        team.setName(rs.getString("team"));
                        player.setTeam(team);
                        players.add(player);
                    }
                }

            }
        return players;
    }

    public List<Player> createPlayers(List<Player> newPlayers) {
        String insertQuery = """
           insert into player (id,name, age, position)
           values (?, ?, ?, ?)
       """;

        try (Connection connection = dbConnection.getDBConnection()) {
            connection.setAutoCommit(false);
            Set<String> names = new HashSet<>();
            for (Player player : newPlayers) {
                if (!names.add(player.getName())) {
                    throw new RuntimeException(
                            "Le joueur " + player.getName() + " est dupliqué dans la liste"
                    );
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                for (Player player : newPlayers) {
                    statement.setInt(1, player.getId());
                    statement.setString(2, player.getName());
                    statement.setInt(3, player.getAge());
                    statement.setString(4, player.getPosition().name());
                    statement.executeUpdate();
                }
            }
            connection.commit();
            System.out.println("success");
            return newPlayers;
        } catch (Exception e) {
            throw new RuntimeException("Transaction annulée : aucun joueur enregistré", e);
        }
    }

    Team saveTeam(Team teamToSave) throws SQLException {
        throw new RuntimeException();
    }

    List<Team> findTeamsByPlayerName(String playerName)  throws SQLException {

       List<Team> teams = new ArrayList<>();
        Player player = new Player();
       StringBuilder findTeamsByPlayerNameQuery = new StringBuilder("""
               select team.id as team_id, team.name as team_name, team.continent as continent ,player.name as player_name from  team
               left join player on player.id_team = team.id
               where 1 = 1
               """);
        List<Object> parameters = new ArrayList<>();

        if (playerName != null) {
          findTeamsByPlayerNameQuery.append("and player.name ilike ? ");
            parameters.add("%" + playerName + "%");
        }
       try (Connection connection = dbConnection.getDBConnection();
       PreparedStatement statement = connection.prepareStatement(findTeamsByPlayerNameQuery.toString())) {
           for (int i = 0; i < parameters.size(); i++) {
               statement.setObject(i + 1, parameters.get(i));
           }
           ResultSet rs = statement.executeQuery();
           while (rs.next()) {
               if (rs.getInt("team_id") != 0) {
                   Team team = new Team();
                   team.setId(rs.getInt("team_id"));
                   team.setName(rs.getString("team_name"));
                   team.setContinent(ContinentEnum.valueOf(rs.getString("continent")));
                   teams.add(team);
               }
           }

       }
       return teams;
    }

    public List<Player> findPlayersByCriteria(
            String playerName,
            PlayerPositionEnum position,
            String teamName,
            ContinentEnum continent,
            int page,
            int size
    ) throws SQLException {

        List<Player> players = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
        select 
            p.id as player_id,
            p.name as player_name,
            p.age,
            p.position,
            t.id as team_id,
            t.name as team_name,
            t.continent
        from player p
        left join team t on p.id_team = t.id
        where 1 = 1
    """);

        List<Object> params = new ArrayList<>();


        if (playerName != null && !playerName.isBlank()) {
            sql.append(" and p.name like ? ");
            params.add("%" + playerName + "%");
        }


        if (position != null) {
            sql.append(" and p.position = ?::enum_position ");
            params.add(position.name());
        }


        if (teamName != null && !teamName.isBlank()) {
            sql.append(" and t.name = ? ");
            params.add(teamName);
        }


        if (continent != null) {
            sql.append(" and t.continent = ?::enum_continent ");
            params.add(continent.name());
        }


        sql.append(" limit ? offset ? ");
        int offset = (page - 1) * size;
        params.add(size);
        params.add(offset);

        try (Connection connection = dbConnection.getDBConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {


            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Player player = new Player();
                player.setId(rs.getInt("player_id"));
                player.setName(rs.getString("player_name"));
                player.setAge(rs.getInt("age"));
                player.setPosition(
                        PlayerPositionEnum.valueOf(rs.getString("position"))
                );


                int teamId = rs.getInt("team_id");
                if (!rs.wasNull()) {
                    Team team = new Team();
                    team.setId(teamId);
                    team.setName(rs.getString("team_name"));
                    team.setContinent(
                            ContinentEnum.valueOf(rs.getString("continent"))
                    );
                    player.setTeam(team);
                } else {
                    player.setTeam(null);
                }

                players.add(player);
            }
        }

        return players;
    }


}
