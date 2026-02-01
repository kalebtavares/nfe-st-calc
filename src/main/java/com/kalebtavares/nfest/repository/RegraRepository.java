package com.kalebtavares.nfest.repository;

import com.kalebtavares.nfest.config.DatabaseConfig;
import com.kalebtavares.nfest.model.RegraTributaria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class RegraRepository {

    public Optional<RegraTributaria> buscarPorNcm(String ncm) {
        String sql = "SELECT * FROM regras_tributarias WHERE ncm = ? AND ativo = TRUE";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, ncm);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                RegraTributaria regra = new RegraTributaria();
                regra.setNcm(rs.getString("ncm"));
                regra.setMva(rs.getDouble("mva"));
                regra.setAliquotaInterna(rs.getDouble("aliquota_interna"));
                return Optional.of(regra);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}