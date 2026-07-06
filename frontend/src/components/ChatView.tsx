import { useState, useEffect, useRef, useCallback } from 'react'
import { Send, Brain, ThumbsUp, ThumbsDown, StopCircle } from 'lucide-react'
import { streamChat, getMessages, submitFeedback, Message } from '../api/baymd'

interface Props {
  conversationId: string | null
  deepThinking: boolean
  onToggleDeepThinking: () => void
}

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  thinking?: string
  messageId?: string
}

export default function ChatView({ conversationId, deepThinking, onToggleDeepThinking }: Props) {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [streaming, setStreaming] = useState('')
  const [thinking, setThinking] = useState('')
  const abortRef = useRef<AbortController | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages, streaming, thinking])

  // Load history when conversation changes
  useEffect(() => {
    if (!conversationId) { setMessages([]); return }
    getMessages(conversationId).then(msgs =>
      setMessages(msgs.map(m => ({
        id: m.id,
        role: m.role,
        content: m.content,
        thinking: m.thinkingContent,
        messageId: m.id
      })))
    ).catch(() => {})
  }, [conversationId])

  const handleSend = useCallback(() => {
    if (!input.trim() || loading) return
    const question = input.trim()
    setInput('')
    setLoading(true)
    setStreaming('')
    setThinking('')

    const userMsg: ChatMessage = { id: Date.now().toString(), role: 'user', content: question }
    setMessages(prev => [...prev, userMsg])

    let content = ''
    let think = ''
    let metaReceived = false

    abortRef.current = streamChat(
      question, conversationId, deepThinking,
      (meta) => { metaReceived = true },
      (type, delta) => {
        if (type === 'think') { think += delta; setThinking(think) }
        else { content += delta; setStreaming(content) }
      },
      (payload) => {
        setLoading(false)
        const assistantMsg: ChatMessage = {
          id: payload.messageId || Date.now().toString(),
          role: 'assistant',
          content: content || '',
          thinking: think || undefined,
          messageId: payload.messageId
        }
        setMessages(prev => [...prev, assistantMsg])
        setStreaming('')
        setThinking('')
      },
      (err) => {
        setLoading(false)
        setStreaming('')
        setMessages(prev => [...prev, {
          id: Date.now().toString(), role: 'assistant',
          content: `请求失败: ${err.message}`
        }])
      }
    )
  }, [input, loading, conversationId, deepThinking])

  const handleStop = () => {
    abortRef.current?.abort()
    setLoading(false)
  }

  const handleFeedback = async (msgId: string | undefined, vote: 1 | -1) => {
    if (!msgId) return
    await submitFeedback(msgId, vote)
  }

  return (
    <div className="flex-1 flex flex-col bg-surface">
      {/* Header */}
      <header className="h-12 border-b border-border flex items-center justify-between px-4 shrink-0">
        <h1 className="text-sm font-semibold text-text-primary">
          {conversationId ? '对话详情' : '新对话'}
        </h1>
        <button
          onClick={onToggleDeepThinking}
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium transition-colors
            ${deepThinking ? 'bg-accent-light text-accent' : 'text-muted hover:text-text-primary'}`}
        >
          <Brain className="w-3.5 h-3.5" />
          深度思考
        </button>
      </header>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-4 py-3 space-y-4">
        {messages.length === 0 && !loading && (
          <div className="flex flex-col items-center justify-center h-full text-muted">
            <Brain className="w-12 h-12 mb-3 animate-clinical-pulse" />
            <p className="text-sm">BayMD 医疗助手</p>
            <p className="text-xs mt-1">输入您的问题开始对话</p>
          </div>
        )}

        {messages.map(msg => (
          <div key={msg.id} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[70%] rounded-xl px-4 py-3 ${
              msg.role === 'user'
                ? 'bg-accent text-white'
                : 'bg-panel border border-border'
            }`}>
              {msg.thinking && (
                <details className="mb-2">
                  <summary className="text-xs text-muted cursor-pointer">思考过程</summary>
                  <p className="text-xs text-muted mt-1 whitespace-pre-wrap">{msg.thinking}</p>
                </details>
              )}
              <p className="text-sm whitespace-pre-wrap leading-relaxed">{msg.content}</p>
              {msg.role === 'assistant' && msg.messageId && (
                <div className="flex gap-1 mt-2 pt-2 border-t border-border">
                  <button onClick={() => handleFeedback(msg.messageId, 1)}
                    className="p-1 text-muted hover:text-accent transition-colors">
                    <ThumbsUp className="w-3.5 h-3.5" />
                  </button>
                  <button onClick={() => handleFeedback(msg.messageId, -1)}
                    className="p-1 text-muted hover:text-vital transition-colors">
                    <ThumbsDown className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}
            </div>
          </div>
        ))}

        {/* Streaming */}
        {(streaming || thinking) && (
          <div className="flex justify-start">
            <div className="max-w-[70%] rounded-xl px-4 py-3 bg-panel border border-border">
              {thinking && (
                <div className="mb-2 animate-fade-in">
                  <p className="text-xs text-accent font-medium mb-1">
                    <span className="inline-block w-2 h-2 rounded-full bg-accent animate-clinical-pulse mr-1.5" />
                    思考中...
                  </p>
                  <p className="text-xs text-muted whitespace-pre-wrap">{thinking}</p>
                </div>
              )}
              {streaming && (
                <p className="text-sm whitespace-pre-wrap leading-relaxed animate-fade-in">
                  {streaming}
                  <span className="inline-block w-1.5 h-4 bg-accent ml-0.5 animate-clinical-pulse" />
                </p>
              )}
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div className="p-3 border-t border-border bg-surface">
        <div className="flex gap-2">
          <input
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && !e.shiftKey && handleSend()}
            placeholder="输入医疗问题..."
            disabled={loading}
            className="flex-1 px-4 py-2.5 rounded-xl border border-border
              bg-white text-sm text-text-primary placeholder:text-muted
              focus:outline-none focus:ring-2 focus:ring-accent/20 focus:border-accent
              disabled:opacity-50"
          />
          {loading ? (
            <button onClick={handleStop}
              className="px-3 py-2.5 rounded-xl bg-vital text-white hover:opacity-90 transition-opacity">
              <StopCircle className="w-5 h-5" />
            </button>
          ) : (
            <button onClick={handleSend} disabled={!input.trim()}
              className="px-4 py-2.5 rounded-xl bg-accent text-white hover:opacity-90
                disabled:opacity-40 transition-opacity">
              <Send className="w-5 h-5" />
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
