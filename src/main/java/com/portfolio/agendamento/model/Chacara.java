package com.portfolio.agendamento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chacaras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Chacara {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome; // "Sao Francisco" ou "Magnolia"

    @Column(length = 255)
    private String descricao;

    @Column(name = "imagem_url", length = 500)
    private String imagemUrl;

    @Column(nullable = false)
    private boolean ativa = true;
}
