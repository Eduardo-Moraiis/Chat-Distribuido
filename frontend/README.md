# Front-end do Chat Distribuido

Interface em React/Vite para integrar com os microsservicos do trabalho.

## Rodando com Docker Compose

Na pasta `Chat-Distribuido-main`, rode:

```bash
docker compose up frontend
```

O Vite abre em `http://localhost:5173`.

## Rodando com Node local

```bash
cd frontend
npm install
npm run dev -- --host
```

## Configuracao

Por padrao o front usa:

- Auth service: `http://localhost:8081`
- Chat service: `http://localhost:8082`

Para alterar:

```bash
VITE_AUTH_URL=http://localhost:8081 VITE_CHAT_URL=http://localhost:8082 npm run dev -- --host
```

## Fluxo implementado

- Cadastro e login via `/auth/register` e `/auth/login`.
- Conexao STOMP/SockJS em `/ws` com JWT no header `Authorization`.
- Historico de salas em `/history/group/{groupId}`.
- Historico privado em `/history/private/{user1}/{user2}`.
- Envio de mensagens para `/app/chat.sendMessage`.
- Salas 1:N: `public`, `geral` e `turma`.
- Conversas privadas 1:1 criadas pelo nome do usuario destino.
