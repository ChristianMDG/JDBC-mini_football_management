package org.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
                    players.add(player);
                }
            }

        }
        return players;
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
    public List<Player> createPlayers(List<Player> newPlayers) {

        String checkSql = "SELECT id FROM player WHERE id = ?";
        String insertSql = "INSERT INTO player (id, name, age, position, id_team) VALUES (?, ?, ?, ?::enum_position, ?)";

        if (newPlayers == null || newPlayers.isEmpty()) {
            return new ArrayList<>();
        }

        try (Connection connection = dbConnection.getDBConnection()) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    for (Player player : newPlayers) {
                        checkStmt.setInt(1, player.getId());
                        ResultSet rs = checkStmt.executeQuery();
                        if (rs.next()) {
                            connection.rollback();
                            throw new RuntimeException("Player ID " + player.getId() + " already exists");
                        }
                    }
                }

                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    for (Player player : newPlayers) {
                        insertStmt.setInt(1, player.getId());
                        insertStmt.setString(2, player.getName());
                        insertStmt.setInt(3, player.getAge());
                        insertStmt.setString(4, player.getPosition().name());
                        if (player.getTeam() != null) {
                            insertStmt.setInt(5, player.getTeam().getId());
                        } else {
                            insertStmt.setNull(5, Types.INTEGER);
                        }
                        insertStmt.executeUpdate();
                    }
                }

                connection.commit();
                return new ArrayList<>(newPlayers);

            } catch (SQLException e) {
                connection.rollback();
                throw new RuntimeException(e);
            } catch (RuntimeException e) {
                connection.rollback();
                throw e;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public Team saveTeam(Team teamToSave) throws SQLException {
        throw new RuntimeException("Not yet implemented");
    }
    List<Player> findPlayersByCriteria(String playerName, PlayerPositionEnum position, String teamName,
                                       ContinentEnum continent, int page, int size
    ) throws SQLException {
        Team team = new Team();

        List<Player> players = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT player.id AS player_id, player.name AS player_name, player.age AS age, player.position AS position,\s
                team.name AS team FROM  player LEFT JOIN team ON player.id_team = team.id
                where 1 = 1
               \s""");

        List<Object> parameters = new ArrayList<>();

        if (playerName != null) {
            sql.append("and player.name ilike ? ");
            parameters.add("%" + playerName + "%");
        }
        if (position != null) {
            sql.append("and player.position = ? ");
            parameters.add(position);
        }
        if (teamName != null) {
            sql.append("and team.name = ? ");
            parameters.add(teamName);
        }
        if (continent != null) {
            sql.append("and player.position = ? ");
            parameters.add(continent);
        }
        sql.append("limit ? offset ?");

        int offset = (page - 1) * size;
        parameters.add(size);
        parameters.add(offset);

        try (Connection connection = dbConnection.getDBConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
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

}
