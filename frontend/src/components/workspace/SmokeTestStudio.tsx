import React, { useState } from 'react';
import { Cpu, FileCode2, Zap, CheckCircle2 } from 'lucide-react';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Badge } from '../ui/Badge';
import { useToast } from '../ui/Toast';
import { api, ApiError } from '../../lib/api';
import { SingleSmokeTestResponse } from '../../types';

export function SmokeTestStudio() {
  const [url, setUrl] = useState('https://jsonplaceholder.typicode.com/posts/1');
  const [expectedStatus, setExpectedStatus] = useState(200);
  const [jsonPointer, setJsonPointer] = useState('/id');
  const [expectedValue, setExpectedValue] = useState('1');
  const [bearerToken, setBearerToken] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<SingleSmokeTestResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { toast } = useToast();

  const handleRunSmokeTest = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const payload = {
        name: 'Single Smoke Test',
        url,
        expectedStatus: Number(expectedStatus),
        jsonPointer: jsonPointer ? jsonPointer : undefined,
        expectedJsonValue: expectedValue ? expectedValue : undefined,
        bearerToken: bearerToken.trim() ? bearerToken.trim() : undefined
      };
      const token = localStorage.getItem('lc_token');
      const res = await api.smokeTests.run(payload, token);
      setBearerToken('');
      setResult(res);
      window.dispatchEvent(new CustomEvent('launchcheck:audit-recorded'));
      if (res.success) {
        toast.success('Smoke test passed', `HTTP ${res.actualStatus} in ${res.latencyMs} ms`);
      } else {
        toast.error('Smoke test failed', res.errorMessage || res.diagnostic || `Expected HTTP ${res.expectedStatus}`);
      }
    } catch (err: any) {
      setBearerToken('');
      const message = err instanceof ApiError ? err.message : (err.message || 'Smoke test execution failed.');
      setError(message);
      toast.error('Smoke test execution failed', message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <div className="text-xs font-mono uppercase tracking-widest text-[#FF65D8] mb-1">
          // API SMOKE TEST & JSON POINTER ASSERTS
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#F4F8F4]">
          Deterministic Payload Assertions
        </h1>
        <p className="text-xs sm:text-sm text-[#C0CAC2] mt-1">
          Verify API responses against exact HTTP status codes and RFC 6901 JSON Pointers.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Form */}
        <div className="lg:col-span-6 bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-6">
          <form onSubmit={handleRunSmokeTest} className="space-y-4">
            <Input
              label="Endpoint URL"
              type="url"
              required
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://api.example.com/health"
              icon={<Cpu className="w-4 h-4" />}
              monospace
            />
            <Input
              label="Expected HTTP Status Code"
              type="number"
              required
              value={expectedStatus}
              onChange={(e) => setExpectedStatus(Number(e.target.value))}
              monospace
            />
            <Input
              label="RFC 6901 JSON Pointer (Optional)"
              type="text"
              value={jsonPointer}
              onChange={(e) => setJsonPointer(e.target.value)}
              placeholder="/status"
              icon={<FileCode2 className="w-4 h-4" />}
              monospace
              hint="e.g. /status or /data/active"
            />
            <Input
              label="Target Bearer Token (Optional)"
              type="password"
              value={bearerToken}
              onChange={(e) => setBearerToken(e.target.value)}
              placeholder="Optional credential for the configured target"
              hint="Used for this request only; never saved"
              monospace
            />
            <Input
              label="Expected Value at JSON Pointer (Optional)"
              type="text"
              value={expectedValue}
              onChange={(e) => setExpectedValue(e.target.value)}
              placeholder="healthy"
              monospace
            />

            {error && (
              <div className="p-3 bg-[#FF5A78]/10 border border-[#FF5A78]/40 text-[#FF5A78] text-xs font-mono rounded-[2px]">
                {error}
              </div>
            )}

            <div className="pt-2">
              <Button
                type="submit"
                variant="primary"
                size="md"
                isLoading={loading}
                icon={<Zap className="w-4 h-4" />}
                className="w-full"
              >
                Execute Smoke Test
              </Button>
            </div>
          </form>
        </div>

        {/* Results */}
        <div className="lg:col-span-6 bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-[#1D2A22]">
            <span className="font-mono text-xs font-bold text-[#FF65D8] uppercase tracking-wider">
              SMOKE TEST TELEMETRY
            </span>
            {result && <Badge status={result.success ? 'PASS' : 'FAIL'} />}
          </div>

          {result ? (
            <div className="space-y-3 font-mono text-xs">
              <div className="flex justify-between p-2.5 bg-[#070A08] border border-[#1D2A22] rounded-[2px]">
                <span className="text-[#849188]">Execution Result:</span>
                <span className={result.success ? 'text-[#38FF8E] font-bold' : 'text-[#FF5A78] font-bold'}>
                  {result.success ? 'SUCCESS (PASS)' : 'FAILED'}
                </span>
              </div>
              <div className="flex justify-between p-2.5 bg-[#070A08] border border-[#1D2A22] rounded-[2px]">
                <span className="text-[#849188]">Actual Status:</span>
                <span className="text-[#F4F8F4]">{result.actualStatus}</span>
              </div>
              <div className="flex justify-between p-2.5 bg-[#070A08] border border-[#1D2A22] rounded-[2px]">
                <span className="text-[#849188]">Latency:</span>
                <span className="text-[#F4F8F4]">{result.latencyMs} ms</span>
              </div>
              {result.diagnostic && (
                <div className={`p-3 border rounded-[2px] text-[11px] ${result.success ? 'bg-[#38FF8E]/10 border-[#38FF8E]/40 text-[#38FF8E]' : 'bg-[#FF5A78]/10 border-[#FF5A78]/40 text-[#FF5A78]'}`}>
                  {result.diagnostic}
                </div>
              )}
              {result.jsonAssertionDiagnostic && (
                <div className={`p-3 border rounded-[2px] text-[11px] ${result.jsonAssertionPassed ? 'bg-[#38FF8E]/10 border-[#38FF8E]/40 text-[#38FF8E]' : 'bg-[#FF5A78]/10 border-[#FF5A78]/40 text-[#FF5A78]'}`}>
                  {result.jsonAssertionDiagnostic}
                </div>
              )}
              {result.errorMessage && result.errorMessage !== result.diagnostic && (
                <div className="p-3 bg-[#FF5A78]/10 border border-[#FF5A78]/40 text-[#FF5A78] rounded-[2px] text-[11px]">
                  {result.errorMessage}
                </div>
              )}
            </div>
          ) : (
            <div className="py-12 text-center text-xs font-mono text-[#849188]">
              Execute smoke test to inspect status codes and JSON Pointer evaluation.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
