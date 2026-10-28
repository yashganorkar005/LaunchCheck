import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { ResultsView } from '../components/workspace/ResultsView';
import { AuditResponse } from '../types';

describe('ResultsView Component', () => {
  const mockAuditResult: AuditResponse = {
    auditId: '123e4567-e89b-12d3-a456-426614174000',
    startedAt: new Date().toISOString(),
    completedAt: new Date().toISOString(),
    durationMs: 100,
    overallStatus: 'PASS',
    targets: [
      {
        role: 'FRONTEND',
        host: 'app.example.com',
        status: 'PASS',
        httpStatus: 200,
        responseTimeMs: 50,
        tls: {
          status: 'PASS',
          certificateExpiresAt: '2026-12-31T23:59:59Z',
          diagnostic: 'TLS handshake successful'
        },
        redirects: [],
        diagnostic: 'Reachability confirmed',
        nextStep: 'No action required',
        securityHeaders: []
      }
    ]
  };

  it('shows full report and JSON download actions for an available result', () => {
    render(<ResultsView auditResult={mockAuditResult} onBackToStudio={() => {}} />);

    expect(screen.getByRole('button', { name: /download full report/i })).toBeDefined();
    expect(screen.getByRole('button', { name: /^json$/i })).toBeDefined();
  });

  it('renders certificate expiry date correctly when available', () => {
    render(<ResultsView auditResult={mockAuditResult} onBackToStudio={() => {}} />);

    // Check if the formatted date is present
    const dateText = screen.getByText(/Valid Until:/i).parentElement?.textContent;
    expect(dateText).toBeDefined();
    // The component should render the date using toLocaleDateString()
    expect(dateText).toMatch(/\d{1,2}\/\d{1,2}\/\d{4}/);
  });

  it('renders "Active" when expiry is missing', () => {
    const auditWithoutTlsDate = {
      ...mockAuditResult,
      targets: [
        {
          ...mockAuditResult.targets[0],
          tls: { ...mockAuditResult.targets[0].tls, certificateExpiresAt: undefined }
        }
      ]
    };
    render(<ResultsView auditResult={auditWithoutTlsDate} onBackToStudio={() => {}} />);

    expect(screen.getByText('Active')).toBeDefined();
  });

  it('renders days remaining when supplied', () => {
    const auditWithDaysRemaining = {
      ...mockAuditResult,
      targets: [
        {
          ...mockAuditResult.targets[0],
          tls: { ...mockAuditResult.targets[0].tls, daysRemaining: 64 }
        }
      ]
    };
    render(<ResultsView auditResult={auditWithDaysRemaining} onBackToStudio={() => {}} />);

    expect(screen.getByText('64 days')).toBeDefined();
  });

  it('falls back to Active for invalid expiry metadata', () => {
    const auditWithInvalidTlsDate = {
      ...mockAuditResult,
      targets: [
        {
          ...mockAuditResult.targets[0],
          tls: { ...mockAuditResult.targets[0].tls, certificateExpiresAt: 'not-a-date' }
        }
      ]
    };
    render(<ResultsView auditResult={auditWithInvalidTlsDate} onBackToStudio={() => {}} />);

    expect(screen.getByText('Active')).toBeDefined();
    expect(screen.queryByText('Invalid Date')).toBeNull();
  });

  it('keeps TLS failure status and diagnostic visible alongside expiry metadata', () => {
    const failedTlsAudit = {
      ...mockAuditResult,
      targets: [
        {
          ...mockAuditResult.targets[0],
          status: 'FAIL' as const,
          tls: {
            ...mockAuditResult.targets[0].tls,
            status: 'FAIL' as const,
            diagnostic: 'TLS handshake failed: certificate hostname mismatch'
          }
        }
      ]
    };
    render(<ResultsView auditResult={failedTlsAudit} onBackToStudio={() => {}} />);

    expect(screen.getByText('TLS handshake failed: certificate hostname mismatch')).toBeDefined();
    expect(screen.getAllByText('FAIL').length).toBeGreaterThan(0);
    expect(screen.getByText(/Valid Until:/i).parentElement?.textContent).not.toContain('Active');
  });
});
