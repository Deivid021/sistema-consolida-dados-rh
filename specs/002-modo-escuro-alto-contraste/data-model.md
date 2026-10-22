# Modelo de dados: Modo Escuro de Alto Contraste

Esta funcionalidade não altera o banco de dados nem a API. O único dado novo fica no
navegador da pessoa.

## Preferência de aparência

| Campo | Onde fica | Valores | Regra |
|-------|-----------|---------|-------|
| `tema` | `localStorage` do navegador | `claro` ou `escuro` | Só existe depois que a pessoa escolhe um tema. Qualquer outro valor é ignorado. |
| `data-tema` | atributo do elemento `<html>` | `claro` ou `escuro` | Tema em uso na página. Sempre presente. |

**Como o tema em uso é decidido** (na abertura da página e quando o sistema operacional muda):

1. Se `tema` existe no navegador, vale ele (FR-006).
2. Senão, se o sistema operacional prefere escuro, vale `escuro` (FR-005).
3. Senão, vale `claro`.

**Transições**: escolher um tema grava `tema` e altera `data-tema` na hora (FR-003). Se o
navegador bloquear o armazenamento, a escolha vale até fechar a página.

**Privacidade**: não é dado pessoal, não sai do navegador e não é enviado ao servidor
(princípio I).
