package br.com.achou.api.service;

import br.com.achou.api.config.AchouProperties;
import br.com.achou.api.domain.Usuario;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final AchouProperties props;
    private final String host;
    private final int port;

    public EmailService(ObjectProvider<JavaMailSender> sender, AchouProperties props,
                        @Value("${spring.mail.host:}") String host,
                        @Value("${spring.mail.port:587}") int port) {
        this.sender = sender;
        this.props = props;
        this.host = host;
        this.port = port;
    }

    /** Falha ao subir (e não só na hora de enviar) se o e-mail foi ligado sem servidor SMTP configurado. */
    @PostConstruct
    void validarConfiguracao() {
        if (!props.mail().enabled()) {
            log.warn("E-mail DESATIVADO (ACHOU_MAIL_ENABLED=false): os códigos de recuperação serão escritos no log.");
            return;
        }
        if (host == null || host.isBlank()) {
            throw new IllegalStateException(
                    "ACHOU_MAIL_ENABLED=true exige ACHOU_MAIL_HOST, ACHOU_MAIL_USER e ACHOU_MAIL_PASSWORD.");
        }
        if (props.mail().from() == null || props.mail().from().endsWith(".local")) {
            log.warn("ACHOU_MAIL_FROM não foi definido: provedores SMTP costumam recusar o remetente padrão.");
        }
        log.info("E-mail ATIVADO via SMTP {}:{}.", host, port);
    }

    /**
     * Envia o código de recuperação. É assíncrono para que o tempo de resposta não revele se o RM existe.
     * Com e-mail desativado (padrão de desenvolvimento), o código é escrito no log do servidor.
     */
    @Async
    public void enviarCodigoRecuperacao(Usuario usuario, String codigo, int validadeMinutos) {
        JavaMailSender mailSender = sender.getIfAvailable();
        if (!props.mail().enabled() || mailSender == null) {
            log.warn("[DESENVOLVIMENTO] E-mail desativado. Código de recuperação do RM {}: {}", usuario.getRm(), codigo);
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(props.mail().from(), "CA Achou?");
            h.setTo(usuario.getEmail());
            h.setSubject("CA Achou? - código para redefinir sua senha");
            h.setText(texto(usuario.getNome(), codigo, validadeMinutos), html(usuario.getNome(), codigo, validadeMinutos));
            mailSender.send(msg);
            log.info("E-mail de recuperação enviado para o RM {}.", usuario.getRm());
        } catch (Exception e) {
            // Roda em outra thread: a exceção não chega ao usuário, então o log é o único rastro.
            log.error("Falha ao enviar e-mail de recuperação para o RM {} (SMTP {}:{}). "
                    + "Confira host, porta, usuário, senha e se a rede libera essa porta.",
                    usuario.getRm(), host, port, e);
        }
    }

    private static String texto(String nome, String codigo, int minutos) {
        return "Olá, " + nome + "!\n\n"
                + "Seu código para redefinir a senha do CA Achou? é: " + codigo + "\n"
                + "Ele vale por " + minutos + " minutos.\n\n"
                + "Se você não pediu a redefinição, ignore esta mensagem.";
    }

    private static String html(String nome, String codigo, int minutos) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:auto;color:#1b3057">
                  <h2 style="margin:0 0 12px">CA Achou?</h2>
                  <p>Olá, %s!</p>
                  <p>Use o código abaixo para redefinir sua senha:</p>
                  <p style="font-size:32px;font-weight:bold;letter-spacing:8px;text-align:center;color:#e00008;background:#f1f4fa;border-radius:8px;padding:14px">%s</p>
                  <p>Ele vale por %d minutos.</p>
                  <p style="color:#6b7894;font-size:13px">Se você não pediu a redefinição, ignore esta mensagem.</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(nome), codigo, minutos);
    }
}
