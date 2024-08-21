package dto;

import server.DBConnection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private int uuidLength;
    private String uuid;
    private int lv;
    private int repeat;
    private int usernameLength;
    private String username;
    private int roguepoint;

    public static Player deserializePlayer(byte[] data) throws IOException {
        System.out.println("[" + data.length + "] DB Packet Received: " + bytesToHex(data));


        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bais)) {

            Player playerData = new Player();

            // UUID 읽기=
            int uuidLength = dis.readByte();
            byte[] uuidBytes = new byte[uuidLength];
            for (int i = 0; i < uuidBytes.length; i++) {
                uuidBytes[i] = dis.readByte();
            }
            playerData.setUuid(new String(uuidBytes, StandardCharsets.UTF_8));

            // Level 읽기
            int levelByte = reverseBytesRe(dis);
            playerData.setLv(levelByte);

            // Repeat 읽기
            int repeatByte = reverseBytesRe(dis);
            playerData.setRepeat(repeatByte);

            // Username 읽기
            int usernameLength = dis.readByte();
            byte[] usernameBytes = new byte[usernameLength];
            for (int i = 0; i < usernameLength; i++) {
                usernameBytes[i] = dis.readByte();
            }
            playerData.setUsername(new String(usernameBytes, StandardCharsets.UTF_8));

            int roguepointByte = reverseBytesRe(dis);
            playerData.setRoguepoint(roguepointByte);
            System.out.println("UUID: " + playerData.getUuid());
            System.out.println("Level: " + playerData.getLv());
            System.out.println("Repeat: " + playerData.getRepeat());
            System.out.println("Username: " + playerData.getUsername());
            System.out.println("Roguepoint: " + playerData.getRoguepoint());

            return playerData;
        }
    }

    private static int reverserByte(DataInputStream dis) throws IOException {
        byte[] readBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            readBytes[i] = dis.readByte();
        }
        int levelByte = ((readBytes[0] & 0xFF)) |
                ((readBytes[1] & 0xFF) << 8) |
                ((readBytes[2] & 0xFF) << 16) |
                ((readBytes[3] & 0xFF) << 24);
        return levelByte;
    }

    private static int reverseBytesRe(DataInputStream dis) throws IOException {
        int originalValue = reverserByte(dis);

        if (originalValue > 1000) {
            // 역순으로 바이트를 읽고 변환
            byte[] reversedBytes = new byte[4];
            for (int i = 0; i < 4; i++) {
                reversedBytes[3 - i] = dis.readByte();
            }
            int reversedValue = ((reversedBytes[0] & 0xFF)) |
                    ((reversedBytes[1] & 0xFF) << 8) |
                    ((reversedBytes[2] & 0xFF) << 16) |
                    ((reversedBytes[3] & 0xFF) << 24);
            return reversedValue;
        } else {
            return originalValue;
        }
    }

    public static void newPlayer(byte[] data) throws IOException, SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            Date nowTime = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String createDate = format.format(nowTime);
            Player player = deserializePlayer(data);
            String insert = "INSERT INTO account VALUES (null,?,?,?,?,?,null,?)";
            PreparedStatement ps = conn.prepareStatement(insert);
            ps.setString(1, player.getUuid());
            ps.setString(2, player.getUsername());
            ps.setInt(3, player.getLv());
            ps.setInt(4, player.getRepeat());
            ps.setString(5, createDate);
            ps.setInt(6, player.getRoguepoint());
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void savePlayer(byte[] data) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            Date nowTime = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String lastDate = format.format(nowTime);
            Player player = deserializePlayer(data);
            String updateQuery = "UPDATE account SET lv = ?, `repeat` = ?, username = ?, lastloggedin = ?, roguepoint = ? WHERE uuid = ?";
            PreparedStatement pstmt = con.prepareStatement(updateQuery);
            pstmt.setInt(1, player.getLv());
            pstmt.setInt(2, player.getRepeat());
            pstmt.setString(3, player.getUsername());
            pstmt.setString(4, lastDate);
            pstmt.setInt(5, player.getRoguepoint());
            pstmt.setString(6, player.getUuid());
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static byte[] getPlayer(byte[] data) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            Player player = deserializePlayer(data);
            String Select = "SELECT * FROM account WHERE uuid = ?";
            PreparedStatement pstmt = con.prepareStatement(Select);
            pstmt.setString(1, player.getUuid());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                player.setUuid(rs.getString("uuid"));
                player.setLv(rs.getInt("lv"));
                player.setRepeat(rs.getInt("repeat"));
                player.setUsername(rs.getString("username"));
                player.setRoguepoint(rs.getInt("roguepoint"));
            }
            return SerializePlayerData(player);
        }
    }

    public static String setCode(byte[] data) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            Player player = deserializePlayer(data);

            String updateQuery = "UPDATE account SET continuouscode = ? WHERE uuid = ?";
            String checkQuery = "SELECT COUNT(*) FROM account WHERE continuouscode = ?";

            String passCode;
            boolean isDuplicate;

            do {
                passCode = randomString(8);
                try (PreparedStatement checkStmt = con.prepareStatement(checkQuery)) {
                    checkStmt.setString(1, passCode);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        rs.next();
                        isDuplicate = rs.getInt(1) > 0;  // 중복되면 true, 중복되지 않으면 false
                    }
                }
            } while (isDuplicate);

            try (PreparedStatement pstmt = con.prepareStatement(updateQuery)) {
                pstmt.setString(1, passCode);
                pstmt.setString(2, player.getUuid());
                pstmt.executeUpdate();
            }

            return passCode;
        }
    }

    public static byte[] checkCode(byte[] data) throws IOException, SQLException {
        try (Connection con = DBConnection.getConnection()) {
            byte[] playerData = null;
            Player player = deserializePlayer(data);
            String selectQuery = "SELECT * FROM account WHERE continuouscode = ?";
            boolean vaild = false;
            PreparedStatement pstmt = con.prepareStatement(selectQuery);
            pstmt.setString(1, player.getUuid());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                vaild = Objects.equals(player.getUsername(), rs.getString("continuouscode"));
            }
            if(vaild){
                playerData = getPlayer(data);
                setCode(data);
                System.out.println("Checked UUID : "+player.getUuid());
            }
            return playerData;
        }
    }

    private static byte[] SerializePlayerData(Player playerData) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);
            // UUID 쓰기
            byte[] uuidBytes = playerData.getUuid().getBytes(StandardCharsets.UTF_8);
            out.writeByte(playerData.getUuid().length());
            out.write(uuidBytes); // UUID 데이터

            // Level을 int로 직렬화
            out.writeInt(playerData.getLv());

            // Repeat을 int로 직렬화
            out.writeInt(playerData.getRepeat());

            // Username 쓰기
            byte[] usernameBytes = playerData.getUsername().getBytes();
            out.write(playerData.getUsername().getBytes(StandardCharsets.UTF_8).length);
            out.write(usernameBytes); // Username 데이터

            out.write(playerData.getRoguepoint());
/*
            System.out.println("W UUID Length: "+playerData.getUuid().length());
            System.out.println("W UUID: "+playerData.getUuid());
            System.out.println("W LV: "+playerData.getLv());
            System.out.println("W Repeat: "+playerData.getRepeat());
            System.out.println("W Username Length: "+playerData.getUsername().length());
            System.out.println("W Username: "+playerData.getUsername());*/
            // 바이트 배열로 변환하여 반환
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    public static String randomString(int usernameLength) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < usernameLength; i++) {
            boolean isUpperCase = random.nextBoolean();
            char randomChar;
            if (isUpperCase) {
                randomChar = (char) ('A' + random.nextInt(26));
            } else {
                randomChar = (char) ('a' + random.nextInt(26));
            }
            sb.append(randomChar);
        }

        return sb.toString();
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getLv() {
        return lv;
    }

    public void setLv(int lv) {
        this.lv = lv;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getRoguepoint() {
        return roguepoint;
    }

    public void setRoguepoint(int roguepoint) {
        this.roguepoint = roguepoint;
    }
}
