package org.example;

public class Player {
private  int id;
private String name;
private int age;
private PlayerPositionEnum position;
private Team team;


public  Player() {}
    public Player(int id, String name, int age, PlayerPositionEnum positionEnum, Team team) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.position = positionEnum;
        this.team = team;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public PlayerPositionEnum getPosition() {
        return position;
    }

    public void setPosition(PlayerPositionEnum position) {
        this.position = position;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }



    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", position=" + position +
                '}';
    }
}
