<template>
  <div class="state-machine-designer">
    <div class="designer-header">
      <h1>State Machine Designer</h1>
      <div class="header-actions">
        <input
          v-model="machineName"
          placeholder="State Machine Name"
          class="machine-name-input"
        />
        <button @click="addState" class="btn btn-primary">Add State</button>
        <button @click="saveStateMachine" class="btn btn-success" :disabled="!canSave">
          Save State Machine
        </button>
      </div>
    </div>

    <div class="designer-toolbar">
      <div class="mode-selector">
        <label>
          <input type="radio" v-model="mode" value="move" />
          Move Mode
        </label>
        <label>
          <input type="radio" v-model="mode" value="transition" />
          Transition Mode
        </label>
      </div>
      <div class="instructions">
        <span v-if="mode === 'move'">Click and drag states to move them</span>
        <span v-if="mode === 'transition'"
          >Click a source state, then click a target state to create a transition</span
        >
      </div>
    </div>

    <div
      ref="canvas"
      class="canvas"
      @click="handleCanvasClick"
      @mousemove="handleMouseMove"
      @mouseup="handleMouseUp"
    >
      <svg class="connections-layer">
        <defs>
          <marker
            id="arrowhead"
            markerWidth="10"
            markerHeight="10"
            refX="9"
            refY="3"
            orient="auto"
          >
            <polygon points="0 0, 10 3, 0 6" fill="#666" />
          </marker>
        </defs>
        <line
          v-for="(transition, index) in transitions"
          :key="'transition-' + index"
          :x1="transition.x1"
          :y1="transition.y1"
          :x2="transition.x2"
          :y2="transition.y2"
          stroke="#666"
          stroke-width="2"
          marker-end="url(#arrowhead)"
          class="transition-line"
          @click.stop="selectTransition(index)"
        />
        <line
          v-if="tempTransition"
          :x1="tempTransition.x1"
          :y1="tempTransition.y1"
          :x2="tempTransition.x2"
          :y2="tempTransition.y2"
          stroke="#999"
          stroke-width="2"
          stroke-dasharray="5,5"
        />
        <text
          v-for="(transition, index) in transitions"
          :key="'transition-label-' + index"
          :x="(transition.x1 + transition.x2) / 2"
          :y="(transition.y1 + transition.y2) / 2 - 10"
          class="transition-label"
          @click.stop="editTransition(index)"
        >
          {{ transition.eventName || '?' }}
        </text>
      </svg>

      <div
        v-for="(state, index) in states"
        :key="'state-' + index"
        :class="[
          'state-node',
          state.type.toLowerCase(),
          { selected: selectedState === index },
        ]"
        :style="{ left: state.x + 'px', top: state.y + 'px' }"
        @mousedown.stop="handleStateMouseDown(index, $event)"
        @click.stop="handleStateClick(index)"
      >
        <div class="state-content">
          <input
            v-model="state.name"
            placeholder="State Name"
            class="state-name-input"
            @click.stop
          />
          <select v-model="state.type" class="state-type-select" @click.stop>
            <option value="START">Start</option>
            <option value="NORMAL">Normal</option>
            <option value="END">End</option>
          </select>
          <button @click.stop="removeState(index)" class="btn-remove">×</button>
        </div>
      </div>
    </div>

    <div v-if="selectedTransitionIndex !== null" class="transition-editor">
      <h3>Edit Transition</h3>
      <input
        v-model="transitions[selectedTransitionIndex].eventName"
        placeholder="Event Name"
        class="event-name-input"
      />
      <button @click="removeTransition(selectedTransitionIndex)" class="btn btn-danger">
        Delete Transition
      </button>
      <button @click="selectedTransitionIndex = null" class="btn btn-secondary">
        Close
      </button>
    </div>

    <div v-if="notification" class="notification" :class="notification.type">
      {{ notification.message }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useStateMachineStore } from '@/store/stateMachine'

const stateMachineStore = useStateMachineStore()

const machineName = ref('')
const states = ref([])
const transitions = ref([])
const mode = ref('move')
const selectedState = ref(null)
const selectedTransitionIndex = ref(null)
const draggedState = ref(null)
const dragOffset = ref({ x: 0, y: 0 })
const transitionSource = ref(null)
const tempTransition = ref(null)
const canvas = ref(null)
const notification = ref(null)

let stateCounter = 0

const canSave = computed(() => {
  return (
    machineName.value.trim() !== '' &&
    states.value.length > 0 &&
    states.value.some((s) => s.type === 'START')
  )
})

function addState() {
  states.value.push({
    name: `State${++stateCounter}`,
    type: 'NORMAL',
    x: 100 + states.value.length * 30,
    y: 100 + states.value.length * 30,
  })
}

function removeState(index) {
  states.value.splice(index, 1)
  transitions.value = transitions.value.filter(
    (t) => t.sourceIndex !== index && t.targetIndex !== index
  )
}

function handleStateMouseDown(index, event) {
  if (mode.value === 'move') {
    draggedState.value = index
    const state = states.value[index]
    const rect = canvas.value.getBoundingClientRect()
    dragOffset.value = {
      x: event.clientX - rect.left - state.x,
      y: event.clientY - rect.top - state.y,
    }
  }
}

function handleMouseMove(event) {
  if (draggedState.value !== null && mode.value === 'move') {
    const rect = canvas.value.getBoundingClientRect()
    states.value[draggedState.value].x = event.clientX - rect.left - dragOffset.value.x
    states.value[draggedState.value].y = event.clientY - rect.top - dragOffset.value.y
    updateTransitions()
  } else if (tempTransition.value) {
    const rect = canvas.value.getBoundingClientRect()
    tempTransition.value.x2 = event.clientX - rect.left
    tempTransition.value.y2 = event.clientY - rect.top
  }
}

function handleMouseUp() {
  draggedState.value = null
}

function handleCanvasClick() {
  selectedState.value = null
  selectedTransitionIndex.value = null
}

function handleStateClick(index) {
  if (mode.value === 'transition') {
    if (transitionSource.value === null) {
      transitionSource.value = index
      const state = states.value[index]
      tempTransition.value = {
        x1: state.x + 100,
        y1: state.y + 50,
        x2: state.x + 100,
        y2: state.y + 50,
      }
    } else {
      createTransition(transitionSource.value, index)
      transitionSource.value = null
      tempTransition.value = null
    }
  } else {
    selectedState.value = index
  }
}

function createTransition(sourceIndex, targetIndex) {
  if (sourceIndex === targetIndex) return

  const existing = transitions.value.find(
    (t) => t.sourceIndex === sourceIndex && t.targetIndex === targetIndex
  )
  if (existing) return

  const source = states.value[sourceIndex]
  const target = states.value[targetIndex]

  transitions.value.push({
    sourceIndex,
    targetIndex,
    sourceName: source.name,
    targetName: target.name,
    eventName: '',
    x1: source.x + 100,
    y1: source.y + 50,
    x2: target.x,
    y2: target.y + 50,
  })
}

function updateTransitions() {
  transitions.value.forEach((transition) => {
    const source = states.value[transition.sourceIndex]
    const target = states.value[transition.targetIndex]
    transition.sourceName = source.name
    transition.targetName = target.name
    transition.x1 = source.x + 100
    transition.y1 = source.y + 50
    transition.x2 = target.x
    transition.y2 = target.y + 50
  })
}

function selectTransition(index) {
  selectedTransitionIndex.value = index
}

function editTransition(index) {
  selectedTransitionIndex.value = index
}

function removeTransition(index) {
  transitions.value.splice(index, 1)
  selectedTransitionIndex.value = null
}

async function saveStateMachine() {
  const hasStart = states.value.some((s) => s.type === 'START')
  if (!hasStart) {
    showNotification('At least one START state is required', 'error')
    return
  }

  const invalidTransitions = transitions.value.filter((t) => !t.eventName)
  if (invalidTransitions.length > 0) {
    showNotification('All transitions must have an event name', 'error')
    return
  }

  const config = {
    name: machineName.value,
    states: states.value.map((s) => ({
      name: s.name,
      type: s.type,
    })),
    transitions: transitions.value.map((t) => ({
      sourceName: t.sourceName,
      targetName: t.targetName,
      eventName: t.eventName,
    })),
  }

  try {
    await stateMachineStore.createStateMachine(config)
    showNotification('State machine created successfully!', 'success')
  } catch (error) {
    const errorMessage = error.response?.data?.message || error.message || 'Error creating state machine'
    showNotification(`Error: ${errorMessage}`, 'error')
  }
}

function showNotification(message, type) {
  notification.value = { message, type }
  setTimeout(() => {
    notification.value = null
  }, 5000)
}
</script>

<style scoped>
.state-machine-designer {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.designer-header {
  background: white;
  padding: 1rem 2rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.designer-header h1 {
  margin: 0;
  font-size: 1.5rem;
}

.header-actions {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.machine-name-input {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  min-width: 250px;
}

.designer-toolbar {
  background: white;
  padding: 0.75rem 2rem;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mode-selector {
  display: flex;
  gap: 2rem;
}

.mode-selector label {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.instructions {
  color: #666;
  font-style: italic;
}

.canvas {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: linear-gradient(90deg, #f0f0f0 1px, transparent 1px),
    linear-gradient(#f0f0f0 1px, transparent 1px);
  background-size: 20px 20px;
}

.connections-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.transition-line {
  pointer-events: stroke;
  cursor: pointer;
}

.transition-label {
  font-size: 14px;
  fill: #333;
  pointer-events: all;
  cursor: pointer;
  font-weight: bold;
}

.state-node {
  position: absolute;
  width: 200px;
  background: white;
  border: 2px solid #333;
  border-radius: 8px;
  padding: 1rem;
  cursor: move;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: box-shadow 0.2s;
}

.state-node:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.state-node.selected {
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.2);
}

.state-node.start {
  border-color: #28a745;
  background: #f0fff4;
}

.state-node.end {
  border-color: #dc3545;
  background: #fff5f5;
}

.state-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.state-name-input {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  font-weight: bold;
}

.state-type-select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
}

.btn-remove {
  position: absolute;
  top: 0.25rem;
  right: 0.25rem;
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  font-size: 1.2rem;
  line-height: 1;
}

.btn-remove:hover {
  background: #c82333;
}

.transition-editor {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  min-width: 300px;
}

.transition-editor h3 {
  margin-top: 0;
  margin-bottom: 1rem;
}

.event-name-input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  margin-right: 0.5rem;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-primary:hover {
  background: #0056b3;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #218838;
}

.btn-success:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
}

.notification {
  position: fixed;
  top: 2rem;
  right: 2rem;
  padding: 1rem 1.5rem;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  z-index: 1000;
}

.notification.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.notification.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}
</style>

