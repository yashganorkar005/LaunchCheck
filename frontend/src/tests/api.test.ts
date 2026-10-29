import { describe, expect, it } from 'vitest';
import { buildApiUrl } from '../lib/api';

describe('buildApiUrl', () => {
  it('keeps relative API paths for local Vite proxying', () => {
    expect(buildApiUrl('/api/v1/auth/me', undefined, true)).toBe('/api/v1/auth/me');
  });

  it('joins a hosted API origin and path without duplicate slashes', () => {
    expect(buildApiUrl('/api/v1/auth/me', 'https://api.example.com/', false))
      .toBe('https://api.example.com/api/v1/auth/me');
  });

  it('supports a backend base path', () => {
    expect(buildApiUrl('/api/v1/auth/me', 'https://example.com/backend', false))
      .toBe('https://example.com/backend/api/v1/auth/me');
  });

  it('fails clearly when production configuration is missing', () => {
    expect(() => buildApiUrl('/api/v1/auth/me', undefined, false))
      .toThrow('VITE_API_BASE_URL is required for production builds');
  });

  it('rejects malformed or non-http production URLs', () => {
    expect(() => buildApiUrl('/api/v1/auth/me', 'not-a-url', false))
      .toThrow('VITE_API_BASE_URL must be an absolute http(s) URL');
    expect(() => buildApiUrl('/api/v1/auth/me', 'ftp://api.example.com', false))
      .toThrow('VITE_API_BASE_URL must use http or https');
  });
});
