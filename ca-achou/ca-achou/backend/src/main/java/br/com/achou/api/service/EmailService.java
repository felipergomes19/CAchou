package br.com.achou.api.service;

import br.com.achou.api.config.AchouProperties;
import br.com.achou.api.domain.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final AchouProperties props;

    public EmailService(ObjectProvider<JavaMailSender> sender, AchouProperties props) {
        this.sender = sender;
        this.props = props;
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
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.mail().from());
            msg.setTo(usuario.getEmail());
            msg.setSubject("CA Achou? - código para redefinir sua senha");
            msg.setText("Olá, " + usuario.getNome() + "!\n\n"
                    + "Seu código para redefinir a senha do CA Achou? é: " + codigo + "\n"
                    + "Ele vale por " + validadeMinutos + " minutos.\n\n"
                    + "Se você não pediu a redefinição, ignore esta mensagem.");
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de recuperação para o RM {}", usuario.getRm(), e);
        }
    }
}
