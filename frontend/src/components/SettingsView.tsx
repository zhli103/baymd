import { useState, useEffect } from 'react'
import { Settings, Trash2, Shield, Activity } from 'lucide-react'
import { getSettings, clearMemory, getTraces } from '../api/baymd'

export default function SettingsView() {
  const [settings, setSettings] = useState<any>(null)
  const [traces, setTraces] = useState<any[]>([])
  const [memoryCleared, setMemoryCleared] = useState(false)

  useEffect(() => {
    getSettings().then(res => setSettings(res.data)).catch(() => {})
    getTraces(1, 5).then(res => setTraces(res.data?.records || [])).catch(() => {})
  }, [])

  const handleClearMemory = async () => {
    try {
      await clearMemory()
      setMemoryCleared(true)
      setTimeout(() => setMemoryCleared(false), 2000)
    } catch {}
  }

  return (
    <div className="flex-1 flex flex-col bg-surface">
      <header className="h-12 border-b border-border flex items-center px-4 shrink-0">
        <h1 className="text-sm font-semibold text-text-primary">系统设置</h1>
      </header>

      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        {/* RAG Config */}
        <section className="bg-panel border border-border rounded-xl p-4">
          <div className="flex items-center gap-2 mb-3">
            <Settings className="w-4 h-4 text-accent" />
            <h2 className="text-sm font-semibold">RAG 配置</h2>
          </div>
          {settings?.rag ? (
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div className="text-muted">向量引擎</div>
              <div>{settings.rag.vector?.type || 'pg'}</div>
              <div className="text-muted">向量维度</div>
              <div>{settings.rag.default?.dimension || 1536}</div>
              <div className="text-muted">查询改写</div>
              <div>{settings.rag.queryRewrite?.enabled ? '开启' : '关闭'}</div>
              <div className="text-muted">限流</div>
              <div>{settings.rag.rateLimit?.global?.enabled ? '开启' : '关闭'}</div>
            </div>
          ) : (
            <p className="text-xs text-muted">加载中...</p>
          )}
        </section>

        {/* Memory */}
        <section className="bg-panel border border-border rounded-xl p-4">
          <div className="flex items-center gap-2 mb-3">
            <Shield className="w-4 h-4 text-vital" />
            <h2 className="text-sm font-semibold">隐私与记忆</h2>
          </div>
          <button onClick={handleClearMemory}
            className="flex items-center gap-2 px-3 py-2 rounded-lg
              border border-vital text-vital text-xs hover:bg-vital/5 transition-colors">
            <Trash2 className="w-3.5 h-3.5" />
            {memoryCleared ? '记忆已清空' : '清空所有用户记忆'}
          </button>
          <p className="text-xs text-muted mt-2">
            清空后，系统将忘记所有关于您的事实和对话情节
          </p>
        </section>

        {/* Trace */}
        <section className="bg-panel border border-border rounded-xl p-4">
          <div className="flex items-center gap-2 mb-3">
            <Activity className="w-4 h-4 text-accent" />
            <h2 className="text-sm font-semibold">最近链路</h2>
          </div>
          {traces.length > 0 ? (
            <div className="space-y-2">
              {traces.map((t: any) => (
                <div key={t.traceId} className="flex items-center justify-between text-xs py-1 border-b border-border last:border-0">
                  <span className="text-muted font-mono">{t.traceId?.slice(0, 8)}</span>
                  <span className="text-muted">{t.totalDurationMs}ms</span>
                  <span className={`${t.status === 'SUCCESS' ? 'text-accent' : 'text-vital'}`}>
                    {t.status}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-muted">暂无链路数据</p>
          )}
        </section>
      </div>
    </div>
  )
}
