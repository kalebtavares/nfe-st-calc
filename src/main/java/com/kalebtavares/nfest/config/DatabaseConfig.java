package com.kalebtavares.nfest.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class DatabaseConfig {

    // O banco será criado na pasta do usuário para não sumir
    private static final String DB_URL = "jdbc:h2:./dados_nfe_db;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    // Método para rodar aquele script SQL inicial
    public static void inicializarBanco() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Lê o arquivo init.sql dos resources
            String sql = new String(DatabaseConfig.class
                    .getResourceAsStream("/database/init.sql").readAllBytes());

            // Executa os comandos
            stmt.execute(sql);
            System.out.println("Banco de dados inicializado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro ao inicializar banco: " + e.getMessage());
        }
    }
}