import { useState, useEffect } from 'react'
import { Upload, FileText, Database, Search } from 'lucide-react'
import { listKnowledgeBases, listDocuments } from '../api/baymd'

export default function KnowledgeView() {
  const [kbs, setKbs] = useState<any[]>([])

  useEffect(() => {
    listKnowledgeBases().then(res => setKbs(res.data || [])).catch(() => {})
  }, [])

  return (
    <div className="flex-1 flex flex-col bg-surface">
      <header className="h-12 border-b border-border flex items-center px-4 shrink-0">
        <h1 className="text-sm font-semibold text-text-primary">知识库管理</h1>
      </header>

      <div className="flex-1 overflow-y-auto p-6">
        {kbs.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-muted">
            <Database className="w-12 h-12 mb-3" />
            <p className="text-sm">暂无知识库</p>
            <p className="text-xs mt-1">通过后端 API 创建知识库并上传文档</p>
          </div>
        ) : (
          <div className="grid gap-4">
            {kbs.map((kb: any) => (
              <div key={kb.id} className="bg-panel border border-border rounded-xl p-4">
                <div className="flex items-center gap-3 mb-3">
                  <Database className="w-5 h-5 text-accent" />
                  <div>
                    <h3 className="text-sm font-semibold">{kb.name || kb.id}</h3>
                    <p className="text-xs text-muted">{kb.description}</p>
                  </div>
                </div>
                <div className="flex gap-2">
                  <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg
                    bg-accent text-white text-xs hover:opacity-90 transition-opacity">
                    <Upload className="w-3.5 h-3.5" />
                    上传文档
                  </button>
                  <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg
                    border border-border text-text-primary text-xs hover:bg-surface transition-colors">
                    <Search className="w-3.5 h-3.5" />
                    查看文档
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
