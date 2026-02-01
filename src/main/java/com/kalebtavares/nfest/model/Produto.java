package com.kalebtavares.nfest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data // O Lombok gera Getters, Setters e toString automaticamente
@JsonIgnoreProperties(ignoreUnknown = true) // Ignora tags que não usaremos (ex: codigo de barras)
public class Produto {

    private String codigo;
    private String nome;
    private String ncm;
    private String cest;
    private String cfop;
    private Double valorProduto;
    private Double valorFrete; // Importante para base de calculo
    private Double valorIPI;   // Importante para base de calculo

    // Campos calculados pelo nosso sistema depois
    private Double mvaAplicada;
    private Double icmsStCalculado;
    private boolean devePagarSt;

    // O Jackson usa esse construtor/setters para preencher os dados vindo da tag <prod>
    @JsonProperty("prod")
    private void unpackProd(java.util.Map<String, Object> prod) {
        this.codigo = (String) prod.get("cProd");
        this.nome = (String) prod.get("xProd");
        this.ncm = (String) prod.get("NCM");
        this.cest = (String) prod.get("CEST");
        this.cfop = (String) prod.get("CFOP");
        this.valorProduto = Double.valueOf((String) prod.get("vProd"));

        // Frete e IPI as vezes não existem no item, precisamos tratar
        this.valorFrete = prod.containsKey("vFrete") ? Double.valueOf((String) prod.get("vFrete")) : 0.0;
    }

    // Mapeamento do imposto para pegar IPI se houver
    @JsonProperty("imposto")
    private void unpackImposto(java.util.Map<String, Object> imposto) {
        // Lógica simplificada para extrair IPI se necessário no futuro
        // Por enquanto vamos focar no valor do produto
    }
}