import React, { useEffect, useMemo, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import {
  LogOut,
  MessageCircle,
  Radio,
  Search,
  Send,
  ShieldCheck,
  UserPlus,
  Users,
  Wifi,
  WifiOff
} from "lucide-react";
import "./styles.css";

const AUTH_URL = import.meta.env.VITE_AUTH_URL ?? "http://localhost:8081";
const CHAT_URL = import.meta.env.VITE_CHAT_URL ?? "http://localhost:8082";

const defaultRooms = [
  { id: "public", title: "Sala Publica", type: "group" },
  { id: "geral", title: "Geral", type: "group" },
  { id: "turma", title: "Turma", type: "group" }
];

function conversationsStorageKey(username) {
  return `chat-conversations:${username}`;
}

function conversationKey(conversation) {
  return `${conversation.type}:${conversation.id}`;
}

function formatTime(value) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return new Intl.DateTimeFormat("pt-BR", {
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

function loadStoredSession() {
  const raw = localStorage.getItem("chat-session");
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw);
    if (parsed?.username && parsed?.token) return parsed;
  } catch {
    localStorage.removeItem("chat-session");
  }

  return null;
}

function App() {
  const [session, setSession] = useState(loadStoredSession);
  const [mode, setMode] = useState("login");
  const [authForm, setAuthForm] = useState({ username: "", name: "", password: "" });
  const [authLoading, setAuthLoading] = useState(false);
  const [authMessage, setAuthMessage] = useState("");
  const [connected, setConnected] = useState(false);
  const [conversations, setConversations] = useState(defaultRooms);
  const [activeKey, setActiveKey] = useState(conversationKey(defaultRooms[0]));
  const [messagesByConversation, setMessagesByConversation] = useState({});
  const [historyError, setHistoryError] = useState("");
  const [recipient, setRecipient] = useState("");
  const [draft, setDraft] = useState("");
  const [filter, setFilter] = useState("");
  const stompRef = useRef(null);
  const listEndRef = useRef(null);

  const activeConversation = useMemo(
    () => conversations.find((item) => conversationKey(item) === activeKey) ?? conversations[0],
    [activeKey, conversations]
  );

  const messages = messagesByConversation[activeKey] ?? [];

  useEffect(() => {
    if (!session) return undefined;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${CHAT_URL}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${session.token}`
      },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        defaultRooms.forEach((room) => {
          client.subscribe(`/topic/${room.id}`, (frame) => addIncomingMessage(JSON.parse(frame.body)));
        });
        client.subscribe("/user/queue/messages", (frame) => addIncomingMessage(JSON.parse(frame.body)));
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
      onWebSocketClose: () => setConnected(false)
    });

    client.activate();
    stompRef.current = client;

    return () => {
      setConnected(false);
      client.deactivate();
      stompRef.current = null;
    };
  }, [session]);

  useEffect(() => {
    if (!session) return;

    const loadConversations = async () => {
      let savedPrivateConversations = [];

      try {
      const raw = localStorage.getItem(conversationsStorageKey(session.username));
      const saved = raw ? JSON.parse(raw) : [];
        savedPrivateConversations = Array.isArray(saved)
        ? saved.filter((item) => item?.id && item?.type === "private")
        : [];
      } catch {
        localStorage.removeItem(conversationsStorageKey(session.username));
      }

      try {
        const contacts = await request(`${CHAT_URL}/history/conversations/${encodeURIComponent(session.username)}`, {
          headers: {
            Authorization: `Bearer ${session.token}`
          }
        });

        const historyConversations = Array.isArray(contacts)
          ? contacts.map((contact) => ({
              id: contact,
              title: contact,
              type: "private"
            }))
          : [];

        const merged = [...savedPrivateConversations, ...historyConversations].filter(
          (conversation, index, all) =>
            all.findIndex((item) => conversationKey(item) === conversationKey(conversation)) === index
        );

        setConversations([...defaultRooms, ...merged]);
      } catch (error) {
        console.error("Erro ao carregar conversas privadas", error);
        setConversations([...defaultRooms, ...savedPrivateConversations]);
      }
    };

    loadConversations();
  }, [session]);

  useEffect(() => {
    if (!session) return;

    const privateConversations = conversations.filter((item) => item.type === "private");
    localStorage.setItem(conversationsStorageKey(session.username), JSON.stringify(privateConversations));
  }, [conversations, session]);

  useEffect(() => {
    if (!session || !activeConversation) return;
    loadHistory(activeConversation);
  }, [session, activeKey]);

  useEffect(() => {
    listEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length, activeKey]);

  function resolveMessageConversation(message) {
    if (message.receiver === "public" || defaultRooms.some((room) => room.id === message.receiver)) {
      return `group:${message.receiver}`;
    }

    const otherUser = message.sender === session?.username ? message.receiver : message.sender;
    ensurePrivateConversation(otherUser);
    return `private:${otherUser}`;
  }

  function addIncomingMessage(message) {
    const key = resolveMessageConversation(message);
    setMessagesByConversation((current) => ({
      ...current,
      [key]: [...(current[key] ?? []), message]
    }));
  }

  function ensurePrivateConversation(username) {
    const cleanUsername = username.trim();
    if (!cleanUsername || cleanUsername === session?.username) return null;

    const conversation = {
      id: cleanUsername,
      title: cleanUsername,
      type: "private"
    };

    setConversations((current) => {
      if (current.some((item) => conversationKey(item) === conversationKey(conversation))) {
        return current;
      }
      return [...current, conversation];
    });

    return conversation;
  }

  async function request(path, options = {}) {
    const response = await fetch(path, options);
    const text = await response.text();
    const data = text ? tryParseJson(text) : null;

    if (!response.ok) {
      const message = typeof data === "string" ? data : data?.message ?? text ?? "Erro na requisicao";
      throw new Error(`${response.status} ${message}`);
    }

    return data;
  }

  function tryParseJson(text) {
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }

  async function handleAuth(event) {
    event.preventDefault();
    setAuthLoading(true);
    setAuthMessage("");

    try {
      if (mode === "register") {
        await request(`${AUTH_URL}/auth/register`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            username: authForm.username.trim(),
            name: authForm.name.trim(),
            password: authForm.password
          })
        });
        setMode("login");
        setAuthMessage("Usuario criado. Entre para abrir o chat.");
        return;
      }

      const data = await request(`${AUTH_URL}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: authForm.username.trim(),
          password: authForm.password
        })
      });
      const nextSession = { username: authForm.username.trim(), token: data.token };
      localStorage.setItem("chat-session", JSON.stringify(nextSession));
      setSession(nextSession);
    } catch (error) {
      setAuthMessage(error.message);
    } finally {
      setAuthLoading(false);
    }
  }

  async function loadHistory(conversation) {
    const key = conversationKey(conversation);
    const path =
      conversation.type === "group"
        ? `${CHAT_URL}/history/group/${encodeURIComponent(conversation.id)}`
        : `${CHAT_URL}/history/private/${encodeURIComponent(session.username)}/${encodeURIComponent(conversation.id)}`;

    try {
      setHistoryError("");
      const history = await request(path, {
        headers: {
          Authorization: `Bearer ${session.token}`
        }
      });
      setMessagesByConversation((current) => ({
        ...current,
        [key]: Array.isArray(history) ? history : []
      }));
    } catch (error) {
      console.error("Erro ao carregar historico", error);
      setHistoryError(`Nao foi possivel carregar o historico: ${error.message}`);
      setMessagesByConversation((current) => ({
        ...current,
        [key]: current[key] ?? []
      }));
    }
  }

  function addPrivateConversation(event) {
    event.preventDefault();
    const conversation = ensurePrivateConversation(recipient);
    if (conversation) {
      setActiveKey(conversationKey(conversation));
      setRecipient("");
    }
  }

  function sendMessage(event) {
    event.preventDefault();
    const content = draft.trim();
    if (!content || !activeConversation || !stompRef.current?.connected) return;

    const message = {
      sender: session.username,
      receiver: activeConversation.id,
      content,
      type: "CHAT"
    };

    stompRef.current.publish({
      destination: "/app/chat.sendMessage",
      body: JSON.stringify(message)
    });

    setDraft("");

    if (activeConversation.type === "private") {
      setMessagesByConversation((current) => ({
        ...current,
        [activeKey]: [...(current[activeKey] ?? []), { ...message, timestamp: new Date().toISOString() }]
      }));
    }
  }

  function logout() {
    localStorage.removeItem("chat-session");
    setSession(null);
    setConnected(false);
    setMessagesByConversation({});
    setHistoryError("");
    setActiveKey(conversationKey(defaultRooms[0]));
  }

  if (!session) {
    return (
      <main className="auth-shell">
        <section className="auth-panel">
          <div className="brand-mark">
            <MessageCircle aria-hidden="true" />
          </div>
          <h1>Chat Distribuido</h1>
          <p>Entre com seu usuario para trocar mensagens privadas ou em salas compartilhadas.</p>

          <div className="mode-switch" role="tablist" aria-label="Modo de autenticacao">
            <button className={mode === "login" ? "active" : ""} onClick={() => setMode("login")} type="button">
              Login
            </button>
            <button className={mode === "register" ? "active" : ""} onClick={() => setMode("register")} type="button">
              Cadastro
            </button>
          </div>

          <form className="auth-form" onSubmit={handleAuth}>
            <label>
              Usuario
              <input
                autoComplete="username"
                minLength="3"
                onChange={(event) => setAuthForm({ ...authForm, username: event.target.value })}
                required
                value={authForm.username}
              />
            </label>
            {mode === "register" && (
              <label>
                Nome
                <input
                  autoComplete="name"
                  minLength="2"
                  onChange={(event) => setAuthForm({ ...authForm, name: event.target.value })}
                  required
                  value={authForm.name}
                />
              </label>
            )}
            <label>
              Senha
              <input
                autoComplete={mode === "login" ? "current-password" : "new-password"}
                minLength="4"
                onChange={(event) => setAuthForm({ ...authForm, password: event.target.value })}
                required
                type="password"
                value={authForm.password}
              />
            </label>
            {authMessage && <div className="notice">{authMessage}</div>}
            <button className="primary-button" disabled={authLoading} type="submit">
              <ShieldCheck aria-hidden="true" />
              {authLoading ? "Aguarde..." : mode === "login" ? "Entrar" : "Criar usuario"}
            </button>
          </form>
        </section>
      </main>
    );
  }

  const filteredConversations = conversations.filter((item) =>
    item.title.toLowerCase().includes(filter.trim().toLowerCase())
  );

  return (
    <main className="chat-shell">
      <aside className="sidebar">
        <header className="sidebar-header">
          <div>
            <span className="eyebrow">Usuario</span>
            <h1>{session.username}</h1>
          </div>
          <button aria-label="Sair" className="icon-button" onClick={logout} title="Sair" type="button">
            <LogOut aria-hidden="true" />
          </button>
        </header>

        <div className={`status-line ${connected ? "online" : "offline"}`}>
          {connected ? <Wifi aria-hidden="true" /> : <WifiOff aria-hidden="true" />}
          {connected ? "Conectado em tempo real" : "Reconectando..."}
        </div>

        <form className="new-chat" onSubmit={addPrivateConversation}>
          <label>
            Nova conversa privada
            <div className="input-with-button">
              <input
                onChange={(event) => setRecipient(event.target.value)}
                placeholder="usuario destino"
                value={recipient}
              />
              <button aria-label="Adicionar conversa" className="icon-button solid" title="Adicionar conversa" type="submit">
                <UserPlus aria-hidden="true" />
              </button>
            </div>
          </label>
        </form>

        <label className="search-box">
          <Search aria-hidden="true" />
          <input onChange={(event) => setFilter(event.target.value)} placeholder="Buscar conversa" value={filter} />
        </label>

        <nav className="conversation-list" aria-label="Conversas">
          {filteredConversations.map((conversation) => {
            const key = conversationKey(conversation);
            const isActive = key === activeKey;
            return (
              <button className={isActive ? "conversation active" : "conversation"} key={key} onClick={() => setActiveKey(key)} type="button">
                <span className="conversation-icon">
                  {conversation.type === "group" ? <Users aria-hidden="true" /> : <MessageCircle aria-hidden="true" />}
                </span>
                <span>
                  <strong>{conversation.title}</strong>
                  <small>{conversation.type === "group" ? "Conversa 1:N" : "Conversa 1:1"}</small>
                </span>
              </button>
            );
          })}
        </nav>
      </aside>

      <section className="chat-panel">
        <header className="chat-header">
          <div>
            <span className="eyebrow">{activeConversation.type === "group" ? "Sala" : "Privado"}</span>
            <h2>{activeConversation.title}</h2>
          </div>
          <div className="chat-kind">
            {activeConversation.type === "group" ? <Radio aria-hidden="true" /> : <MessageCircle aria-hidden="true" />}
            {activeConversation.type === "group" ? "1:N" : "1:1"}
          </div>
        </header>

        <div className="messages" aria-live="polite">
          {historyError ? (
            <div className="empty-state error-state">
              <MessageCircle aria-hidden="true" />
              <strong>Historico indisponivel</strong>
              <span>{historyError}</span>
            </div>
          ) : messages.length === 0 ? (
            <div className="empty-state">
              <MessageCircle aria-hidden="true" />
              <strong>Nenhuma mensagem ainda</strong>
              <span>Envie a primeira mensagem desta conversa.</span>
            </div>
          ) : (
            messages.map((message, index) => {
              const mine = message.sender === session.username;
              return (
                <article className={mine ? "message mine" : "message"} key={message.id ?? `${message.sender}-${message.timestamp}-${index}`}>
                  <div className="message-meta">
                    <strong>{mine ? "Voce" : message.sender}</strong>
                    <time>{formatTime(message.timestamp)}</time>
                  </div>
                  <p>{message.content}</p>
                </article>
              );
            })
          )}
          <div ref={listEndRef} />
        </div>

        <form className="composer" onSubmit={sendMessage}>
          <input
            disabled={!connected}
            onChange={(event) => setDraft(event.target.value)}
            placeholder={connected ? "Digite sua mensagem" : "Aguardando conexao com o chat"}
            value={draft}
          />
          <button aria-label="Enviar mensagem" className="primary-button send-button" disabled={!connected || !draft.trim()} title="Enviar mensagem" type="submit">
            <Send aria-hidden="true" />
            Enviar
          </button>
        </form>
      </section>
    </main>
  );
}

createRoot(document.getElementById("root")).render(<App />);
