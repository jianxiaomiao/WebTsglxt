<template>
  <!-- ==================== 知识图谱面板 ==================== -->
  <Teleport to="body">
    <transition name="slide-right">
      <div
        v-if="visible"
        class="graph-side-panel glass-panel"
        :class="{ 'dark-mode': isDark }"
        :style="{ width: panelWidth + 'px' }"
        @click="closeGraphMenu"
      >
        <div class="panel-resizer" @mousedown="handleGraphResizeStart"></div>

        <div class="graph-header">
          <div style="display: flex; align-items: center; gap: 8px; overflow: hidden;">
            <span style="font-weight: bold; font-size: 14px; white-space: nowrap;">🐱 {{ bookName || '未知书籍' }}</span>
            <ElButton size="small" type="primary" plain round @click="showGraphSidebar = !showGraphSidebar">
              {{ showGraphSidebar ? '隐藏侧栏' : '添加组件' }}
            </ElButton>
          </div>
          <div class="graph-actions" style="flex-shrink: 0;">
            <ElButton size="small" @click="close" circle icon="Close" />
          </div>
        </div>

        <!-- 🔥 工具栏 -->
        <div class="graph-toolbar">
          <div class="toolbar-group">
            <ElButton size="small" @click="handleUndo" title="撤销 (Ctrl+Z)" circle>↩️</ElButton>
            <ElButton size="small" @click="handleRedo" title="重做 (Ctrl+Y)" circle>↪️</ElButton>
          </div>
          <div class="toolbar-group">
            <ElButton size="small" @click="handleZoomIn" title="放大" circle>➕</ElButton>
            <ElButton size="small" @click="handleZoomOut" title="缩小" circle>➖</ElButton>
          </div>
          <div class="toolbar-group">
            <ElButton size="small" type="warning" plain round @click="handleAutoLayout" title="AI智能拓扑梳理">✨ 智能理线</ElButton>
            <ElButton size="small" type="success" plain round @click="handleExportPNG" title="导出高清长图">📸 导出</ElButton>
            <ElButton size="small" type="danger" plain round @click="handleDeleteSelected" title="Backspace/Delete">🗑️ 删除</ElButton>
          </div>
        </div>

        <div class="graph-body" style="display: flex; flex: 1; overflow: hidden; position: relative;">
          <!-- 侧边栏 -->
          <transition name="slide-left">
            <div v-show="showGraphSidebar" class="graph-sidebar" :class="{ 'dark-mode': isDark }">
              <div class="sidebar-title">拖拽添加</div>
              <div class="node-shape rect" @mousedown="handleDndStart($event, 'rect')">矩形</div>
              <div class="node-shape circle" @mousedown="handleDndStart($event, 'circle')">圆形</div>
              <div class="node-shape ellipse" @mousedown="handleDndStart($event, 'ellipse')">椭圆</div>
              <div class="node-shape diamond" @mousedown="handleDndStart($event, 'polygon')">菱形</div>
            </div>
          </transition>

          <!-- 画布 -->
          <div id="x6-graph-container" ref="graphContainerRef" class="graph-container"
            tabindex="0" style="flex: 1; outline: none; height: 100%; width: 100%;"></div>

          <!-- 🔥 小地图 -->
          <div ref="minimapContainerRef" class="minimap-container glass-panel" :class="{ 'dark-mode': isDark }"></div>

          <!-- 🔥 右键菜单 -->
          <div
            v-if="showContextMenu" class="graph-context-menu glass-panel"
            :style="{ left: menuPos.x + 'px', top: menuPos.y + 'px' }" @click.stop
          >
            <div class="menu-item" @click="handleCopyCell">📄 复制选中节点</div>
            <div class="menu-item" @click="handlePasteCell">📋 粘贴节点 (Ctrl+V)</div>
            <div class="menu-divider"></div>
            <div class="menu-item danger" @click="handleDeleteSelected">🗑️ 删除该项</div>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>

  <!-- 打开按钮 -->
  <Teleport to="body">
    <ElButton
      v-show="!visible"
      class="graph-toggle-btn glass-button"
      circle size="large" @click="open"
      style="position: fixed; right: 30px; top: 50%; transform: translateY(-50%); z-index: 9000;"
    >🐱</ElButton>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import { Graph, Selection, Transform, Keyboard, History, Clipboard, Snapline, MiniMap, Export, Dnd } from '@antv/x6'
import { ElButton, ElMessage } from 'element-plus'
import { useUserStore } from '../stores/userStore'
import { usePetStore } from '../stores/petStore'
import request from '../utils/request'

// ====================== Props ======================
const props = defineProps({
  isbn: { type: String, default: '' },
  userId: { type: String, default: '' },
  bookName: { type: String, default: '' },
  panelWidth: { type: Number, default: 400 },
  isDark: { type: Boolean, default: false }
})

const emit = defineEmits(['update:panelWidth'])
const userStore = useUserStore()
const petStore = usePetStore()

// ====================== 状态 ======================
const visible = ref(false)
const graphContainerRef = ref(null)
const minimapContainerRef = ref(null)
const graphInstance = ref(null)
let dndInstance = null

const showGraphSidebar = ref(true)
const panelWidth = ref(userStore.graphPanelWidth || props.panelWidth || 400)
const isDark = computed(() => props.isDark)

// 右键菜单
const showContextMenu = ref(false)
const menuPos = ref({ x: 0, y: 0 })

// 防抖工具
function debounce(fn, delay = 250) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => { fn.apply(this, args); timer = null }, delay)
  }
}

// ====================== 节点类型枚举 ======================
const GRAPH_NODE_TYPE = {
  NOTE: 1, AI_IMAGE: 2, AI_SUMMARY: 3, AI_ROLE: 4,
  QUIZ: 5, BOOKMARK: 6, QA: 7, MANUAL: 0
}

const nodeColors = {
  1: '#e6f7ff', 2: '#f6ffed', 3: '#fff0f6', 4: '#fffbe6',
  5: '#f0f5ff', 6: '#fcffe6', 7: '#f9f0ff', 0: '#ffffff'
}

// ====================== 注册复合节点 ======================
Graph.registerNode('image-text-node', {
  width: 140, height: 140, resizing: true,
  markup: [
    { tagName: 'rect', selector: 'body' },
    { tagName: 'text', selector: 'label' },
    { tagName: 'image', selector: 'image' }
  ],
  attrs: {
    body: { rx: 8, ry: 8, stroke: '#409eff', fill: '#fff', width: 160, height: 140 },
    label: { refX: 8, refY: 18, fontSize: 13, textAnchor: 'start', fill: '#333' },
    image: { x: 0, y: 30, width: 160, height: 110, preserveAspectRatio: 'xMidYMid meet' }
  }
}, true)

// ====================== 连接桩配置 ======================
const commonPortsConfig = {
  groups: {
    absolute: {
      position: 'absolute',
      attrs: { circle: { r: 5, magnet: true, stroke: '#409eff', strokeWidth: 1, fill: '#fff', style: { visibility: 'hidden' } } }
    }
  },
  items: [
    { id: 'p_top', group: 'absolute', args: { x: '50%', y: 0 } },
    { id: 'p_bottom', group: 'absolute', args: { x: '50%', y: '100%' } },
    { id: 'p_left', group: 'absolute', args: { x: 0, y: '50%' } },
    { id: 'p_right', group: 'absolute', args: { x: '100%', y: '50%' } }
  ]
}

// ====================== 打开/关闭 ======================
const open = () => {
  visible.value = true
  nextTick(() => { initGraph(); loadGraphData() })
}
const close = () => { visible.value = false }
const closeGraphMenu = () => { showContextMenu.value = false }

watch(visible, (v) => {
  if (!v && graphInstance.value) {
    graphInstance.value.dispose()
    graphInstance.value = null
  }
})

// ====================== 面板拉伸 ======================
const handleGraphResizeStart = (e) => {
  e.preventDefault()
  const startX = e.clientX
  const startWidth = panelWidth.value
  const onMouseMove = (ev) => {
    panelWidth.value = Math.max(320, Math.min(window.innerWidth - 80, startWidth + (startX - ev.clientX)))
  }
  const onMouseUp = () => {
    if (userStore.updateGraphPanelWidth) userStore.updateGraphPanelWidth(panelWidth.value)
    emit('update:panelWidth', panelWidth.value)
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

watch([panelWidth, showGraphSidebar], () => {
  if (graphInstance.value && graphContainerRef.value) {
    nextTick(() => {
      const rect = graphContainerRef.value.getBoundingClientRect()
      graphInstance.value.resize(rect.width, rect.height)
    })
  }
})

// ====================== 工具栏方法 ======================
const handleUndo = () => graphInstance.value?.undo()
const handleRedo = () => graphInstance.value?.redo()
const handleZoomIn = () => graphInstance.value?.zoom(0.1)
const handleZoomOut = () => graphInstance.value?.zoom(-0.1)

const handleExportPNG = () => {
  if (!graphInstance.value) return
  graphInstance.value.exportPNG(`${props.bookName || '书籍'}_知识图谱.png`, {
    backgroundColor: isDark.value ? '#1e1e1e' : '#f5f7fa', padding: 30, quality: 1
  })
}

const handleCopyCell = () => {
  const cells = graphInstance.value?.getSelectedCells()
  if (cells?.length) { graphInstance.value?.copy(cells); ElMessage.success('已复制节点') }
  showContextMenu.value = false
}
const handlePasteCell = () => {
  if (!graphInstance.value?.isClipboardEmpty()) {
    const cells = graphInstance.value?.paste({ offset: 32 })
    graphInstance.value?.cleanSelection(); graphInstance.value?.select(cells)
  }
  showContextMenu.value = false
}

const handleDeleteSelected = () => {
  if (!graphInstance.value) return
  const cells = graphInstance.value.getSelectedCells()
  if (!cells || cells.length === 0) return

  for (const cell of cells) {
    const actionType = cell.isNode() ? 'node' : 'edge'
    // 🔥 乐观更新：画布上立即移除
    const cellId = parseInt(cell.id)
    cell.remove()
    // 后台异步同步后端（fire-and-forget）
    request.delete('/graph', { params: { action: actionType, id: cellId } }).catch(() => {})
  }
  showContextMenu.value = false
}

// ====================== 🔥 智能布局 ======================
const handleAutoLayout = () => {
  if (!graphInstance.value) return
  const nodes = graphInstance.value.getNodes()
  const edges = graphInstance.value.getEdges()
  if (nodes.length === 0) return

  const inDegree = new Map()
  const adj = new Map()
  nodes.forEach(n => { inDegree.set(n.id, 0); adj.set(n.id, []) })
  edges.forEach(e => {
    const src = e.getSourceCellId(); const tgt = e.getTargetCellId()
    if (inDegree.has(tgt)) inDegree.set(tgt, inDegree.get(tgt) + 1)
    if (adj.has(src)) adj.get(src).push(tgt)
  })

  const layers = []
  let currentLayer = nodes.filter(n => inDegree.get(n.id) === 0).map(n => n.id)
  const visited = new Set(currentLayer)
  if (currentLayer.length === 0 && nodes.length > 0) { currentLayer = [nodes[0].id]; visited.add(nodes[0].id) }

  while (currentLayer.length > 0) {
    layers.push(currentLayer)
    const nextLayer = []
    currentLayer.forEach(u => {
      (adj.get(u) || []).forEach(v => { if (!visited.has(v)) { visited.add(v); nextLayer.push(v) } })
    })
    currentLayer = nextLayer
  }
  const leftovers = nodes.filter(n => !visited.has(n.id)).map(n => n.id)
  if (leftovers.length) layers.push(leftovers)

  layers.forEach((layerIds, colIdx) => {
    layerIds.forEach((nodeId, rowIdx) => {
      const node = graphInstance.value.getCellById(nodeId)
      if (node && node.isNode()) {
        const targetX = 80 + colIdx * 240; const targetY = 80 + rowIdx * 130
        node.setPosition({ x: targetX, y: targetY })
        request.put('/graph?action=node', { id: parseInt(node.id), positionX: targetX, positionY: targetY }).catch(() => {})
      }
    })
  })
  ElMessage.success('🧠 图谱层级拓扑排版完毕！')
}

// ====================== 🔥 DnD 拖拽 ======================
const handleDndStart = (e, shapeType) => {
  if (!graphInstance.value || !dndInstance) return
  const labelText = shapeType === 'rect' ? '矩形节点' : shapeType === 'circle' ? '圆形节点' : shapeType === 'ellipse' ? '椭圆节点' : '菱形节点'
  const node = graphInstance.value.createNode({
    shape: shapeType, width: 150, height: 60, ports: commonPortsConfig,
    attrs: { body: { fill: '#fff', stroke: '#409eff', rx: 8, ry: 8 }, label: { text: labelText, fill: '#333' } },
    data: { contentHeader: labelText, content: '请双击编辑内容...', type: GRAPH_NODE_TYPE.MANUAL, _isFromDnd: true }
  })
  dndInstance.start(node, e)
}

// ====================== 1. 初始化画布 ======================
const initGraph = () => {
  if (graphInstance.value || !graphContainerRef.value) return

  graphInstance.value = new Graph({
    container: graphContainerRef.value,
    background: { color: 'transparent' },
    grid: { size: 10, visible: true, type: 'dot', args: { color: isDark.value ? '#555' : '#ddd' } },
    panning: { enabled: true, eventTypes: ['leftMouseDown', 'mouseWheel'], modifiers: 'alt|ctrl|shift' },
    mousewheel: { enabled: true, zoomAtMousePosition: true, modifiers: 'ctrl' },
    connecting: {
      snap: true, allowBlank: false, allowLoop: false, highlight: true,
      createEdge() {
        return graphInstance.value.createEdge({
          shape: 'edge',
          reconnectable: true,
          connector: { name: 'smooth' },
          attrs: { line: { stroke: isDark.value ? '#1890ff' : '#409eff', strokeWidth: 2, targetMarker: { name: 'block', width: 6, height: 6 } } }
        })
      }
    }
  })

  // 🔥 注入 8 大官方插件
  graphInstance.value.use(new Selection({ enabled: true, multiple: true, rubberband: true, showNodeSelectionBox: true }))
  graphInstance.value.use(new Transform({ resizing: { enabled: true, minWidth: 50, minHeight: 30, orthogonal: false } }))
  graphInstance.value.use(new Keyboard({ enabled: true }))
  graphInstance.value.use(new History({ enabled: true }))
  graphInstance.value.use(new Clipboard({ enabled: true }))
  graphInstance.value.use(new Snapline({ enabled: true }))
  graphInstance.value.use(new Export())
  graphInstance.value.use(new MiniMap({ container: minimapContainerRef.value, width: 130, height: 90, padding: 8 }))

  dndInstance = new Dnd({ target: graphInstance.value, scaled: false, animation: true })

  // 快捷键
  graphInstance.value.on('cell:click', () => { graphContainerRef.value?.focus() })
  graphInstance.value.on('blank:click', () => { graphContainerRef.value?.focus() })
  graphInstance.value.bindKey(['backspace', 'delete'], () => { handleDeleteSelected() })
  graphInstance.value.bindKey(['ctrl+z', 'meta+z'], () => { handleUndo() })
  graphInstance.value.bindKey(['ctrl+y', 'meta+y', 'ctrl+shift+z'], () => { handleRedo() })

  // 🔥 右键菜单
  graphInstance.value.on('node:contextmenu', ({ e, node }) => {
    e.preventDefault(); e.stopPropagation()
    const panelRect = document.querySelector('.graph-side-panel')?.getBoundingClientRect()
    if (panelRect) menuPos.value = { x: e.clientX - panelRect.left, y: e.clientY - panelRect.top }
    graphInstance.value.select(node)
    showContextMenu.value = true
  })

  // 悬停端口
  graphInstance.value.on('node:mouseenter', ({ node }) => {
    node.getPorts().forEach(port => node.setPortProp(port.id, 'attrs/circle/style/visibility', 'visible'))
    node.addTools({
      name: 'button',
      args: {
        markup: [
          { tagName: 'circle', selector: 'button', attrs: { r: 9, fill: '#ff4d4f', cursor: 'pointer' } },
          { tagName: 'path', selector: 'icon', attrs: { d: 'M -3 -3 L 3 3 M -3 3 L 3 -3', stroke: '#ffffff', strokeWidth: 2, cursor: 'pointer' } }
        ],
        x: '100%', y: 0, offset: { x: -5, y: 5 },
        onClick: ({ cell }) => {
          // 🔥 乐观更新：画布上立即移除，后端异步同步
          const cellId = parseInt(cell.id)
          cell.remove()
          ElMessage.success('节点已删除')
          request.delete('/graph', { params: { action: 'node', id: cellId } }).catch(() => {})
        }
      }
    })
  })
  graphInstance.value.on('node:mouseleave', ({ node }) => {
    node.getPorts().forEach(port => node.setPortProp(port.id, 'attrs/circle/style/visibility', 'hidden'))
    node.removeTools()
  })

  // 🔥 DnD 落地
  graphInstance.value.on('node:added', async ({ node }) => {
    const d = node.getData() || {}
    if (!d._isFromDnd) return
    delete d._isFromDnd
    const pos = node.getPosition(); const shape = node.shape
    const payload = {
      mapId: props.isbn, mapUser: props.userId, type: GRAPH_NODE_TYPE.MANUAL,
      contentHeader: d.contentHeader, content: d.content, positionX: pos.x, positionY: pos.y, style: shape
    }
    // 🔥 乐观更新：节点已在画布上（DnD 自动放入），后台异步获取真实ID
    request.post('/graph?action=node', payload).then(res => {
      if (res.code === 200 && res.data?.id) {
        const realId = String(res.data.id)
        node.setData({ ...d, type: 0 })
        // 尝试更新节点ID（X6 不直接支持改ID，用 remove+readd 但节点已可见无感知）
        try {
          const pos2 = node.getPosition()
          node.remove()
          graphInstance.value.addNode({
            id: realId, shape, x: pos2.x, y: pos2.y, width: 150, height: 60, ports: commonPortsConfig, resizing: true,
            attrs: {
              body: { fill: nodeColors[0], stroke: '#d9d9d9', rx: 8, ry: 8 },
              label: { text: `${d.contentHeader}\n${d.content}`, refX: 0.5, refY: 0.5, textAnchor: 'middle', textVerticalAnchor: 'middle', fontSize: 12, textWrap: { width: -20, height: -12, ellipsis: true } }
            },
            data: { contentHeader: d.contentHeader, content: d.content, type: 0 }
          })
        } catch { /* ID更新失败忽略，节点保持在画布 */ }
      }
    }).catch(() => {})
  })

  // 节点移动同步
  graphInstance.value.on('node:moved', debounce(async ({ node }) => {
    try {
      const pos = node.getPosition(); const nodeId = parseInt(node.id)
      if (!isNaN(nodeId)) await request.put('/graph?action=node', { id: nodeId, positionX: pos.x, positionY: pos.y })
    } catch (err) { console.error(err) }
  }, 250))

  // 节点缩放同步
  graphInstance.value.on('node:resized', debounce(async ({ node }) => {
    try {
      const size = node.getSize(); const pos = node.getPosition(); const nodeId = parseInt(node.id)
      if (isNaN(nodeId)) return
      await request.put('/graph?action=node', { id: nodeId, positionX: pos.x, positionY: pos.y, style: JSON.stringify({ shape: node.shape, width: size.width, height: size.height }) })
      if (node.shape === 'image-text-node') { node.attr('image/refWidth', '100%'); node.attr('image/refHeight', '100%') }
    } catch (err) { console.error(err) }
  }, 250))

  // 双击编辑
  graphInstance.value.on('node:dblclick', ({ node }) => {
    const nodeData = node.getData() || {}; const currentContent = nodeData.content || ''
    if (nodeData.type === GRAPH_NODE_TYPE.AI_IMAGE) { ElMessage.info('AI图片路径无法直接编辑修改'); return }
    const view = graphInstance.value.findViewByCell(node)
    if (!view) return
    const bbox = view.getBBox()
    const textarea = document.createElement('textarea')
    textarea.value = currentContent
    textarea.style.cssText = `
      position: absolute; z-index: 100000; resize: none; padding: 6px;
      left: ${bbox.x}px; top: ${bbox.y}px; width: ${bbox.width}px; height: ${bbox.height}px;
      font-size: 12px; line-height: 1.5; border: 2px solid #409eff; border-radius: 8px;
      outline: none; background: ${isDark.value ? '#1e1e1e' : '#ffffff'};
      color: ${isDark.value ? '#e5e7eb' : '#333333'}; box-shadow: 0 4px 16px rgba(0,0,0,0.25); box-sizing: border-box;
    `
    graphContainerRef.value.appendChild(textarea)
    textarea.focus(); textarea.select()
    const handleBlur = async () => {
      document.removeEventListener('mousedown', handleOutsideClick)
      const newValue = textarea.value.trim()
      if (newValue && newValue !== currentContent) {
        // 🔥 乐观更新：先更新画布显示，再异步同步后端
        node.setData({ ...nodeData, content: newValue })
        node.setAttrs({ label: { text: `${nodeData.contentHeader}\n${newValue}` } })
        request.put('/graph?action=node', { id: parseInt(node.id), content: newValue })
          .then(() => ElMessage.success('更新成功'))
          .catch(() => {})
      }
      if (textarea.parentNode) textarea.parentNode.removeChild(textarea)
    }
    textarea.addEventListener('blur', handleBlur)
    textarea.addEventListener('keydown', (e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); textarea.blur() } })
    const handleOutsideClick = (e) => { if (e.target !== textarea) textarea.blur() }
    setTimeout(() => { document.addEventListener('mousedown', handleOutsideClick) }, 0)
  })

  // 🔥 连线完成（带连接桩）
  graphInstance.value.on('edge:connected', async ({ edge }) => {
    const sourceNode = edge.getSourceNode(); const targetNode = edge.getTargetNode()
    if (!sourceNode || !targetNode || sourceNode.id === targetNode.id) { edge.remove(); return }
    const sourcePortId = edge.getSourcePortId() || null
    const targetPortId = edge.getTargetPortId() || null
    try {
      const res = await request.post('/graph?action=edge', {
        mapId: props.isbn, mapUser: props.userId,
        sourceNodeId: parseInt(sourceNode.id), targetNodeId: parseInt(targetNode.id),
        sourcePort: sourcePortId, targetPort: targetPortId, label: '关联'
      })
      if (res.code !== 200 || !res.data) { edge.remove(); return }
      const realEdgeId = String(res.data)
      edge.remove()
      const newEdge = graphInstance.value.createEdge({
        id: realEdgeId, shape: 'edge', label: '关联',
        reconnectable: true,
        source: sourcePortId ? { cell: sourceNode.id, port: sourcePortId } : { cell: sourceNode.id },
        target: targetPortId ? { cell: targetNode.id, port: targetPortId } : { cell: targetNode.id },
        connector: { name: 'smooth' },
        attrs: { line: { stroke: isDark.value ? '#1890ff' : '#409eff', strokeWidth: 2, targetMarker: { name: 'block', width: 6, height: 6 } } }
      })
      graphInstance.value.addEdge(newEdge)
    } catch { edge.remove() }
  })

  graphInstance.value.on('edge:mouseenter', ({ edge }) => {
    // 🔥 添加可拖拽端点工具：悬浮边时显示源/目标端点手柄，可拖到新端口
    edge.addTools([
      { name: 'source-arrowhead' },
      { name: 'target-arrowhead' }
    ])
  })
  graphInstance.value.on('edge:mouseleave', ({ edge }) => { edge.removeTools() })

  // 🔥 监听连线端点变更（用户拖拽线头到新连接桩），同步后端
  const syncEdgeTerminal = debounce(({ edge }) => {
    const edgeId = parseInt(edge.id)
    if (isNaN(edgeId)) return
    const sourcePort = edge.getSourcePortId() || null
    const targetPort = edge.getTargetPortId() || null
    request.put('/graph?action=edge', {
      id: edgeId, sourcePort, targetPort
    }).catch(() => {})
  }, 500)
  graphInstance.value.on('edge:change:source', syncEdgeTerminal)
  graphInstance.value.on('edge:change:target', syncEdgeTerminal)

  // 双击编辑连线标签
  graphInstance.value.on('edge:dblclick', ({ edge, e }) => {
    e.stopPropagation()
    const labels = edge.getLabels()
    const currentLabelText = labels[0]?.attrs?.text?.text || edge.label || '关联'
    const input = document.createElement('input')
    input.value = currentLabelText
    input.style.cssText = `
      position: fixed; z-index: 100000; padding: 4px 8px;
      left: ${e.clientX - 50}px; top: ${e.clientY - 15}px; width: 100px;
      font-size: 12px; border: 1px solid #409eff; border-radius: 4px;
      outline: none; background: #ffffff; color: #333333; box-shadow: 0 4px 12px rgba(0,0,0,0.2);
    `
    document.body.appendChild(input)
    input.focus(); input.select()
    const handleBlur = async () => {
      document.removeEventListener('mousedown', handleOutsideClick)
      const newValue = input.value.trim()
      if (newValue && newValue !== currentLabelText) {
        // 🔥 乐观更新：画布立即显示新标签，后台异步同步
        edge.setLabels([{ attrs: { text: { text: newValue } } }])
        request.put('/graph?action=edge', { id: parseInt(edge.id), label: newValue })
          .then(() => ElMessage.success('更新关联 ✨'))
          .catch(() => {})
      }
      if (input.parentNode) input.parentNode.removeChild(input)
    }
    input.addEventListener('blur', handleBlur)
    input.addEventListener('keydown', (evt) => { if (evt.key === 'Enter') input.blur() })
    const handleOutsideClick = (evt) => { if (evt.target !== input) input.blur() }
    setTimeout(() => { document.addEventListener('mousedown', handleOutsideClick) }, 0)
  })
}

// ====================== 2. 加载图谱数据 ======================
const loadGraphData = async () => {
  if (!props.isbn || !props.userId) return
  try {
    const res = await request.get('/graph', { params: { action: 'full', map_id: props.isbn, map_user: props.userId } })
    if (res.code === 200 && res.data) {
      const { nodes, edges } = res.data
      const x6Nodes = nodes.map(n => {
        let nodeShape = 'rect'; let customWidth = 150; let customHeight = 60
        if (n.style && n.style.startsWith('{')) {
          try { const cfg = JSON.parse(n.style); nodeShape = cfg.shape || 'rect'; customWidth = cfg.width || 150; customHeight = cfg.height || 60 } catch { }
        } else if (n.style) { nodeShape = n.style }
        let nodeConfig = {
          id: String(n.id), shape: nodeShape, x: n.positionX, y: n.positionY,
          width: customWidth, height: customHeight, ports: commonPortsConfig, resizing: true,
          attrs: {
            body: { fill: nodeColors[n.type] || '#fff', stroke: '#d9d9d9', rx: 8, ry: 8 },
            label: {
              text: `${n.contentHeader}\n${n.content || ''}`,
              refX: 0.5, refY: 0.5, textAnchor: 'middle', textVerticalAnchor: 'middle',
              fill: isDark.value ? '#e5e7eb' : '#333333', fontSize: 12,
              textWrap: { text: `${n.contentHeader}\n${n.content || ''}`, width: -20, height: -12, ellipsis: true }
            }
          },
          data: { contentHeader: n.contentHeader, content: n.content, type: n.type }
        }
        if (n.type === GRAPH_NODE_TYPE.AI_IMAGE) {
          nodeConfig.shape = 'image-text-node'; nodeConfig.width = 140; nodeConfig.height = 140
          nodeConfig.attrs.image = { 'xlink:href': n.content }; nodeConfig.attrs.label = { text: n.contentHeader, fill: '#333' }
        }
        if (nodeShape === 'circle') { nodeConfig.attrs.body.rx = '50%'; nodeConfig.attrs.body.ry = '50%' }
        else if (nodeShape === 'polygon') { nodeConfig.attrs.body.refPoints = '0,10 10,0 20,10 10,20' }
        return nodeConfig
      })

      // 🔥 边还原（带连接桩）
      const x6Edges = edges.map(e => ({
        id: String(e.id),
        reconnectable: true,
        source: e.sourcePort ? { cell: String(e.sourceNodeId), port: e.sourcePort } : { cell: String(e.sourceNodeId) },
        target: e.targetPort ? { cell: String(e.targetNodeId), port: e.targetPort } : { cell: String(e.targetNodeId) },
        label: e.label || '',
        connector: { name: 'smooth' },
        attrs: { line: { stroke: isDark.value ? '#1890ff' : '#409eff', strokeWidth: 2, targetMarker: { name: 'block', width: 6, height: 6 } } }
      }))
      graphInstance.value.fromJSON({ nodes: x6Nodes, edges: x6Edges })
    }
  } catch (error) { console.error('加载图谱失败:', error) }
}

// ====================== 3. 自动创建节点（供父组件调用） ======================
const createAutoGraphNode = async (type, header, content, posX, posY) => {
  if (!props.isbn || !props.userId) return null
  const newNodeData = {
    mapId: props.isbn, mapUser: props.userId, type, contentHeader: header, content,
    positionX: posX ?? (100 + Math.random() * 50), positionY: posY ?? (100 + Math.random() * 100), style: ''
  }
  try {
    const res = await request.post('/graph?action=node', newNodeData)
    return res.code === 200 ? Number(res.data.id) : null
  } catch { return null }
}

// ====================== 4. 自动创建连线（供父组件调用） ======================
const createAutoEdge = async (sourceId, targetId, label = '关联') => {
  if (!sourceId || !targetId || !props.isbn || !props.userId) return false
  try {
    await request.post('/graph?action=edge', { mapId: props.isbn, mapUser: props.userId, sourceNodeId: sourceId, targetNodeId: targetId, label })
    return true
  } catch { return false }
}

// ====================== 暴露给父组件 ======================
defineExpose({ GRAPH_NODE_TYPE, createAutoGraphNode, createAutoEdge, loadGraphData, open, close, visible })
</script>

<style scoped>
/* ==================== 面板 ==================== */
.graph-side-panel {
  position: fixed; top: 60px; right: 20px; width: 400px; height: calc(100vh - 80px);
  z-index: 9500; border-radius: 16px; display: flex; flex-direction: column; overflow: hidden;
  background: var(--glass-bg, rgba(255, 255, 255, 0.4));
  backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid var(--glass-border, rgba(255, 255, 255, 0.3));
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.15); transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.graph-side-panel.dark-mode { background: rgba(30, 30, 30, 0.6); border: 1px solid rgba(255, 255, 255, 0.1); box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5); }
.graph-header { padding: 14px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(0, 0, 0, 0.05); color: var(--el-text-color-primary); }
.graph-side-panel.dark-mode .graph-header { border-bottom: 1px solid rgba(255, 255, 255, 0.1); color: #e5e7eb; }

/* 工具栏 */
.graph-toolbar { padding: 8px 16px; display: flex; justify-content: space-between; align-items: center; background: rgba(255, 255, 255, 0.2); border-bottom: 1px solid rgba(0,0,0,0.05); }
.graph-side-panel.dark-mode .graph-toolbar { background: rgba(0, 0, 0, 0.2); border-bottom-color: rgba(255,255,255,0.08); }
.toolbar-group { display: flex; align-items: center; gap: 4px; }

.graph-container { flex: 1; width: 100%; position: relative; }
.panel-resizer { position: absolute; left: 0; top: 0; bottom: 0; width: 6px; cursor: ew-resize; z-index: 1000; background: transparent; }
.panel-resizer:hover { background: rgba(64, 158, 255, 0.4); }

/* 侧边栏 */
.graph-sidebar { width: 90px; background: rgba(255, 255, 255, 0.5); border-right: 1px solid rgba(0, 0, 0, 0.05); display: flex; flex-direction: column; align-items: center; padding: 10px 0; overflow-y: auto; flex-shrink: 0; }
.graph-sidebar.dark-mode { background: rgba(30, 30, 30, 0.4); border-right: 1px solid rgba(255, 255, 255, 0.1); }
.sidebar-title { font-size: 12px; color: #999; margin-bottom: 15px; }
.node-shape { width: 60px; height: 36px; border: 1px dashed #409eff; background: #fff; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #409eff; margin-bottom: 12px; cursor: grab; user-select: none; }
.node-shape:active { cursor: grabbing; }
.node-shape.circle { height: 60px; border-radius: 50%; }
.node-shape.ellipse { border-radius: 50% / 50%; }
.node-shape.diamond { transform: rotate(45deg); width: 40px; height: 40px; margin-bottom: 20px; margin-top: 10px; }
.graph-sidebar.dark-mode .node-shape { background: #374151; border-color: #66b1ff; color: #e5e7eb; }

/* 小地图 */
.minimap-container { position: absolute; right: 12px; bottom: 12px; width: 130px; height: 90px; border-radius: 8px; z-index: 900; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.15); background: rgba(255,255,255,0.7); border: 1px solid rgba(0,0,0,0.1); }
.minimap-container.dark-mode { background: rgba(20,20,20,0.7); border-color: rgba(255,255,255,0.15); }

/* 右键菜单 */
.graph-context-menu { position: absolute; z-index: 99999; width: 150px; border-radius: 8px; padding: 6px 0; background: rgba(255,255,255,0.85); backdrop-filter: blur(12px); box-shadow: 0 6px 16px rgba(0,0,0,0.15); border: 1px solid rgba(0,0,0,0.08); font-size: 13px; color: #333; }
.graph-side-panel.dark-mode .graph-context-menu { background: rgba(35,35,35,0.85); border-color: rgba(255,255,255,0.1); color: #eee; }
.menu-item { padding: 8px 14px; cursor: pointer; transition: background 0.2s; }
.menu-item:hover { background: rgba(64, 158, 255, 0.15); color: #409eff; }
.menu-item.danger:hover { background: rgba(255, 77, 79, 0.15); color: #ff4d4f; }
.menu-divider { height: 1px; background: rgba(0,0,0,0.06); margin: 4px 0; }
.graph-side-panel.dark-mode .menu-divider { background: rgba(255,255,255,0.08); }

/* 动画 */
.slide-left-enter-active, .slide-left-leave-active { transition: margin-left 0.3s ease, opacity 0.3s ease; }
.slide-left-enter-from, .slide-left-leave-to { margin-left: -90px; opacity: 0; }
.slide-right-enter-active, .slide-right-leave-active { transition: transform 0.4s ease, opacity 0.4s ease; }
.slide-right-enter-from, .slide-right-leave-to { transform: translateX(120%); opacity: 0; }
:deep(.x6-widget-transform) { border: 1px dashed #409eff !important; padding: 2px !important; z-index: 9999 !important; }
</style>
