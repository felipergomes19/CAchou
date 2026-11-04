package br.com.achou.api.security;

/** Identidade extraída do token JWT. */
public record UserPrincipal(Long id, String rm) {}
