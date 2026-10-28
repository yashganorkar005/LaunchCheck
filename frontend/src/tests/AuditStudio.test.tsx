import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuditStudio } from '../components/workspace/AuditStudio';
import { ToastProvider } from '../components/ui/Toast';
import { api } from '../lib/api';

vi.mock('../lib/api', () => ({
  api: {
    audits: {
      runPublic: vi.fn()
    }
  },
  ApiError: class ApiError extends Error {}
}));

const runPublic = vi.mocked(api.audits.runPublic);

function renderStudio(props: Partial<React.ComponentProps<typeof AuditStudio>> = {}) {
  return render(
    <ToastProvider>
      <AuditStudio onAuditComplete={() => {}} {...props} />
    </ToastProvider>
  );
}

describe('AuditStudio demo configuration', () => {
  beforeEach(() => {
    runPublic.mockReset();
    runPublic.mockResolvedValue({
      auditId: 'audit-1',
      startedAt: '2026-10-28T00:00:00Z',
      completedAt: '2026-10-28T00:00:01Z',
      durationMs: 1000,
      overallStatus: 'PASS',
      targets: []
    });
  });

  it('populates the verifiable GitHub demo defaults and serializes its smoke assertions', async () => {
    renderStudio();

    expect(screen.getByLabelText('Frontend Web Target URL')).toHaveValue('https://github.com');
    expect(screen.getByLabelText('Backend API Target URL')).toHaveValue('https://api.github.com');
    expect(screen.getByDisplayValue('Read Public Repository')).toBeInTheDocument();
    expect(screen.getByDisplayValue('/repos/octocat/Spoon-Knife')).toBeInTheDocument();
    expect(screen.getByDisplayValue('200')).toBeInTheDocument();
    expect(screen.getByDisplayValue('/full_name')).toBeInTheDocument();
    expect(screen.getByDisplayValue('octocat/Spoon-Knife')).toBeInTheDocument();
    expect(screen.getByDisplayValue('content-type')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Execute Preflight Audit' }));

    await waitFor(() => expect(runPublic).toHaveBeenCalledOnce());
    expect(runPublic.mock.calls[0][0]).toEqual({
      frontendUrl: 'https://github.com',
      backendUrl: 'https://api.github.com',
      apiChecks: [{
        path: '/repos/octocat/Spoon-Knife',
        method: 'GET',
        expectedStatus: 200,
        headerAssertions: [{ name: 'content-type', value: 'application/json' }],
        jsonAssertions: [{ jsonPointer: '/full_name', expectedValue: 'octocat/Spoon-Knife' }]
      }]
    });
  });

  it('preserves saved project targets instead of applying demo defaults', () => {
    renderStudio({
      selectedProject: {
        id: 'project-1',
        name: 'Production',
        frontendUrl: 'https://app.example.com',
        backendUrl: 'https://api.example.com',
        createdAt: '2026-10-28T00:00:00Z'
      }
    });

    expect(screen.getByLabelText('Frontend Web Target URL')).toHaveValue('https://app.example.com');
    expect(screen.getByLabelText('Backend API Target URL')).toHaveValue('https://api.example.com');
  });

  it('does not serialize an empty added smoke-test slot', async () => {
    renderStudio();

    fireEvent.click(screen.getByRole('button', { name: 'Add Check' }));
    expect(screen.getAllByLabelText(/smoke test \d+ path/i)).toHaveLength(2);
    expect(screen.getByLabelText('Smoke test 2 path')).toHaveValue('');

    fireEvent.click(screen.getByRole('button', { name: 'Execute Preflight Audit' }));

    await waitFor(() => expect(runPublic).toHaveBeenCalledOnce());
    expect(runPublic.mock.calls[0][0].apiChecks).toHaveLength(1);
    expect(runPublic.mock.calls[0][0].apiChecks[0].path).toBe('/repos/octocat/Spoon-Knife');
  });

  it('does not report success when the audit request fails', async () => {
    const onAuditComplete = vi.fn();
    runPublic.mockRejectedValueOnce(new Error('Audit unavailable'));
    renderStudio({ onAuditComplete });

    fireEvent.click(screen.getByRole('button', { name: 'Execute Preflight Audit' }));

    await waitFor(() => expect(screen.getAllByText('Audit unavailable').length).toBeGreaterThan(0));
    expect(onAuditComplete).not.toHaveBeenCalled();
  });
});
