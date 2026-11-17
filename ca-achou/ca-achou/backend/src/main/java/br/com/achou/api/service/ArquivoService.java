package br.com.achou.api.service;

import br.com.achou.api.config.AchouProperties;
import br.com.achou.api.domain.Arquivo;
import br.com.achou.api.repository.ArquivoRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

/** Armazena fotos no banco de dados. O tipo é detectado pelos bytes (não pelo que o cliente declara). */
@Service
public class ArquivoService {

    private static final Pattern NOME_VALIDO = Pattern.compile("[a-f0-9\\-]{36}\\.(jpg|png|webp)");

    private final ArquivoRepository repositorio;
    private final long maximo;

    public ArquivoService(ArquivoRepository repositorio, AchouProperties props) {
        this.repositorio = repositorio;
        this.maximo = props.uploads().tamanhoMaximoBytes();
    }

    /** Valida e grava a imagem; devolve o nome do arquivo gerado. */
    public String salvar(byte[] dados) {
        if (dados == null || dados.length == 0) {
            throw ApiException.invalido("Nenhuma imagem foi enviada.");
        }
        if (dados.length > maximo) {
            throw ApiException.invalido("A imagem é grande demais (máximo " + (maximo / 1024 / 1024) + " MB).");
        }
        String ext = detectarExtensao(dados);
        if (ext == null) {
            throw ApiException.invalido("Formato de imagem não suportado. Use JPG, PNG ou WebP.");
        }
        String nome = UUID.randomUUID() + "." + ext;
        repositorio.save(new Arquivo(nome, dados));
        return nome;
    }

    public void excluir(String nome) {
        if (nome == null || !NOME_VALIDO.matcher(nome).matches()) return;
        repositorio.deleteById(nome);
    }

    public Resource carregar(String nome) {
        if (nome == null || !NOME_VALIDO.matcher(nome).matches()) {
            throw ApiException.naoEncontrado("Arquivo não encontrado.");
        }
        Arquivo a = repositorio.findById(nome)
                .orElseThrow(() -> ApiException.naoEncontrado("Arquivo não encontrado."));
        return new ByteArrayResource(a.getDados());
    }

    public MediaType tipoDe(String nome) {
        if (nome.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (nome.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }

    private static String detectarExtensao(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }
}
