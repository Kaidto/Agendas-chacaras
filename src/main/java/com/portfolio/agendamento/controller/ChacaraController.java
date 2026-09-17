package com.portfolio.agendamento.controller;

import com.portfolio.agendamento.dto.ChacaraResponseDTO;
import com.portfolio.agendamento.model.Chacara;
import com.portfolio.agendamento.repository.ChacaraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chacaras")
public class ChacaraController {

    private final ChacaraRepository chacaraRepository;

    public ChacaraController(ChacaraRepository chacaraRepository) {
        this.chacaraRepository = chacaraRepository;
    }

    @GetMapping
    public ResponseEntity<List<ChacaraResponseDTO>> listar() {
        List<ChacaraResponseDTO> chacaras = chacaraRepository.findAll().stream()
            .filter(Chacara::isAtiva)
            .map(ChacaraResponseDTO::fromEntity)
            .collect(Collectors.toList());

        return ResponseEntity.ok(chacaras);
    }

    /**
     * Permite ao usuário enviar seu próprio arquivo PNG/imagem para a chácara.
     * Salva tanto nos diretórios de desenvolvimento (src e target) quanto em uploads/.
     */
    @PostMapping(value = "/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> atualizarImagem(
            @PathVariable Long id,
            @RequestParam("arquivo") MultipartFile arquivo) {

        if (arquivo == null || arquivo.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Nenhum arquivo de imagem foi enviado."));
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Formato inválido. Por favor, envie uma imagem PNG ou JPG."));
        }

        Chacara chacara = chacaraRepository.findById(id).orElse(null);
        if (chacara == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", "Chácara não encontrada."));
        }

        try {
            String nomeNorm = chacara.getNome().toLowerCase();
            String nomeArquivoBase;
            if (nomeNorm.contains("francisco")) {
                nomeArquivoBase = "chacara_sao_francisco.png";
            } else if (nomeNorm.contains("magnolia")) {
                nomeArquivoBase = "chacara_magnolia.png";
            } else {
                nomeArquivoBase = "chacara_" + id + ".png";
            }

            byte[] bytes = arquivo.getBytes();

            // 1. Diretório de uploads no servidor
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Files.write(uploadDir.resolve(nomeArquivoBase), bytes);

            // 2. Se estiver em ambiente local de código-fonte, atualiza src/main/resources/static/images
            Path srcImagesDir = Paths.get("src", "main", "resources", "static", "images");
            if (Files.exists(srcImagesDir)) {
                Files.write(srcImagesDir.resolve(nomeArquivoBase), bytes);
            }

            // 3. Atualiza target/classes/static/images para refletir imediatamente no servidor em execução
            Path targetImagesDir = Paths.get("target", "classes", "static", "images");
            if (Files.exists(targetImagesDir)) {
                Files.write(targetImagesDir.resolve(nomeArquivoBase), bytes);
            }

            String urlPublica = "/images/" + nomeArquivoBase;
            chacara.setImagemUrl(urlPublica);
            chacaraRepository.save(chacara);

            return ResponseEntity.ok(Map.of(
                    "mensagem", "Imagem atualizada com sucesso!",
                    "imagemUrl", urlPublica,
                    "chacara", ChacaraResponseDTO.fromEntity(chacara)
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao salvar o arquivo: " + e.getMessage()));
        }
    }
}
