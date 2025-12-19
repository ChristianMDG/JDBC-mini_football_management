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
        List<Player> addedPlayers = new ArrayList<>();
        String checkQuery = "SELECT COUNT(*) FROM Player WHERE id = ?";
        String insertQuery = "INSERT INTO Player(id, name, age, position, id_team) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = dbConnection.getDBConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery);
                 PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {

                for (Player player : newPlayers) {

                    checkStmt.setInt(1, player.getId());
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        throw new RuntimeException("Le joueur avec l'id " + player.getId() + " existe déjà !");
                    }


                    insertStmt.setInt(1, player.getId());
                    insertStmt.setString(2, player.getName());
                    insertStmt.setInt(3, player.getAge());
                    insertStmt.setString(4, player.getPosition().name());
                    insertStmt.setInt(5, player.getTeam().getId());

                    insertStmt.executeUpdate();
                    addedPlayers.add(player);
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                addedPlayers.clear();
                throw new RuntimeException("Transaction annulée : " + e.getMessage(), e);
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création des joueurs : " + e.getMessage(), e);
        }

        return addedPlayers;
    }

    public Team saveTeam(Team teamToSave) {
        String checkTeamQuery = "SELECT COUNT(*) FROM Team WHERE id = ?";
        String insertTeamQuery = "INSERT INTO Team(id, name, continent) VALUES (?, ?, ?)";
        String updateTeamQuery = "UPDATE Team SET name = ?, continent = ? WHERE id = ?";
        String dissociatePlayersQuery = "UPDATE Player SET id_team = NULL WHERE id_team = ?";
        String associatePlayerQuery = "UPDATE Player SET id_team = ? WHERE id = ?";

        try (Connection connection = dbConnection.getDBConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement checkStmt = connection.prepareStatement(checkTeamQuery);
                 PreparedStatement insertStmt = connection.prepareStatement(insertTeamQuery);
                 PreparedStatement updateStmt = connection.prepareStatement(updateTeamQuery);
                 PreparedStatement dissocStmt = connection.prepareStatement(dissociatePlayersQuery);
                 PreparedStatement assocStmt = connection.prepareStatement(associatePlayerQuery)) {


                checkStmt.setInt(1, teamToSave.getId());
                ResultSet rs = checkStmt.executeQuery();
                boolean teamExists = false;
                if (rs.next()) {
                    teamExists = rs.getInt(1) > 0;
                }


                if (teamExists) {
                    updateStmt.setString(1, teamToSave.getName());
                    updateStmt.setString(2, teamToSave.getContinent().name());
                    updateStmt.setInt(3, teamToSave.getId());
                    updateStmt.executeUpdate();
                } else {
                    insertStmt.setInt(1, teamToSave.getId());
                    insertStmt.setString(2, teamToSave.getName());
                    insertStmt.setString(3, teamToSave.getContinent().name());
                    insertStmt.executeUpdate();
                }


                dissocStmt.setInt(1, teamToSave.getId());
                dissocStmt.executeUpdate();


                if (teamToSave.getPlayers() != null) {
                    for (Player player : teamToSave.getPlayers()) {
                        assocStmt.setInt(1, teamToSave.getId());
                        assocStmt.setInt(2, player.getId());
                        assocStmt.executeUpdate();
                    }
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw new RuntimeException("Transaction annulée : " + e.getMessage(), e);
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde de l'équipe : " + e.getMessage(), e);
        }

        return teamToSave;
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
    ) {
        List<Player> players = new ArrayList<>();
        StringBuilder query = new StringBuilder(
                "SELECT p.id, p.name, p.age, p.position, t.id as team_id, t.name as team_name, t.continent " +
                        "FROM Player p " +
                        "LEFT JOIN Team t ON p.id_team = t.id " +
                        "WHERE 1=1 "
        );

        List<Object> parameters = new ArrayList<>();

        if (playerName != null && !playerName.isEmpty()) {
            query.append(" AND p.name ILIKE ? ");
            parameters.add("%" + playerName + "%");
        }

        if (position != null) {
            query.append(" AND p.position = ? ");
            parameters.add(position.name());
        }

        if (teamName != null && !teamName.isEmpty()) {
            query.append(" AND t.name ILIKE ? ");
            parameters.add("%" + teamName + "%");
        }

        if (continent != null) {
            query.append(" AND t.continent = ? ");
            parameters.add(continent.name());
        }

        query.append(" ORDER BY p.id ");
        query.append(" LIMIT ? OFFSET ? ");
        parameters.add(size);
        parameters.add(page * size);

        try (Connection connection = dbConnection.getDBConnection();
             PreparedStatement stmt = connection.prepareStatement(query.toString())) {

            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Player player = new Player();
                player.setId(rs.getInt("id"));
                player.setName(rs.getString("name"));
                player.setAge(rs.getInt("age"));
                player.setPosition(PlayerPositionEnum.valueOf(rs.getString("position")));

                Team team = new Team();
                team.setId(rs.getInt("team_id"));
                team.setName(rs.getString("team_name"));
                String continentStr = rs.getString("continent");
                if (continentStr != null) {
                    team.setContinent(ContinentEnum.valueOf(continentStr));
                }
                player.setTeam(team);

                players.add(player);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des joueurs : " + e.getMessage(), e);
        }

        return players;
    }



}
