# Service Template

Scaffold for a new service file. Copy + adapt.

## Public-only service

```typescript
import api from '@/utils/api'
import type { Foo, CreateFooRequest, UpdateFooRequest } from '@/types/{domain}.types'
import type { PagedResponse } from '@/types/common.types'

const fooService = {
  getAll: (): Promise<Foo[]> =>
    api.get('/api/v1/{domain}/foos').then(r => r.data),

  getById: (id: string): Promise<Foo> =>
    api.get(`/api/v1/{domain}/foos/${id}`).then(r => r.data),

  search: (query: string): Promise<Foo[]> =>
    api.get('/api/v1/{domain}/foos/search', { params: { query } }).then(r => r.data),

  paged: (page = 0, size = 20): Promise<PagedResponse<Foo>> =>
    api.get('/api/v1/{domain}/foos', { params: { page, size } }).then(r => r.data),

  create: (data: CreateFooRequest): Promise<Foo> =>
    api.post('/api/v1/{domain}/foos', data).then(r => r.data),

  update: (id: string, data: UpdateFooRequest): Promise<Foo> =>
    api.put(`/api/v1/{domain}/foos/${id}`, data).then(r => r.data),

  remove: (id: string): Promise<void> =>
    api.delete(`/api/v1/{domain}/foos/${id}`).then(() => undefined),
}

export default fooService
```

## Service with an admin section

```typescript
import api from '@/utils/api'
import type { Foo, CreateFooRequest, UpdateFooRequest } from '@/types/{domain}.types'

const fooService = {
  // Public
  getAll: (): Promise<Foo[]> =>
    api.get('/api/v1/{domain}/foos').then(r => r.data),

  // Admin
  adminGetAll: (): Promise<Foo[]> =>
    api.get('/api/v1/{domain}/admin/foos').then(r => r.data),

  adminCreate: (data: CreateFooRequest): Promise<Foo> =>
    api.post('/api/v1/{domain}/admin/foos', data).then(r => r.data),

  adminUpdate: (id: string, data: UpdateFooRequest): Promise<Foo> =>
    api.put(`/api/v1/{domain}/admin/foos/${id}`, data).then(r => r.data),

  adminDelete: (id: string): Promise<void> =>
    api.delete(`/api/v1/{domain}/admin/foos/${id}`).then(() => undefined),

  adminToggleStatus: (id: string): Promise<Foo> =>
    api.patch(`/api/v1/{domain}/admin/foos/${id}/toggle-status`).then(r => r.data),
}

export default fooService
```

## Anti-patterns previously present — do not reintroduce

### ❌ Unnecessary class-based service

```typescript
// Wrong — needless overhead
class FooService {
  async getAll() { ... }
}
export default new FooService()

// Right — an object literal is enough
const fooService = { getAll: () => ... }
export default fooService
```

### ❌ Inline types inside the service

```typescript
// Wrong
export interface Foo { id: string; name: string }
const fooService = { ... }

// Right — type lives in @/types/{domain}.types.ts; the service only imports
import type { Foo } from '@/types/{domain}.types'
```

### ❌ try/catch inside a service method

```typescript
// Wrong — the service shouldn't swallow errors
async getAll(): Promise<Foo[]> {
  try {
    const r = await api.get('/...')
    return r.data
  } catch (e) {
    console.error(e)
    return []
  }
}

// Right — let the error bubble up to store/component
getAll: (): Promise<Foo[]> =>
  api.get('/...').then(r => r.data)
```
