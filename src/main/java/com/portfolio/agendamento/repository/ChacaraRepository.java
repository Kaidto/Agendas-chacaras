package com.portfolio.agendamento.repository;

import com.portfolio.agendamento.model.Chacara;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChacaraRepository extends JpaRepository<Chacara, Long> {
    Optional<Chacara> findByNome(String nome);
}
