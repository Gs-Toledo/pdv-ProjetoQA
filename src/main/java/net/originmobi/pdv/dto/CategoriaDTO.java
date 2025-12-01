package net.originmobi.pdv.dto;

import javax.validation.constraints.NotBlank;

public class CategoriaDTO {

    @NotBlank
    private String nome;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}