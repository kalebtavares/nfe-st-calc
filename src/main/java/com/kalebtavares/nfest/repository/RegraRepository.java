package com.kalebtavares.nfest.repository;

import com.kalebtavares.nfest.config.DatabaseConfig;
import com.kalebtavares.nfest.model.RegraTributaria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegraRepository {

    // Buscar uma única regra (usado no cálculo)
    public Optional<RegraTributaria> buscarPorNcm(String ncm) {
        String sql = "SELECT * FROM regras_tributarias WHERE ncm = ? AND ativo = TRUE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ncm);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(mapearRegra(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // Listar todas as regras (usado na tela de cadastro)
    public List<RegraTributaria> listarTodas() {
        List<RegraTributaria> lista = new ArrayList<>();
        String sql = "SELECT * FROM regras_tributarias ORDER BY ncm";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearRegra(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }

    // Salvar ou Atualizar (Upsert do H2)
    public void salvar(RegraTributaria regra) {
        // MERGE INTO verifica: Se o NCM já existe, atualiza. Se não, insere.
        String sql = "MERGE INTO regras_tributarias (ncm, descricao, mva, aliquota_interna, ativo) KEY(ncm) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, regra.getNcm());
            stmt.setString(2, regra.getDescricao());
            stmt.setDouble(3, regra.getMva());
            stmt.setDouble(4, regra.getAliquotaInterna());
            stmt.setBoolean(5, true); // Sempre ativo ao salvar
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Excluir regra
    public void excluir(String ncm) {
        String sql = "DELETE FROM regras_tributarias WHERE ncm = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ncm);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Método auxiliar para evitar repetição de código
    private RegraTributaria mapearRegra(ResultSet rs) throws SQLException {
        RegraTributaria regra = new RegraTributaria();
        regra.setNcm(rs.getString("ncm"));
        regra.setDescricao(rs.getString("descricao")); // Precisamos adicionar descricao no Model se não tiver
        regra.setMva(rs.getDouble("mva"));
        regra.setAliquotaInterna(rs.getDouble("aliquota_interna"));
        return regra;
    }
}