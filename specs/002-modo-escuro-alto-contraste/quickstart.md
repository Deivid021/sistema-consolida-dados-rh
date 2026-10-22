# Guia de validação: Modo Escuro de Alto Contraste

Pré-requisitos: API rodando em `localhost:8080` com dados (ver o quickstart da funcionalidade
001) e o front em `frontend/` com `npm install`.

## 1. Verificações automáticas

```bash
cd frontend
npm run verificar:contraste        # pares de cor dos dois temas (FR-009 a FR-011, SC-002)
npm run dev                        # em outro terminal
npm run verificar:acessibilidade   # axe-core nas 6 telas, nos dois temas (SC-003)
npm run lint && npm run build
```

Todas devem terminar sem falhas.

## 2. Conferência manual

| # | Passo | Resultado esperado |
|---|-------|--------------------|
| 1 | Abrir `http://localhost:5173` com o sistema operacional em modo claro, numa janela anônima | Tema claro |
| 2 | No índice, em **Aparência**, escolher **Escuro** | A tela inteira fica escura na hora, sem recarregar; filtros e rolagem mantidos |
| 3 | Navegar por Panorama, Servidores, Salários, Entradas e saídas, Alertas e um endereço inexistente | Todas no tema escuro, gráficos legíveis, legendas e tabelas dos gráficos presentes |
| 4 | Recarregar a página (F5) | Abre direto no escuro, sem clarão |
| 5 | Usar só o teclado (Tab até **Aparência**, setas para trocar) | Foco visível; leitor de tela anuncia "Aparência" e o tema marcado |
| 6 | Janela anônima nova com o sistema operacional em modo escuro | Abre no escuro |
| 7 | Largura de 360 px (ferramentas do navegador) | Opção de aparência visível e sem rolagem horizontal, nos dois temas |
| 8 | Windows: Configurações → Acessibilidade → Temas de contraste | Textos, bordas, foco e marcas dos gráficos visíveis |
| 9 | Exportar CSV e PDF no tema escuro | Arquivos iguais aos do tema claro |
