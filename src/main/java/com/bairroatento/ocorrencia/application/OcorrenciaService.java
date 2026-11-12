package com.bairroatento.ocorrencia.application;

import com.bairroatento.morador.infra.MoradorRepository;
import com.bairroatento.ocorrencia.domain.Ocorrencia;
import com.bairroatento.ocorrencia.infra.CategoriaRepository;
import com.bairroatento.ocorrencia.infra.OcorrenciaRepository;
import com.bairroatento.shared.domain.NaoEncontradoException;
import com.bairroatento.shared.domain.RegraNegocioException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OcorrenciaService {

    public static final int TAMANHO_PAGINA = 20;

    private final OcorrenciaRepository ocorrencias;
    private final CategoriaRepository categorias;
    private final MoradorRepository moradores;

    public OcorrenciaService(OcorrenciaRepository ocorrencias, CategoriaRepository categorias,
                             MoradorRepository moradores) {
        this.ocorrencias = ocorrencias;
        this.categorias = categorias;
        this.moradores = moradores;
    }

    @Transactional
    public Ocorrencia registrar(String categoria, String descricao, String endereco, String bairro,
                                Double latitude, Double longitude, Long autorId) {
        if (!moradores.existsById(autorId)) {
            throw new NaoEncontradoException("Morador " + autorId + " não encontrado");
        }
        if (categoria == null || !categorias.existsByNome(categoria.trim())) {
            throw new RegraNegocioException("Categoria inexistente: " + categoria);
        }
        return ocorrencias.save(
                new Ocorrencia(categoria, descricao, endereco, bairro, latitude, longitude, autorId));
    }

    @Transactional(readOnly = true)
    public Page<Ocorrencia> listar(String bairro, String categoria, String status, int pagina) {
        List<Specification<Ocorrencia>> filtros = new ArrayList<>();
        if (bairro != null && !bairro.isBlank()) filtros.add(igual("bairro", bairro.trim()));
        if (categoria != null && !categoria.isBlank()) filtros.add(igual("categoria", categoria.trim()));
        if (status != null && !status.isBlank()) filtros.add(igual("status", status.trim()));
        Specification<Ocorrencia> spec = Specification.allOf(filtros);
        var pageable = PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA,
                Sort.by(Sort.Direction.DESC, "dataCriacao").and(Sort.by(Sort.Direction.DESC, "id")));
        return ocorrencias.findAll(spec, pageable);
    }

    private static Specification<Ocorrencia> igual(String campo, String valor) {
        return (root, q, cb) -> cb.equal(root.get(campo), valor);
    }

    @Transactional(readOnly = true)
    public Ocorrencia buscar(Long id) {
        return ocorrencias.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Ocorrência " + id + " não encontrada"));
    }

    @Transactional
    public Ocorrencia confirmar(Long ocorrenciaId, Long moradorId) {
        Ocorrencia o = buscar(ocorrenciaId);
        if (!moradores.existsById(moradorId)) {
            throw new NaoEncontradoException("Morador " + moradorId + " não encontrado");
        }
        o.confirmar(moradorId);
        return ocorrencias.saveAndFlush(o);
    }
}
