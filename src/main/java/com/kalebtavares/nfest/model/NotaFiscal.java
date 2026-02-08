package com.kalebtavares.nfest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotaFiscal {

    private String chaveAcesso;
    private String nomeEmitente;
    private String cnpjEmitente;
    private String nomeDestinatario;
    private String ufDestino;

    private Double valorTotalNota;
    private Double valorTotalProdutos;

    // Lista de produtos da nota
    private List<Produto> produtos;

    // --- Mapeamento da estrutura complexa do XML ---

    @JsonProperty("NFe")
    private void unpackNFe(Map<String, Object> nfe) {
        Map<String, Object> infNFe = (Map<String, Object>) nfe.get("infNFe");

        // Dados da Nota (Chave)
        this.chaveAcesso = (String) infNFe.get("Id"); // Vem como "NFe3522..."

        // Dados do Emitente
        Map<String, Object> emit = (Map<String, Object>) infNFe.get("emit");
        this.nomeEmitente = (String) emit.get("xNome");
        this.cnpjEmitente = (String) emit.get("CNPJ");

        // Dados do Destinatário
        Map<String, Object> dest = (Map<String, Object>) infNFe.get("dest");
        this.nomeDestinatario = (String) dest.get("xNome");
        Map<String, Object> enderDest = (Map<String, Object>) dest.get("enderDest");
        this.ufDestino = (String) enderDest.get("UF");

        // Dados de Totais
        Map<String, Object> total = (Map<String, Object>) infNFe.get("total");
        Map<String, Object> icmsTot = (Map<String, Object>) total.get("ICMSTot");
        this.valorTotalNota = Double.valueOf((String) icmsTot.get("vNF"));
        this.valorTotalProdutos = Double.valueOf((String) icmsTot.get("vProd"));
    }

    // A lista de produtos (det) precisa ser mapeada separadamente para virar List<Produto>
    // O Jackson XML é um pouco chato com listas sem wrapper, faremos isso no Service ou aqui.
    // Para simplificar, vamos deixar a injeção da lista para o Parser configurar corretamente.
}