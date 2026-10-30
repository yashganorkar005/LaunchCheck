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

  it('populates JSONPlaceholder defaults, labels every field, and serializes its smoke assertion', async () => {
    renderStudio();

    expect(screen.getByLabelText('Frontend Web Target URL')).toHaveValue('https://jsonplaceholder.typicode.com');
    expect(screen.getByLabelText('Backend API Target URL')).toHaveValue('https://jsonplaceholder.typicode.com');
    expect(screen.getByDisplayValue('Read Sample Post')).toBeInTheDocument();
    expect(screen.getByDisplayValue('/posts/1')).toBeInTheDocument();
    expect(screen.getByDisplayValue('200')).toBeInTheDocument();
    expect(screen.getByDisplayValue('/title')).toBeInTheDocument();
    expect(screen.getByDisplayValue('sunt aut facere repellat provident occaecati excepturi optio reprehenderit')).toBeInTheDocument();
    expect(screen.getByDisplayValue('content-type')).toBeInTheDocument();
    expect(screen.getByDisplayValue('application/json')).toBeInTheDocument();
    expect(screen.getByLabelText('Smoke test 1 header match condition')).toHaveValue('contains');

    expect(screen.getByText('Give this API test a short, descriptive name.')).toBeInTheDocument();
    expect(screen.getByText('Enter the endpoint path relative to your Backend API Target URL. Do not include the domain.')).toBeInTheDocument();
    expect(screen.getByText('Enter the HTTP status code that indicates a successful response for this test.')).toBeInTheDocument();
    expect(screen.getByText('Enter the name of the response header you want LaunchCheck to validate.')).toBeInTheDocument();
    expect(screen.getByText('Choose how LaunchCheck should compare the actual response header with your expected value.')).toBeInTheDocument();
    expect(screen.getByText('Enter the text that must appear in the selected response header.')).toBeInTheDocument();
    expect(screen.getByText('Enter the JSON field path you want to validate. Use JSON Pointer syntax beginning with a forward slash.')).toBeInTheDocument();
    expect(screen.getByText('Enter the exact expected value of the selected JSON field.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Execute Preflight Audit' }));

    await waitFor(() => expect(runPublic).toHaveBeenCalledOnce());
    expect(runPublic.mock.calls[0][0]).toEqual({
      frontendUrl: 'https://jsonplaceholder.typicode.com',
      backendUrl: 'https://jsonplaceholder.typicode.com',
      apiChecks: [{
        path: '/posts/1',
        method: 'GET',
        expectedStatus: 200,
        headerAssertions: [{ name: 'content-type', value: 'application/json' }],
        jsonAssertions: [{ jsonPointer: '/title', expectedValue: 'sunt aut facere repellat provident occaecati excepturi optio reprehenderit' }]
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

  it('preserves custom targets and filters an empty added smoke-test slot', async () => {
    renderStudio({
      initialFrontend: 'https://app.example.com',
      initialBackend: 'https://api.example.com'
    });

    expect(screen.getByLabelText('Frontend Web Target URL')).toHaveValue('https://app.example.com');
    expect(screen.getByLabelText('Backend API Target URL')).toHaveValue('https://api.example.com');

    fireEvent.click(screen.getByRole('button', { name: 'Add Check' }));
    expect(screen.getAllByLabelText(/smoke test \d+ path/i)).toHaveLength(2);
    expect(screen.getByLabelText('Smoke test 2 path')).toHaveValue('');

    fireEvent.click(screen.getByRole('button', { name: 'Remove smoke test 2' }));
    expect(screen.getAllByLabelText(/smoke test \d+ path/i)).toHaveLength(1);

    fireEvent.click(screen.getByRole('button', { name: 'Execute Preflight Audit' }));

    await waitFor(() => expect(runPublic).toHaveBeenCalledOnce());
    expect(runPublic.mock.calls[0][0].frontendUrl).toBe('https://app.example.com');
    expect(runPublic.mock.calls[0][0].backendUrl).toBe('https://api.example.com');
    expect(runPublic.mock.calls[0][0].apiChecks).toHaveLength(1);
    expect(runPublic.mock.calls[0][0].apiChecks[0].path).toBe('/posts/1');
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
