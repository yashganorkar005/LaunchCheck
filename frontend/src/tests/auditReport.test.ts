import { describe, expect, it } from 'vitest';
import { AuditResponse } from '../types';
import { createAuditJsonBlob, createAuditPdfBlob, sanitizeAuditForExport } from '../lib/auditReport';

const audit: AuditResponse = {
  auditId: 'audit-42',
  startedAt: '2026-10-28T10:00:00Z',
  completedAt: '2026-10-28T10:00:04Z',
  durationMs: 4000,
  overallStatus: 'WARN',
  targets: [{
    role: 'FRONTEND',
    host: 'app.example.com',
    status: 'WARN',
    httpStatus: 200,
    responseTimeMs: 120,
    tls: {
      status: 'WARN',
      certificateExpiresAt: '2026-11-03T00:00:00Z',
      daysRemaining: 6,
      diagnostic: 'Certificate expires soon'
    },
    redirects: [],
    diagnostic: 'Reachability confirmed',
    nextStep: 'Renew certificate',
    securityHeaders: [{
      headerName: 'Content-Security-Policy',
      status: 'WARN',
      value: "default-src 'self'",
      diagnostic: 'CSP is present but restrictive'
    }]
  }],
  cors: {
    endpointPath: '/api/data',
    method: 'GET',
    origin: 'https://app.example.com',
    status: 'PASS',
    diagnostic: 'Origin permitted',
    preflightAttempted: true,
    responseHeaders: {
      allowOrigin: 'https://app.example.com',
      allowMethods: 'GET, POST',
      allowHeaders: 'content-type',
      allowCredentials: 'true',
      vary: 'Origin'
    }
  },
  apiTests: [{
    path: '/api/data',
    method: 'GET',
    expectedStatus: 200,
    actualStatus: 500,
    status: 'FAIL',
    diagnostic: 'Unexpected response status',
    nextStep: 'Inspect API',
    jsonAssertionPassed: false,
    jsonAssertionDiagnostic: 'JSON pointer /status did not match expected value'
  }]
};

describe('audit report exports', () => {
  it('preserves the complete available audit response in JSON', async () => {
    const json = await (await createAuditJsonBlob(audit)).text();
    const parsed = JSON.parse(json);

    expect(parsed.auditId).toBe('audit-42');
    expect(parsed.targets[0].tls.daysRemaining).toBe(6);
    expect(parsed.targets[0].securityHeaders[0].diagnostic).toContain('restrictive');
    expect(parsed.cors.responseHeaders.allowHeaders).toBe('content-type');
    expect(parsed.cors.responseHeaders.allowCredentials).toBe('true');
    expect(parsed.apiTests[0].jsonAssertionDiagnostic).toContain('/status');
  });

  it('creates a readable PDF containing target, TLS, header, CORS, and smoke diagnostics', async () => {
    const pdf = await (await createAuditPdfBlob(audit)).text();

    expect(pdf).toContain('%PDF-1.4\n');
    expect(pdf).toContain('\nstream\n');
    expect(pdf).toContain('LaunchCheck Full Audit Report');
    expect(pdf).toContain('Certificate expires soon');
    expect(pdf).toContain('Content-Security-Policy');
    expect(pdf).toContain('Origin permitted');
    expect(pdf).toContain('Unexpected response status');
  });

  it('wraps long diagnostics across multiple PDF pages', async () => {
    const longAudit = {
      ...audit,
      targets: [{
        ...audit.targets[0],
        securityHeaders: [{
          ...audit.targets[0].securityHeaders![0],
          value: 'default-src ' + 'github.githubassets.com '.repeat(80)
        }]
      }]
    };
    const pdf = await (await createAuditPdfBlob(longAudit)).text();

    expect((pdf.match(/\/Type \/Page/g) || []).length).toBeGreaterThan(1);
  });

  it('removes credential-like fields from exported data', () => {
    const sanitized = sanitizeAuditForExport({
      ...audit,
      authorization: 'Bearer secret',
      password: 'do-not-export',
      nested: { cookie: 'session-secret', safe: 'kept' }
    } as AuditResponse & Record<string, unknown>);

    expect(sanitized).not.toHaveProperty('authorization');
    expect(sanitized).not.toHaveProperty('password');
    expect(sanitized.nested).not.toHaveProperty('cookie');
    expect(sanitized.nested.safe).toBe('kept');
  });

  it('represents missing fields without inventing values', async () => {
    const incomplete = { ...audit, cors: undefined, apiTests: undefined, targets: [{
      ...audit.targets[0],
      tls: { status: 'SKIPPED' as const, diagnostic: 'TLS unavailable' },
      securityHeaders: undefined
    }] };
    const json = await (await createAuditJsonBlob(incomplete)).text();
    const pdf = await (await createAuditPdfBlob(incomplete)).text();

    expect(JSON.parse(json).cors).toBeNull();
    expect(pdf).toContain('CORS: Not collected');
    expect(pdf).toContain('Security headers: Not collected');
  });
});
