import React, { useState } from 'react';
import { Globe, Server, Zap, ShieldAlert, CheckCircle2, AlertTriangle, Plus, Trash2, Cpu } from 'lucide-react';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Badge } from '../ui/Badge';
import { useToast } from '../ui/Toast';
import { AuditResponse, Project } from '../../types';
import { api, ApiError } from '../../lib/api';

interface AuditStudioProps {
  selectedProject: Project | null;
  onAuditComplete: (result: AuditResponse) => void;
  initialFrontend?: string;
  initialBackend?: string;
}

export function AuditStudio({
  selectedProject,
  onAuditComplete,
  initialFrontend = 'https://github.com',
  initialBackend = 'https://api.github.com'
}: AuditStudioProps) {
  const [frontendUrl, setFrontendUrl] = useState(initialFrontend);
  const [backendUrl, setBackendUrl] = useState(initialBackend);

  React.useEffect(() => {
    if (selectedProject) {
      setFrontendUrl(selectedProject.frontendUrl);
      setBackendUrl(selectedProject.backendUrl);
    }
  }, [selectedProject]);
  const [smokeTests, setSmokeTests] = useState<Array<{
    name: string;
    path: string;
    expectedStatus: number;
    headerAssertions: { name: string; value: string }[];
    jsonAssertions: { jsonPointer: string; expectedValue: string }[];
  }>>([
    {
      name: 'Read Public Repository',
      path: '/repos/octocat/Spoon-Knife',
      expectedStatus: 200,
      headerAssertions: [{ name: 'content-type', value: 'application/json' }],
      jsonAssertions: [{ jsonPointer: '/full_name', expectedValue: 'octocat/Spoon-Knife' }]
    }
  ]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { toast } = useToast();

  const addSmokeTest = () => {
    if (smokeTests.length >= 5) return;
    setSmokeTests([...smokeTests, {
      name: '',
      path: '',
      expectedStatus: 200,
      headerAssertions: [],
      jsonAssertions: []
    }]);
  };

  const removeSmokeTest = (index: number) => {
    setSmokeTests(smokeTests.filter((_, i) => i !== index));
  };

  const handleRunAudit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      toast.info('Audit dispatched', selectedProject ? `Running preflight for ${selectedProject.name}` : 'Running preflight target probes...');
      const apiChecks = smokeTests
        .filter(st => st.path.trim())
        .map(st => ({
          path: st.path,
          method: 'GET',
          expectedStatus: Number(st.expectedStatus),
          headerAssertions: st.headerAssertions,
          jsonAssertions: st.jsonAssertions
        }));

      let result: AuditResponse;
      const token = localStorage.getItem('lc_token');
      if (selectedProject) {
        result = await api.audits.runForProject(
          selectedProject.id,
          apiChecks,
          token || ''
        );
      } else {
        result = await api.audits.runPublic({
          frontendUrl,
          backendUrl,
          apiChecks
        }, token);
      }
      window.dispatchEvent(new CustomEvent('launchcheck:audit-recorded'));

      if (result.overallStatus === 'PASS') {
        toast.success('Preflight audit passed', `Audit completed in ${result.durationMs} ms with status PASS.`);
      } else if (result.overallStatus === 'WARN') {
        toast.warn('Preflight audit warning', `Audit completed in ${result.durationMs} ms with warnings.`);
      } else {
        toast.error('Preflight audit failed', `Audit completed with failure status.`);
      }

      onAuditComplete(result);
    } catch (err: any) {
      const message = err instanceof ApiError ? err.message : (err.message || 'Audit execution failed. Please check target connectivity.');
      setError(message);
      toast.error('Audit execution error', message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <div className="text-xs font-mono uppercase tracking-widest text-[#38FF8E] mb-1">
          // PREFLIGHT AUDIT STUDIO
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#F4F8F4]">
          Configure Deployment Targets
        </h1>
        <p className="text-xs sm:text-sm text-[#C0CAC2] mt-1">
          Execute SSRF-hardened reachability probes, TLS certificate audits, CORS preflights, and smoke tests.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column: Form Configuration */}
        <div className="lg:col-span-7 bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-6">
          <form onSubmit={handleRunAudit} className="space-y-6">
            <div className="space-y-4">
              <h3 className="font-mono text-xs font-bold text-[#65F8FF] uppercase tracking-wider pb-2 border-b border-[#1D2A22]">
                1. Target Endpoints
              </h3>

              <Input
                label="Frontend Web Target URL"
                type="url"
                required
                value={frontendUrl}
                onChange={(e) => setFrontendUrl(e.target.value)}
                placeholder="https://github.com"
                icon={<Globe className="w-4 h-4" />}
                monospace
                hint="Validates URI syntax, HTTP/HTTPS protocols, and standard ports."
              />

              <Input
                label="Backend API Target URL"
                type="url"
                required
                value={backendUrl}
                onChange={(e) => setBackendUrl(e.target.value)}
                placeholder="https://api.example.com"
                icon={<Server className="w-4 h-4" />}
                monospace
                hint="Subject to upfront DNS resolution and SSRF IP safety policies."
              />
            </div>

            {/* Smoke Tests Configuration */}
            <div className="space-y-3 pt-2">
              <div className="flex items-center justify-between pb-2 border-b border-[#1D2A22]">
                <h3 className="font-mono text-xs font-bold text-[#FF65D8] uppercase tracking-wider">
                  2. API Smoke Tests (Up to 5)
                </h3>
                <button
                  type="button"
                  onClick={addSmokeTest}
                  disabled={smokeTests.length >= 5}
                  className="flex items-center space-x-1 text-[11px] font-mono text-[#38FF8E] hover:underline disabled:opacity-40 cursor-pointer"
                >
                  <Plus className="w-3 h-3" />
                  <span>Add Check</span>
                </button>
              </div>

              {smokeTests.map((st, idx) => (
                <div key={idx} className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-[11px] text-[#849188]">CHECK #{idx + 1}</span>
                    {smokeTests.length > 1 && (
                      <button
                        type="button"
                        onClick={() => removeSmokeTest(idx)}
                        className="text-[#FF5A78] hover:text-[#FF5A78]/80 cursor-pointer"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-12 gap-3">
                    <div className="sm:col-span-5">
                      <input
                        aria-label={`Smoke test ${idx + 1} name`}
                        type="text"
                        value={st.name}
                        onChange={(e) => {
                          const updated = [...smokeTests];
                          updated[idx].name = e.target.value;
                          setSmokeTests(updated);
                        }}
                        placeholder="Check Name"
                        className="w-full bg-[#0C110E] border border-[#25352B] rounded-[2px] px-2.5 py-1.5 text-xs font-mono text-[#F4F8F4] focus:outline-none focus:border-[#38FF8E]"
                      />
                    </div>
                    <div className="sm:col-span-5">
                      <input
                        aria-label={`Smoke test ${idx + 1} path`}
                        type="text"
                        value={st.path}
                        onChange={(e) => {
                          const updated = [...smokeTests];
                          updated[idx].path = e.target.value;
                          setSmokeTests(updated);
                        }}
                        placeholder="/api/health"
                        className="w-full bg-[#0C110E] border border-[#25352B] rounded-[2px] px-2.5 py-1.5 text-xs font-mono text-[#F4F8F4] focus:outline-none focus:border-[#38FF8E]"
                      />
                    </div>
                    <div className="sm:col-span-2">
                      <input
                        aria-label={`Smoke test ${idx + 1} expected status`}
                        type="number"
                        value={st.expectedStatus}
                        onChange={(e) => {
                          const updated = [...smokeTests];
                          updated[idx].expectedStatus = Number(e.target.value);
                          setSmokeTests(updated);
                        }}
                        placeholder="200"
                        className="w-full bg-[#0C110E] border border-[#25352B] rounded-[2px] px-2.5 py-1.5 text-xs font-mono text-[#F4F8F4] focus:outline-none focus:border-[#38FF8E]"
                      />
                    </div>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <input
                      aria-label={`Smoke test ${idx + 1} response header`}
                      type="text"
                      value={st.headerAssertions[0]?.name || ''}
                      onChange={(e) => {
                        const updated = [...smokeTests];
                        const assertion = updated[idx].headerAssertions[0] || { name: '', value: '' };
                        updated[idx].headerAssertions = [{ ...assertion, name: e.target.value }];
                        setSmokeTests(updated);
                      }}
                      placeholder="Response header"
                      className="w-full bg-[#0C110E] border border-[#25352B] rounded-[2px] px-2.5 py-1.5 text-xs font-mono text-[#F4F8F4] focus:outline-none focus:border-[#38FF8E]"
                    />
                    <input
                      aria-label={`Smoke test ${idx + 1} response header value`}
                      type="text"
                      value={st.headerAssertions[0]?.value || ''}
                      onChange={(e) => {
                        const updated = [...smokeTests];
                        const assertion = updated[idx].headerAssertions[0] || { name: '', value: '' };
                        updated[idx].headerAssertions = [{ ...assertion, value: e.target.value }];
                        setSmokeTests(updated);
                      }}
                      placeholder="Expected header value"
                      className="w-full bg-[#0C110E] border border-[#25352B] rounded-[2px] px-2.5 py-1.5 text-xs font-mono text-[#F4F8F4] focus:outline-none focus:border-[#38FF8E]"
                    />
                    <input
                      aria-label={`Smoke test ${idx + 1} JSON pointer`}
                      type="text"
                      value={st.jsonAssertions[0]?.jsonPointer || ''}
                      onChange={(e) => {
                        const updated = [...smokeTests];
                        const assertion = updated[idx].jsonAssertions[0] || { jsonPointer: '', expectedValue: '' };
                        updated[idx].jsonAssertions = [{ ...assertion, jsonPointer: e.target.value }];
                        setSmokeTests(updated);
                      }}
                      placeholder="JSON Pointer"
                      className="w-full bg-[#0C110E] border border-[#25352B] rounded-[2px] px-2.5 py-1.5 text-xs font-mono text-[#F4F8F4] focus:outline-none focus:border-[#38FF8E]"
                    />
                    <input
                      aria-label={`Smoke test ${idx + 1} expected JSON value`}
                      type="text"
                      value={st.jsonAssertions[0]?.expectedValue || ''}
                      onChange={(e) => {
                        const updated = [...smokeTests];
                        const assertion = updated[idx].jsonAssertions[0] || { jsonPointer: '', expectedValue: '' };
                        updated[idx].jsonAssertions = [{ ...assertion, expectedValue: e.target.value }];
                        setSmokeTests(updated);
                      }}
                      placeholder="Expected JSON value"
                      className="w-full bg-[#0C110E] border border-[#25352B] rounded-[2px] px-2.5 py-1.5 text-xs font-mono text-[#F4F8F4] focus:outline-none focus:border-[#38FF8E]"
                    />
                  </div>
                </div>
              ))}
            </div>

            {error && (
              <div className="p-3 bg-[#FF5A78]/10 border border-[#FF5A78]/40 text-[#FF5A78] text-xs font-mono rounded-[2px]">
                {error}
              </div>
            )}

            <div className="pt-4 border-t border-[#1D2A22]">
              <Button
                type="submit"
                variant="primary"
                size="lg"
                isLoading={loading}
                icon={<Zap className="w-4 h-4" />}
                className="w-full"
              >
                Execute Preflight Audit
              </Button>
            </div>
          </form>
        </div>

        {/* Right Column: Preflight Safety Overview */}
        <div className="lg:col-span-5 space-y-6">
          <div className="bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4">
            <h3 className="font-mono text-xs font-bold text-[#38FF8E] uppercase tracking-wider">
              // SECURITY & PINNING GUARANTEE
            </h3>
            <p className="text-xs text-[#C0CAC2] leading-relaxed">
              Every audit initiated from this studio executes through LaunchCheck's SSRF-safe pinned transport policy:
            </p>
            <ul className="space-y-2 text-xs font-mono text-[#849188]">
              <li className="flex items-center space-x-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#38FF8E]" />
                <span>Upfront DNS resolution & validation</span>
              </li>
              <li className="flex items-center space-x-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#38FF8E]" />
                <span>Private IP range rejection (RFC 1918)</span>
              </li>
              <li className="flex items-center space-x-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#38FF8E]" />
                <span>Cloud metadata firewall (169.254.169.254)</span>
              </li>
              <li className="flex items-center space-x-2">
                <span className="w-1.5 h-1.5 rounded-full bg-[#38FF8E]" />
                <span>Direct IP socket binding (DNS rebinding defense)</span>
              </li>
            </ul>
          </div>

          <div className="bg-[#070A08] border border-[#1D2A22] p-6 rounded-[2px] space-y-3 font-mono text-xs">
            <div className="text-[#65F8FF] font-bold">ACTIVE PROJECT BINDING:</div>
            <div className="text-[#F4F8F4] font-bold">
              {selectedProject ? selectedProject.name : 'Transient Session (Not Saved to Project)'}
            </div>
            <div className="text-[11px] text-[#849188]">
              {selectedProject
                ? 'Results of this audit will be automatically stored in the audit history of this project.'
                : 'Select or create a project to persist audit histories across sessions.'}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
