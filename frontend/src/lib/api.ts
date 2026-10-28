import {
  AuthResponse,
  Project,
  AuditResponse,
  CorsDiagnosticResult,
  PaginatedAudits,
  ApiCheck,
  SingleSmokeTestResponse
} from '../types';

export class ApiError extends Error {
  constructor(message: string, public status: number, public code?: string) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(path: string, options: RequestInit = {}, token?: string | null): Promise<T> {
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && options.body && typeof options.body === 'string') {
    headers.set('Content-Type', 'application/json');
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(path, {
    ...options,
    headers
  });

  if (response.status === 204) {
    return {} as T;
  }

  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');

  if (!response.ok) {
    let errorMessage = `HTTP error ${response.status}`;
    let errorCode: string | undefined;

    if (isJson) {
      try {
        const errorBody = await response.json();
        errorMessage = errorBody.message || errorBody.error || errorMessage;
        errorCode = errorBody.code;
      } catch {
        // Fallback to default message
      }
    } else {
      const text = await response.text();
      if (text) errorMessage = text;
    }

    throw new ApiError(errorMessage, response.status, errorCode);
  }

  if (!isJson) {
    return {} as T;
  }

  return response.json();
}

export const api = {
  auth: {
    login: (credentials: { email: string; password: string }) =>
      request<AuthResponse>('/api/v1/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials)
      }),

    register: (credentials: { email: string; password: string }) =>
      request<AuthResponse>('/api/v1/auth/register', {
        method: 'POST',
        body: JSON.stringify(credentials)
      }),

    me: (token: string) =>
      request<{ id: string; email: string }>('/api/v1/auth/me', {
        method: 'GET'
      }, token)
  },

  projects: {
    list: (token: string) =>
      request<Project[]>('/api/v1/projects', { method: 'GET' }, token),

    get: (id: string, token: string) =>
      request<Project>(`/api/v1/projects/${id}`, { method: 'GET' }, token),

    create: (data: { name: string; frontendUrl: string; backendUrl: string }, token: string) =>
      request<Project>('/api/v1/projects', {
        method: 'POST',
        body: JSON.stringify(data)
      }, token),

    update: (id: string, data: { name: string; frontendUrl: string; backendUrl: string }, token: string) =>
      request<Project>(`/api/v1/projects/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data)
      }, token),

    delete: (id: string, token: string) =>
      request<void>(`/api/v1/projects/${id}`, { method: 'DELETE' }, token)
  },

  smokeTests: {
    run: (payload: {
      name?: string;
      url: string;
      expectedStatus: number;
      jsonPointer?: string;
      expectedJsonValue?: string;
    }, token?: string | null) =>
      request<SingleSmokeTestResponse>('/api/v1/audits/smoke', {
        method: 'POST',
        body: JSON.stringify(payload)
      }, token)
  },

  audits: {
    runPublic: (data: { frontendUrl: string; backendUrl: string; apiChecks?: ApiCheck[] }, token?: string | null) =>
      request<AuditResponse>('/api/v1/audits', {
        method: 'POST',
        body: JSON.stringify(data)
      }, token),

    runForProject: (projectId: string, apiChecks: ApiCheck[] | null | undefined, token: string) =>
      request<AuditResponse>(`/api/v1/projects/${projectId}/audits`, {
        method: 'POST',
        body: JSON.stringify({ apiChecks: apiChecks && apiChecks.length > 0 ? apiChecks : null })
      }, token),

    getUserHistory: (token: string, page = 0, size = 20, type?: string) => {
      let query = `/api/v1/audits/history?page=${page}&size=${size}`;
      if (type && type !== 'ALL') {
        query += `&type=${encodeURIComponent(type)}`;
      }
      return request<PaginatedAudits>(query, {
        method: 'GET'
      }, token);
    },

    history: (token: string, page = 0, size = 20) =>
      request<PaginatedAudits>(`/api/v1/audits/history?page=${page}&size=${size}`, {
        method: 'GET'
      }, token),

    getHistoryList: (projectId: string, page = 0, size = 20, token: string) =>
      request<PaginatedAudits>(`/api/v1/projects/${projectId}/audits?page=${page}&size=${size}`, {
        method: 'GET'
      }, token),

    getHistoryDetail: (auditId: string, token: string) =>
      request<AuditResponse>(`/api/v1/audits/history/${auditId}`, {
        method: 'GET'
      }, token),

    deleteRecord: (id: string, token: string) =>
      request<void>(`/api/v1/audits/history/${id}`, {
        method: 'DELETE'
      }, token),

    clearHistory: (token: string) =>
      request<void>('/api/v1/audits/history', {
        method: 'DELETE'
      }, token),

    checkCors: (backendUrl: string, origin: string, endpointPath: string, token?: string | null) => {
      const params = new URLSearchParams({
        backendUrl,
        origin,
        endpointPath
      });
      return request<CorsDiagnosticResult>(`/api/v1/audits/cors?${params.toString()}`, {
        method: 'GET'
      }, token);
    }
  }
};
