package br.com.achou.api;

import br.com.achou.api.domain.Usuario;
import br.com.achou.api.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:achou-test;DB_CLOSE_DELAY=-1",
        "achou.uploads.dir=${java.io.tmpdir}/achou-test-uploads"
})
@AutoConfigureMockMvc
class FluxoCompletoTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockitoBean EmailService email;

    private JsonNode chamar(HttpMethod m, String url, String token, Object corpo, int esperado) throws Exception {
        var b = request(m, url);
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (corpo != null) b.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(corpo));
        String res = mvc.perform(b).andExpect(status().is(esperado)).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        return res.isBlank() ? null : om.readTree(res);
    }

    private String cadastrar(String rm, String nome) throws Exception {
        JsonNode r = chamar(HttpMethod.POST, "/api/auth/cadastro", null, Map.of(
                "nome", nome, "sobrenome", "Souza", "rm", rm, "email", nome.toLowerCase() + "@exemplo.com",
                "curso", "Desenvolvimento de Sistemas", "ano", 2, "senha", "segredo123"), 201);
        return r.get("token").asText();
    }

    @Test
    void cadastroLoginPublicacaoFiltrosEComentarios() throws Exception {
        String tokenAna = cadastrar("12345", "Ana");
        String tokenLeo = cadastrar("54321", "Leo");

        // RM duplicado e senha errada
        chamar(HttpMethod.POST, "/api/auth/cadastro", null, Map.of(
                "nome", "X", "sobrenome", "Y", "rm", "12345", "email", "x@y.com",
                "curso", "Informática", "ano", 1, "senha", "segredo123"), 409);
        chamar(HttpMethod.POST, "/api/auth/login", null, Map.of("rm", "12345", "senha", "errada"), 401);
        JsonNode login = chamar(HttpMethod.POST, "/api/auth/login", null, Map.of("rm", "12345", "senha", "segredo123"), 200);
        assertEquals("Ana Souza", login.get("usuario").get("nomeCompleto").asText());

        // sem token = 401
        chamar(HttpMethod.GET, "/api/publicacoes", null, null, 401);

        // publicação com categoria/subtipo
        JsonNode pub = chamar(HttpMethod.POST, "/api/publicacoes", tokenAna, Map.of(
                "tipo", "PERDI", "titulo", "Fone bluetooth preto", "descricao", "Caixinha com adesivo",
                "categoria", "ELETRONICOS", "subcategoria", "Fones de ouvido", "local", "Quadra"), 201);
        long id = pub.get("id").asLong();

        // subtipo inválido para a categoria
        chamar(HttpMethod.POST, "/api/publicacoes", tokenAna, Map.of(
                "tipo", "PERDI", "titulo", "Algo", "categoria", "ACESSORIOS", "subcategoria", "Celular",
                "local", "Pátio"), 400);

        // upload de foto (assinatura PNG) e rejeição de arquivo que não é imagem
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        String resp = mvc.perform(request(HttpMethod.PUT, "/api/publicacoes/" + id + "/foto")
                        .header("Authorization", "Bearer " + tokenAna)
                        .contentType(MediaType.IMAGE_PNG).content(png))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(om.readTree(resp).get("fotoUrl").asText().startsWith("/api/arquivos/"));
        mvc.perform(request(HttpMethod.PUT, "/api/publicacoes/" + id + "/foto")
                        .header("Authorization", "Bearer " + tokenAna)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM).content("texto".getBytes()))
                .andExpect(status().isBadRequest());

        // filtros
        assertEquals(1, chamar(HttpMethod.GET, "/api/publicacoes?categoria=ELETRONICOS", tokenLeo, null, 200).get("total").asInt());
        assertEquals(1, chamar(HttpMethod.GET, "/api/publicacoes?categoria=ELETRONICOS&subcategoria=Fones de ouvido", tokenLeo, null, 200).get("total").asInt());
        assertEquals(0, chamar(HttpMethod.GET, "/api/publicacoes?categoria=ACESSORIOS", tokenLeo, null, 200).get("total").asInt());
        assertEquals(1, chamar(HttpMethod.GET, "/api/publicacoes?q=bluetooth", tokenLeo, null, 200).get("total").asInt());
        assertEquals(0, chamar(HttpMethod.GET, "/api/publicacoes?q=%", tokenLeo, null, 200).get("total").asInt());
        assertEquals(0, chamar(HttpMethod.GET, "/api/publicacoes?minhas=true", tokenLeo, null, 200).get("total").asInt());

        // comentários
        chamar(HttpMethod.POST, "/api/publicacoes/" + id + "/comentarios", tokenLeo, Map.of("texto", "Vi na quadra!"), 201);
        assertEquals(1, chamar(HttpMethod.GET, "/api/publicacoes/" + id + "/comentarios", tokenAna, null, 200).size());
        assertEquals(1, chamar(HttpMethod.GET, "/api/publicacoes?categoria=ELETRONICOS", tokenAna, null, 200)
                .get("itens").get(0).get("comentarios").asInt());

        // só o autor altera
        chamar(HttpMethod.PATCH, "/api/publicacoes/" + id + "/devolvido", tokenLeo, null, 403);
        assertTrue(chamar(HttpMethod.PATCH, "/api/publicacoes/" + id + "/devolvido", tokenAna, null, 200).get("devolvido").asBoolean());
        chamar(HttpMethod.DELETE, "/api/publicacoes/" + id, tokenLeo, null, 403);
        chamar(HttpMethod.DELETE, "/api/publicacoes/" + id, tokenAna, null, 204);
    }

    @Test
    void recuperacaoDeSenhaPorCodigo() throws Exception {
        cadastrar("99999", "Bia");

        // RM inexistente responde igual (não revela cadastro)
        chamar(HttpMethod.POST, "/api/auth/esqueci-senha", null, Map.of("rm", "00000"), 200);
        chamar(HttpMethod.POST, "/api/auth/esqueci-senha", null, Map.of("rm", "99999"), 200);

        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(email).enviarCodigoRecuperacao(any(Usuario.class), codigo.capture(), anyInt());

        chamar(HttpMethod.POST, "/api/auth/redefinir-senha", null,
                Map.of("rm", "99999", "codigo", "000000".equals(codigo.getValue()) ? "111111" : "000000", "novaSenha", "novaSenha1"), 400);
        chamar(HttpMethod.POST, "/api/auth/redefinir-senha", null,
                Map.of("rm", "99999", "codigo", codigo.getValue(), "novaSenha", "novaSenha1"), 200);

        chamar(HttpMethod.POST, "/api/auth/login", null, Map.of("rm", "99999", "senha", "segredo123"), 401);
        chamar(HttpMethod.POST, "/api/auth/login", null, Map.of("rm", "99999", "senha", "novaSenha1"), 200);
        // o código não pode ser reutilizado
        chamar(HttpMethod.POST, "/api/auth/redefinir-senha", null,
                Map.of("rm", "99999", "codigo", codigo.getValue(), "novaSenha", "outraSenha1"), 400);
    }
}
