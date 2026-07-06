import { useState, useEffect } from 'react'
import { Plus, Trash2, MessageSquare } from 'lucide-react'
import { listConversations, deleteConversation, Conversation } from '../api/baymd'

interface Props {
  onSelect: (id: string) => void
  onNew: () => void
  activeId: string | null
}

export default function Sidebar({ onSelect, onNew, activeId }: Props) {
  const [conversations, setConversations] = useState<Conversation[]>([])

  useEffect(() => {
    listConversations().then(setConversations).catch(() => {})
  }, [activeId])

  const handleDelete = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation()
    await deleteConversation(id)
    setConversations(prev => prev.filter(c => c.conversationId !== id))
    if (activeId === id) onNew()
  }

  return (
    <aside className="w-64 bg-panel border-r border-border flex flex-col shrink-0">
      <div className="p-3 border-b border-border">
        <button
          onClick={onNew}
          className="w-full flex items-center gap-2 px-3 py-2 rounded-lg
            bg-accent text-white text-sm font-medium
            hover:opacity-90 transition-opacity"
        >
          <Plus className="w-4 h-4" />
          新对话
        </button>
      </div>
      <div className="flex-1 overflow-y-auto">
        {conversations.length === 0 ? (
          <p className="p-4 text-sm text-muted text-center">
            暂无对话记录
          </p>
        ) : (
          conversations.map(conv => (
            <button
              key={conv.conversationId}
              onClick={() => onSelect(conv.conversationId)}
              className={`w-full text-left px-3 py-2.5 flex items-center gap-2
                text-sm transition-colors group
                ${activeId === conv.conversationId
                  ? 'bg-accent-light text-accent border-r-2 border-accent'
                  : 'text-text-primary hover:bg-surface'}`}
            >
              <MessageSquare className="w-4 h-4 shrink-0 text-muted" />
              <span className="truncate flex-1">{conv.title || '新对话'}</span>
              <Trash2
                className="w-3.5 h-3.5 text-muted opacity-0 group-hover:opacity-100
                  hover:text-vital transition-all shrink-0"
                onClick={e => handleDelete(e, conv.conversationId)}
              />
            </button>
          ))
        )}
      </div>
    </aside>
  )
}
