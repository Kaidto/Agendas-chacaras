package com.portfolio.agendamento.dto;

import com.portfolio.agendamento.model.Chacara;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChacaraResponseDTO {

    private Long id;
    private String nome;
    private String descricao;
    private String imagemUrl;

    public static ChacaraResponseDTO fromEntity(Chacara c) {
        return new ChacaraResponseDTO(c.getId(), c.getNome(), c.getDescricao(), c.getImagemUrl());
    }
}
