import React, { useState } from 'react';
import { Radio, ShieldCheck, Zap, ArrowRight } from 'lucide-react';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Badge } from '../ui/Badge';
import { api, ApiError } from '../../lib/api';
import { CorsDiagnosticResult } from '../../types';

export function CorsStudio() {
  const [targetUrl, setTargetUrl] = useState('https://api.github.com');
  const [origin, setOrigin] = useState('https://github.com');
  const [endpointPath, setEndpointPath] = useState('/repos/octocat/Spoon-Knife');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<CorsDiagnosticResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleTestCors = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('lc_token');
      const res = await api.audits.checkCors(targetUrl, origin, endpointPath, token);
      setResult(res);
      window.dispatchEvent(new CustomEvent('launchcheck:audit-recorded'));
    } catch (err: any) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError(err.message || 'CORS diagnostic simulation failed.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <div className="text-xs font-mono uppercase tracking-widest text-[#65F8FF] mb-1">
          // CORS PREFLIGHT LAB
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#F4F8F4]">
          Simulate Cross-Origin Requests
        </h1>
        <p className="text-xs sm:text-sm text-[#C0CAC2] mt-1">
          Inspect Access-Control headers, allowed methods, credentials support, and wildcard vulnerabilities.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Form */}
        <div className="lg:col-span-6 bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-6">
          <form onSubmit={handleTestCors} className="space-y-4">
            <Input
              label="Backend Target URL"
              type="url"
              required
              value={targetUrl}
              onChange={(e) => setTargetUrl(e.target.value)}
              placeholder="https://api.example.com"
              icon={<Radio className="w-4 h-4" />}
              monospace
            />
            <Input
              label="Requesting Origin Header"
              type="text"
              required
              value={origin}
              onChange={(e) => setOrigin(e.target.value)}
              placeholder="https://trusted-client.com"
              monospace
            />
            <Input
              label="Endpoint Path"
              type="text"
              required
              value={endpointPath}
              onChange={(e) => setEndpointPath(e.target.value)}
              placeholder="/posts/1"
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
                Simulate CORS Preflight
              </Button>
            </div>
          </form>
        </div>

        {/* Results Panel */}
        <div className="lg:col-span-6 bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-[#1D2A22]">
            <span className="font-mono text-xs font-bold text-[#65F8FF] uppercase tracking-wider">
              SIMULATION REPORT
            </span>
            {result && <Badge status={result.status} />}
          </div>

          {result ? (
            <div className="space-y-3 font-mono text-xs">
              <div className="flex justify-between p-2.5 bg-[#070A08] border border-[#1D2A22] rounded-[2px]">
                <span className="text-[#849188]">Access-Control-Allow-Origin:</span>
                <span className="text-[#38FF8E] font-bold">{result.responseHeaders?.allowOrigin || 'Not Set'}</span>
              </div>
              <div className="flex justify-between p-2.5 bg-[#070A08] border border-[#1D2A22] rounded-[2px]">
                <span className="text-[#849188]">Access-Control-Allow-Methods:</span>
                <span className="text-[#F4F8F4]">{result.responseHeaders?.allowMethods || 'N/A'}</span>
              </div>
              <div className="flex justify-between p-2.5 bg-[#070A08] border border-[#1D2A22] rounded-[2px]">
                <span className="text-[#849188]">Access-Control-Allow-Credentials:</span>
                <span className="text-[#F4F8F4]">{result.responseHeaders?.allowCredentials || 'N/A'}</span>
              </div>
              <div className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] text-[11px] text-[#C0CAC2] space-y-1">
                <div className="text-[#65F8FF] font-bold">Diagnostic Evaluation:</div>
                <p>{result.diagnostic || 'CORS headers analyzed.'}</p>
              </div>
            </div>
          ) : (
            <div className="py-12 text-center text-xs font-mono text-[#849188]">
              Configure target and origin above to simulate CORS preflight headers.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
