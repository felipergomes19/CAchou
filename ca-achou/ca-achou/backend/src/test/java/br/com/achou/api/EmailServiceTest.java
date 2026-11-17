package br.com.achou.api;

import br.com.achou.api.config.AchouProperties;
import br.com.achou.api.domain.Usuario;
import br.com.achou.api.service.EmailService;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testa o EmailService isolado (sem subir o Spring): não envia e-mail de verdade, usa um JavaMailSender falso. */
class EmailServiceTest {

    private static AchouProperties props(boolean enabled) {
        return new AchouProperties(null, new AchouProperties.Uploads(1024),
                new AchouProperties.Mail(enabled, "no-reply@escola.exemplo"), null, List.of());
    }

    private static Usuario usuario() {
        Usuario u = new Usuario();
        u.setRm("12345");
        u.setNome("Ana");
        u.setEmail("ana@exemplo.com");
        return u;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> provedor(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(sender);
        return p;
    }

    @Test
    void enviaMensagemParaOEmailDoUsuarioQuandoAtivado() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

        new EmailService(provedor(sender), props(true), "smtp.exemplo.com", 587)
                .enviarCodigoRecuperacao(usuario(), "123456", 15);

        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(enviado.capture());
        assertEquals("ana@exemplo.com", enviado.getValue().getRecipients(Message.RecipientType.TO)[0].toString());
        assertTrue(enviado.getValue().getSubject().contains("código"));
    }

    @Test
    void naoEnviaNadaQuandoDesativado() {
        JavaMailSender sender = mock(JavaMailSender.class);

        new EmailService(provedor(sender), props(false), "", 587)
                .enviarCodigoRecuperacao(usuario(), "123456", 15);

        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void falhaDeEnvioNaoPropagaExcecao() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenThrow(new IllegalStateException("SMTP fora do ar"));

        assertDoesNotThrow(() -> new EmailService(provedor(sender), props(true), "smtp.exemplo.com", 587)
                .enviarCodigoRecuperacao(usuario(), "123456", 15));
    }

    @Test
    void ligarEmailSemServidorSmtpFalhaNaInicializacao() {
        EmailService servico = new EmailService(provedor(null), props(true), "", 587);
        // validarConfiguracao() é package-private e roda no @PostConstruct; aqui exercitamos via reflexão.
        var ex = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            var m = EmailService.class.getDeclaredMethod("validarConfiguracao");
            m.setAccessible(true);
            m.invoke(servico);
        });
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }
}
