/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tluigi003.reSender;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLiteJDBC {

    private Connection con;

    public Votes getVotesByUserAndMessage(long idUser, long idMessage) {
        Votes voto = null;
        try {
            Statement stmt = getCon().createStatement();
            stmt.execute("SELECT VotosMask FROM VOTOS WHERE USUARIO=" + idUser + " AND " + "MENSAJE=" + idMessage + ";");
            ResultSet set = stmt.getResultSet();
            if (set.next()) {
                voto = Votes.valueOf(set.getString(1));
            } else {
                voto = Votes.EMPTY;
            }

        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return voto;
    }

    public List<Integer> getAllMessages() {
        List<Integer> lista = new ArrayList<>();
        try {
            Statement stmt = getCon().createStatement();
            stmt.execute("SELECT MENSAJE FROM VOTOS GROUP BY MENSAJE;");
            ResultSet set = stmt.getResultSet();
            while (set.next()) {
                lista.add(set.getInt(1));
            }
        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return lista;
    }

    public boolean isMessageAlready(Integer i) {
        try {
            Statement stmt = getCon().createStatement();
            //System.out.println(String.format("INSERT INTO MENSAJES VALUES(%d,%s,%d);",message,photo,time));
            stmt.execute(String.format("SELECT * FROM MENSAJES WHERE IDMENSAJE=%d;", i));
            return stmt.getResultSet().next();
        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public void saveMessage(int message, String photo, int time) {
        try {
            Statement stmt = getCon().createStatement();
            //System.out.println(String.format("INSERT INTO MENSAJES VALUES(%d,%s,%d);",message,photo,time));

            stmt.execute(String.format("INSERT INTO MENSAJES VALUES(%d,'%s',%d);", message, photo, time));
        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public EnumMap<Votes, Integer> getVotesByMessage(long idMessage) {
        EnumMap<Votes, Integer> votos = new EnumMap<>(Votes.class);
        try {
            Statement stmt = getCon().createStatement();
            stmt.execute("SELECT VotosMask FROM VOTOS WHERE MENSAJE=" + idMessage + ";");
            ResultSet set = stmt.getResultSet();
            while (set.next()) {
                votos.put(Votes.valueOf(set.getString(1)), votos.getOrDefault(Votes.valueOf(set.getString(1)), 0) + 1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return votos;
    }

    public long loadTopMessage(Votes voto) {
        try {
            Statement stmt = getCon().createStatement();
            stmt.execute("SELECT MessageID FROM RANKING WHERE VOTE='" + voto.name() + "';");
            ResultSet set = stmt.getResultSet();
            if (set.next()) {
                return set.getLong(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public int getTopMessage(Votes voto, int column) {
        try {
            Statement stmt = getCon().createStatement();
            stmt.execute("SELECT count(*),Mensaje,VotosMask FROM Votos WHERE VotosMask='" + voto.name() + "' group by Mensaje, VotosMask order by Count(*) DESC limit 1;");
            ResultSet set = stmt.getResultSet();
            if (set.next()) {
                return set.getInt(column);
            }
        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public int[] getTopMessages(int arraySize, long desde, long hasta, boolean best) {
        try {
            Map<Integer, EnumMap<Votes, Integer>> mapa = new HashMap<>();
            EnumMap<Votes, Integer> tempMap = null;
            int[] devolver = new int[arraySize];
            int[] devolverScore = new int[arraySize];
            int lastMessage = -1;
            Statement stmt = getCon().createStatement();
            if (desde != Integer.MIN_VALUE || hasta != Integer.MAX_VALUE) {
                System.out.println(String.format("select count(*), Mensaje, VotosMask from Votos inner join Mensajes on Mensaje=IDMensaje where fecha between %d and %d group by Mensaje, VotosMask;", desde, hasta));
                stmt.execute(String.format("select count(*), Mensaje, VotosMask from Votos inner join Mensajes on Mensaje=IDMensaje where fecha between %d and %d group by Mensaje, VotosMask;", desde, hasta));
            } else {
                stmt.execute("select count(*), Mensaje,VotosMask from Votos group by Mensaje, VotosMask;");
            }
            ResultSet set = stmt.getResultSet();
            while (set.next()) {
                if (set.getInt(2) != lastMessage) {
                    if (tempMap != null) {
                        mapa.put(lastMessage, tempMap);
                    }
                    lastMessage = set.getInt(2);
                    tempMap = new EnumMap(Votes.class);
                }
                tempMap.put(Votes.valueOf(set.getString(3)), set.getInt(1));
            }
            mapa.put(lastMessage, tempMap);
            for (Integer i : mapa.keySet()) {
                boolean stop = false;
                int up = mapa.get(i).getOrDefault(Votes.UP, 0);
                int down = mapa.get(i).getOrDefault(Votes.DOWN, 0);
                int heart = mapa.get(i).getOrDefault(Votes.HEART, 0);
                int drops = mapa.get(i).getOrDefault(Votes.DROPS, 0);
                int calculo = up + 2 * heart + 3 * drops - 2 * down;
                for (int j = 0; j < devolver.length && !stop; j++) {
                    if ((best && calculo > devolverScore[j]) || (!best && calculo < devolverScore[j])) {
                        for (int k = devolver.length - 1; k > j; k--) {
                            devolver[k] = devolver[k - 1];
                            devolverScore[k] = devolverScore[k - 1];
                        }
                        devolverScore[j] = calculo;
                        devolver[j] = i;
                        stop = true;
                    }
                }
            }
            return devolver;
        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void saveTopMessage(Votes voto, long id) {
        try {
            Statement stmt = getCon().createStatement();
            stmt.execute(String.format("SELECT * FROM RANKING WHERE Vote='%s'", voto.toString()));
            ResultSet set = stmt.getResultSet();
            if (set.next()) {
                stmt.execute(String.format("UPDATE RANKING SET MessageID=%d WHERE Vote='%s'", id, voto.toString()));
            } else {
                stmt.execute("INSERT INTO RANKING VALUES ('" + voto.name() + "', " + id + ");");
            }
        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public LinkedHashSet<Long> getUpdatableMessages() {
        LinkedHashSet<Long> votos = new LinkedHashSet<>(0);
        try {
            Statement stmt = getCon().createStatement();
            stmt.execute("SELECT * FROM UpdateMessages;");
            ResultSet set = stmt.getResultSet();
            while (set.next()) {
                votos.add(set.getLong(1));
            }
            stmt = getCon().createStatement();
            stmt.execute("DELETE FROM UpdateMessages;");
        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
        return votos;
    }

    public void saveUpdatableMessages(LinkedHashSet<Long> votos) {
        try {
            for (Long l : votos) {
                System.out.println("Guardando");
                Statement stmt = getCon().createStatement();
                stmt.execute("INSERT INTO UpdateMessages VALUES (" + l + ");");
            }

        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void saveVote(long idUser, long idMessage, Votes voto) {
        try {
            Statement stmt = getCon().createStatement();
            stmt.execute("SELECT VotosMask FROM Votos WHERE USUARIO=" + idUser + " AND " + "MENSAJE=" + idMessage + ";");
            ResultSet set = stmt.getResultSet();
            if (set.next()) {
                stmt.execute("UPDATE Votos SET VOTOSMASK = '" + voto.name() + "' WHERE USUARIO=" + idUser + " AND " + "MENSAJE=" + idMessage + ";");
            } else {
                stmt.execute("INSERT INTO Votos VALUES (" + idUser + "," + idMessage + ",'" + voto.name() + "');");
            }
            stmt.close();
        } catch (SQLException ex) {
            Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private Connection getCon() {

        if (con == null) {
            try {
                newCon();
                return con;
            } catch (URISyntaxException ex) {
                Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            } catch (SQLException ex) {
                Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        } else {
            try {
                Statement stmt = con.createStatement();
                stmt.execute("SELECT VotosMask FROM Votos;");
                return con;
            } catch (SQLException ex) {
                try {
                    newCon();
                } catch (URISyntaxException ex1) {
                    Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex1);
                } catch (SQLException ex1) {
                    Logger.getLogger(SQLiteJDBC.class.getName()).log(Level.SEVERE, null, ex1);
                }finally{
                    return con;
                }
            }
        }

    }

    private void newCon() throws URISyntaxException, SQLException {
        URI dbUri = new URI(System.getenv("DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + dbUri.getPath();
        con = DriverManager.getConnection(dbUrl, username, password);
    }

    public SQLiteJDBC() {

        try {

            Class.forName("org.postgresql.Driver");
            con = null;
        } catch (ClassNotFoundException e) {
            System.out.println("Class not found " + e);
        }
        try {
            /*Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:BotInfo.db");
            System.out.println("Opened database successfully");*/

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }
}
