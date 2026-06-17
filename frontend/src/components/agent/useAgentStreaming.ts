import { useCallback, useRef, useState } from 'react'
import { http } from '@/api/client'
import type { ChatSendPayload } from '@/api/modules/chat'
import { getStoredDeviceId } from '@/utils/device'
import { storage } from '@/utils/storage'
import { parseStreamingChunk, type AgentMessage } from './agentModel'

export function useAgentStreaming() {
  const [streaming, setStreaming] = useState(false)
  const [streamError, setStreamError] = useState<string | null>(null)
  const abortRef = useRef<AbortController | null>(null)

  const stop = useCallback(() => {
    abortRef.current?.abort()
    abortRef.current = null
    setStreaming(false)
  }, [])

  const sendStreamingMessage = useCallback(async (payload: ChatSendPayload, onDelta: (message: AgentMessage) => void) => {
    stop()
    const controller = new AbortController()
    abortRef.current = controller
    setStreaming(true)
    setStreamError(null)

    try {
      const response = await fetch(`${http.defaults.baseURL || ''}/api/chat/stream`, {
        method: 'POST',
        headers: buildStreamingHeaders(),
        body: JSON.stringify(payload),
        signal: controller.signal
      })
      if (!response.ok || !response.body) throw new Error(`Streaming failed: ${response.status}`)

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const chunks = buffer.split(/\n\n|\r\n\r\n/)
        buffer = chunks.pop() || ''
        for (const chunk of chunks) {
          const parsed = parseStreamingChunk(chunk)
          if (parsed?.content) onDelta(parsed as AgentMessage)
        }
      }
      const tail = parseStreamingChunk(buffer)
      if (tail?.content) onDelta(tail as AgentMessage)
    } catch (error) {
      if (!(error instanceof DOMException && error.name === 'AbortError')) {
        setStreamError(error instanceof Error ? error.message : 'Streaming failed')
        throw error
      }
    } finally {
      setStreaming(false)
      abortRef.current = null
    }
  }, [stop])

  return { streaming, streamError, sendStreamingMessage, stop }
}

function buildStreamingHeaders() {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = storage.getToken()
  const deviceId = getStoredDeviceId()
  if (token) headers.Authorization = `Bearer ${token}`
  if (deviceId) headers['X-Device-Id'] = deviceId
  return headers
}
