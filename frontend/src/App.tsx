import { useState, useCallback } from 'react'
import { MessageSquare, BookOpen, Settings, Brain } from 'lucide-react'
import Sidebar from './components/Sidebar'
import ChatView from './components/ChatView'
import KnowledgeView from './components/KnowledgeView'
import SettingsView from './components/SettingsView'

type Page = 'chat' | 'knowledge' | 'settings'

function App() {
  const [page, setPage] = useState<Page>('chat')
  const [conversationId, setConversationId] = useState<string | null>(null)
  const [deepThinking, setDeepThinking] = useState(false)

  const selectConversation = useCallback((id: string) => {
    setConversationId(id)
    setPage('chat')
  }, [])

  const newChat = useCallback(() => {
    setConversationId(null)
    setPage('chat')
  }, [])

  const navItems: { id: Page; label: string; icon: typeof MessageSquare }[] = [
    { id: 'chat', label: '对话', icon: MessageSquare },
    { id: 'knowledge', label: '知识库', icon: BookOpen },
    { id: 'settings', label: '设置', icon: Settings },
  ]

  return (
    <div className="flex h-screen bg-surface">
      {/* Navigation bar */}
      <nav className="w-14 bg-panel border-r border-border flex flex-col items-center py-4 gap-2 shrink-0">
        <div className="w-8 h-8 rounded-lg bg-accent flex items-center justify-center mb-4">
          <Brain className="w-5 h-5 text-white" />
        </div>
        {navItems.map(item => (
          <button
            key={item.id}
            onClick={() => setPage(item.id)}
            className={`w-9 h-9 rounded-lg flex items-center justify-center transition-colors
              ${page === item.id
                ? 'bg-accent-light text-accent'
                : 'text-muted hover:text-text-primary hover:bg-panel'}`}
            title={item.label}
          >
            <item.icon className="w-5 h-5" />
          </button>
        ))}
      </nav>

      {/* Main area */}
      <div className="flex flex-1 overflow-hidden">
        {page === 'chat' && (
          <>
            <Sidebar
              onSelect={selectConversation}
              onNew={newChat}
              activeId={conversationId}
            />
            <ChatView
              conversationId={conversationId}
              deepThinking={deepThinking}
              onToggleDeepThinking={() => setDeepThinking(d => !d)}
            />
          </>
        )}
        {page === 'knowledge' && <KnowledgeView />}
        {page === 'settings' && <SettingsView />}
      </div>
    </div>
  )
}

export default App
