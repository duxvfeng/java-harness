# Vue Code Standards - Vue Style Guide

## Overview

This reference document describes Vue.js code standards based on the official Vue.js Style Guide and best practices for component development within the harness-review skill.

## Standards Source

**Vue.js Official Style Guide**
- Maintained by: Vue.js Core Team
- Latest version: Vue 3.x compatible
- Coverage: Component structure, naming conventions, best practices, and patterns
- Reference: [Vue Style Guide](https://vuejs.org/style-guide/)

## Integration Method

The harness-review skill automatically applies Vue standards when reviewing Vue component files (`.vue` extensions).

## Standards Categories

### 1. Component Naming

#### Multi-word Component Names
- **Rule A**: **Required** - Component names should always be multi-word
- **Reason**: Prevents conflicts with existing and future HTML elements
- **Example**:

```vue
<!-- ❌ Bad -->
<template>
  <TodoItem />
</template>

<!-- ✅ Good -->
<template>
  <TodoListItem />
</template>
```

#### Component Name Casing
- **Rule B**: **Recommended** - Use PascalCase for component names in templates
- **Reason**: Improves readability and consistency
- **Example**:

```vue
<!-- ❌ Bad -->
<template>
  <todo-list-item />
</template>

<!-- ✅ Good -->
<template>
  <TodoListItem />
</template>
```

### 2. Component Structure

#### Template Structure
- **Rule C**: **Recommended** - Organize template with logical grouping
- Use consistent indentation
- Group related elements
- Use comments for complex sections

```vue
<template>
  <div class="user-profile">
    <!-- Header Section -->
    <header class="profile-header">
      <UserAvatar :user="user" />
      <UserInfo :user="user" />
    </header>

    <!-- Content Section -->
    <main class="profile-content">
      <UserStats :stats="user.stats" />
      <UserActivity :activities="user.activities" />
    </main>
  </div>
</template>
```

#### Script Structure
- **Rule D**: **Recommended** - Organize script with consistent order
1. Component name
2. Props
3. Data/Computed
4. Methods
5. Lifecycle hooks
6. Watchers

```vue
<script setup>
import { ref, computed, onMounted } from 'vue'

// Props
const props = defineProps({
  userId: {
    type: String,
    required: true
  }
})

// Data
const user = ref(null)
const loading = ref(false)

// Computed
const userName = computed(() => user.value?.name || '')

// Methods
const fetchUser = async () => {
  loading.value = true
  try {
    user.value = await getUserById(props.userId)
  } finally {
    loading.value = false
  }
}

// Lifecycle
onMounted(() => {
  fetchUser()
})
</script>
```

### 3. Props Definition

#### Prop Naming
- **Rule E**: **Recommended** - Use camelCase for prop names
- **Reason**: Follows JavaScript conventions and avoids HTML attribute conflicts

```vue
<script setup>
// ❌ Bad
defineProps({
  'user-id': String,
  'post-count': Number
})

// ✅ Good
defineProps({
  userId: String,
  postCount: Number
})
</script>
```

#### Prop Validation
- **Rule F**: **Recommended** - Always validate props with types and defaults

```vue
<script setup>
const props = defineProps({
  user: {
    type: Object,
    required: true,
    validator: (value) => {
      return value.id && value.name
    }
  },
  theme: {
    type: String,
    default: 'light',
    validator: (value) => ['light', 'dark'].includes(value)
  }
})
</script>
```

### 4. Data and Computed Properties

#### Data Definition
- **Rule G**: **Recommended** - Use function syntax for data (Options API)
- **Reason**: Ensures each component instance gets its own data object

```vue
<script>
// ❌ Bad
export default {
  data: {
    count: 0,
    message: ''
  }
}

// ✅ Good
export default {
  data() {
    return {
      count: 0,
      message: ''
    }
  }
}
</script>
```

#### Computed vs Methods
- **Rule H**: **Recommended** - Use computed properties for derived data
- **Reason**: Computed properties are cached and more efficient

```vue
<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: Array
})

// ❌ Bad - Using method when computed would be better
const totalPrice = () => {
  return props.items.reduce((sum, item) => sum + item.price, 0)
}

// ✅ Good - Using computed
const totalPrice = computed(() => {
  return props.items.reduce((sum, item) => sum + item.price, 0)
})
</script>
```

### 5. Event Handling

#### Event Naming
- **Rule I**: **Recommended** - Use kebab-case for event names
- **Reason**: Follows HTML attribute conventions

```vue
<script setup>
// ❌ Bad
const emit = defineEmits(['userUpdated', 'dataChanged'])

// ✅ Good
const emit = defineEmits(['user-updated', 'data-changed'])
</script>
```

#### Event Validation
- **Rule J**: **Recommended** - Define events with validation

```vue
<script setup>
const emit = defineEmits({
  'user-updated': (user) => {
    if (!user.id) {
      console.warn('User update event requires user.id')
      return false
    }
    return true
  }
})
</script>
```

### 6. v-for Usage

#### Key Attribute
- **Rule K**: **Required** - Always use `:key` with `v-for`
- **Reason**: Enables efficient DOM updates

```vue
<template>
  <!-- ❌ Bad -->
  <div v-for="item in items">
    {{ item.name }}
  </div>

  <!-- ✅ Good -->
  <div v-for="item in items" :key="item.id">
    {{ item.name }}
  </div>
</template>
```

#### v-for with v-if
- **Rule L**: **Recommended** - Avoid using `v-if` and `v-for` on same element
- **Reason**: Causes performance issues and logic conflicts

```vue
<template>
  <!-- ❌ Bad -->
  <div v-for="user in users" v-if="user.active" :key="user.id">
    {{ user.name }}
  </div>

  <!-- ✅ Good - Use computed property -->
  <div v-for="user in activeUsers" :key="user.id">
    {{ user.name }}
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  users: Array
})

const activeUsers = computed(() =>
  props.users.filter(user => user.active)
)
</script>
```

### 7. Component Registration

#### Global Registration
- **Rule M**: **Recommended** - Avoid global component registration
- **Reason**: Prevents naming conflicts and improves tree-shaking

```javascript
// ❌ Bad - Global registration
app.component('BaseButton', BaseButton)
app.component('BaseInput', BaseInput)

// ✅ Good - Local registration
import BaseButton from './components/BaseButton.vue'
```

### 8. Style Organization

#### Scoped Styles
- **Rule N**: **Recommended** - Use scoped styles for component-specific CSS
- **Reason**: Prevents style leakage and conflicts

```vue
<style scoped>
.button {
  background: blue;
}
</style>
```

#### CSS Module Organization
- **Rule O**: **Recommended** - Group styles logically
- Use CSS custom properties for theming
- Follow BEM or similar naming conventions

```vue
<style scoped>
/* CSS Custom Properties */
:root {
  --primary-color: #3498db;
  --border-radius: 4px;
}

/* Component Styles */
.button {
  background: var(--primary-color);
  border-radius: var(--border-radius);
}

/* Modifiers */
.button--large {
  padding: 12px 24px;
}
</style>
```

### 9. Composition API Best Practices

#### Composables
- **Rule P**: **Recommended** - Extract reusable logic into composables
- **Reason**: Promotes code reuse and testing

```javascript
// composables/useUser.js
import { ref, computed } from 'vue'

export function useUser(userId) {
  const user = ref(null)
  const loading = ref(false)

  const fetchUser = async () => {
    loading.value = true
    try {
      user.value = await getUserById(userId)
    } finally {
      loading.value = false
    }
  }

  const userName = computed(() => user.value?.name || '')

  return {
    user,
    loading,
    userName,
    fetchUser
  }
}
```

#### Reactive References
- **Rule Q**: **Recommended** - Use `ref()` for primitives and `reactive()` for objects
- **Reason**: Follows Vue 3 reactivity best practices

```vue
<script setup>
import { ref, reactive } from 'vue'

// Primitives
const count = ref(0)
const message = ref('')

// Objects
const user = reactive({
  id: '',
  name: '',
  email: ''
})
</script>
```

### 10. TypeScript Integration

#### Type Definitions
- **Rule R**: **Recommended** - Use TypeScript for type safety
- **Reason**: Catches errors at development time

```vue
<script setup lang="ts">
interface User {
  id: string
  name: string
  email: string
}

interface Props {
  userId: string
  theme?: 'light' | 'dark'
}

const props = defineProps<Props>()

const user = ref<User | null>(null)

const fetchUser = async (): Promise<void> => {
  user.value = await getUserById(props.userId)
}
</script>
```

## Rule Severity Levels

| Level | Meaning | Examples |
|-------|---------|-----------|
| **【强制】** (Required) | Must be followed | Multi-word component names, :key with v-for |
| **【推荐】** (Recommended) | Should be followed | Consistent naming, prop validation, computed properties |
| **【参考】** (Reference) | Guidelines | Specific organization preferences, style choices |

## Common Patterns and Anti-Patterns

### ✅ Good Practices

```vue
<template>
  <div class="user-card">
    <img :src="user.avatar" :alt="user.name" />
    <h3>{{ user.name }}</h3>
    <p>{{ user.email }}</p>
    <BaseButton @click="handleEdit" :disabled="loading">
      Edit User
    </BaseButton>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import BaseButton from './BaseButton.vue'

interface Props {
  userId: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'user-updated': [user: User]
}>()

const user = ref<User | null>(null)
const loading = ref(false)

const userName = computed(() => user.value?.name || 'Unknown')

const fetchUser = async () => {
  loading.value = true
  try {
    user.value = await getUserById(props.userId)
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  // Edit logic
}

onMounted(() => {
  fetchUser()
})
</script>

<style scoped>
.user-card {
  padding: 1rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>
```

### ❌ Anti-Patterns

```vue
<!-- Bad: Single-word component name -->
<template>
  <button>
    {{ label }}
  </button>
</template>

<!-- Bad: No key with v-for -->
<template>
  <div v-for="item in items">
    {{ item.name }}
  </div>
</template>

<!-- Bad: v-if and v-for on same element -->
<template>
  <div v-for="item in items" v-if="item.active">
    {{ item.name }}
  </div>
</template>

<script>
// Bad: Not using function for data
export default {
  data: {
    count: 0
  }
}
</script>
```

## Configuration

Vue standards integration can be configured via `.claude/config/code-standards.config.json`:

```json
{
  "languageMapping": {
    "vue": {
      "standards": ["vue-style-guide"],
      "extensions": [".vue"],
      "defaultSeverity": "moderate",
      "reviewScope": "component"
    }
  }
}
```

## Integration with Vue Ecosystem

### Development Tools
- **Volar**: VS Code extension for Vue 3
- **Vue DevTools**: Browser extension for debugging
- **ESLint**: Official Vue plugin for linting
- **Prettier**: Code formatting

### Build Tools
- **Vite**: Recommended build tool for Vue 3
- **Vue CLI**: Official CLI tool (being phased out)
- **Nuxt**: Full-stack Vue framework

## Performance Considerations

- Use computed properties for expensive calculations
- Implement lazy loading for components
- Optimize list rendering with proper keys
- Use functional components for simple display logic
- Implement pagination or virtual scrolling for long lists

## Testing Vue Components

### Unit Testing
```javascript
import { mount } from '@vue/test-utils'
import UserCard from './UserCard.vue'

describe('UserCard', () => {
  it('displays user name', () => {
    const wrapper = mount(UserCard, {
      props: {
        userId: '123'
      }
    })

    expect(wrapper.text()).toContain('John Doe')
  })
})
```

### Best Practices
- Test component behavior, not implementation details
- Use shallow mounting for isolated tests
- Test user interactions and events
- Mock external dependencies

## Security Considerations

- Sanitize user input before displaying
- Use Vue's built-in XSS protection
- Validate props from external sources
- Be careful with `v-html` directive
- Implement proper authentication in API calls

## References

- [Vue.js Official Style Guide](https://vuejs.org/style-guide/)
- [Vue 3 Documentation](https://vuejs.org/)
- [Vue Test Utils](https://test-utils.vuejs.org/)
- [Volar Documentation](https://github.com/vuejs/language-tools)
- Related documents: See `architecture.md` for overall multilingual standards architecture

## Future Enhancements

Potential improvements:
1. Pinia state management standards
2. Vue Router best practices
3. Composition API patterns library
4. Performance optimization guidelines
5. Accessibility (A11y) standards for Vue components
