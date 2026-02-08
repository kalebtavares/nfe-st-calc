package com.kalebtavares.nfest.model;
import lombok.Data;

@Data
public class RegraTributaria {
    private String ncm;
    private String descricao; // Adicionado
    private Double mva;
    private Double aliquotaInterna;
}