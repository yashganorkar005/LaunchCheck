import React from 'react';
import { BookOpen, Terminal, Layers, Shield, Cpu, Radio, CheckCircle2, AlertTriangle, ArrowRight, Code } from 'lucide-react';
import { Button } from '../ui/Button';

interface DocumentationViewProps {
  onNavigateTab?: (tab: 'audit' | 'cors' | 'smoke' | 'projects' | 'history' | 'security') => void;
}

export function DocumentationView({ onNavigateTab }: DocumentationViewProps) {
  return (
    <div className="space-y-8 max-w-4xl mx-auto">
      <div>
        <div className="text-xs font-mono uppercase tracking-widest text-[#65F8FF] mb-1">
          // DEVELOPER MANUAL & RUNTIME SPECIFICATION
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#F4F8F4]">
          LaunchCheck Platform Documentation
        </h1>
        <p className="text-xs sm:text-sm text-[#C0CAC2] mt-1">
          Technical guide for running preflight audits, simulating CORS headers, executing smoke tests, and integrating APIs.
        </p>
      </div>

      <div className="space-y-6">
        {/* Getting Started Overview */}
        <div className="bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4">
          <div className="flex items-center space-x-2 text-[#38FF8E]">
            <Terminal className="w-4 h-4" />
            <h2 className="font-mono text-sm font-bold uppercase tracking-wider">
              1. Getting Started Workflow
            </h2>
          </div>
          <p className="text-xs sm:text-sm text-[#C0CAC2] leading-relaxed font-sans">
            LaunchCheck is a pre-deployment verification engine designed to evaluate web services before promoting code to production. It runs fail-closed network diagnostics across three distinct pillars:
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
            <div className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-2">
              <div className="font-mono text-xs font-bold text-[#38FF8E]">Preflight Audit</div>
              <p className="text-[11px] text-[#849188]">
                Probes frontend web and backend API endpoints with DNS resolution, TLS certificate validation, and reachability tests.
              </p>
              {onNavigateTab && (
                <button
                  onClick={() => onNavigateTab('audit')}
                  className="text-[10px] font-mono text-[#38FF8E] hover:underline flex items-center space-x-1 cursor-pointer"
                >
                  <span>Open Studio</span>
                  <ArrowRight className="w-2.5 h-2.5" />
                </button>
              )}
            </div>

            <div className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-2">
              <div className="font-mono text-xs font-bold text-[#65F8FF]">CORS Diagnostic Lab</div>
              <p className="text-[11px] text-[#849188]">
                Simulates preflight OPTIONS handshakes from any origin to verify Access-Control headers and wildcard vulnerabilities.
              </p>
              {onNavigateTab && (
                <button
                  onClick={() => onNavigateTab('cors')}
                  className="text-[10px] font-mono text-[#65F8FF] hover:underline flex items-center space-x-1 cursor-pointer"
                >
                  <span>Open Lab</span>
                  <ArrowRight className="w-2.5 h-2.5" />
                </button>
              )}
            </div>

            <div className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-2">
              <div className="font-mono text-xs font-bold text-[#FF65D8]">API Smoke Tests</div>
              <p className="text-[11px] text-[#849188]">
                Deterministic status code verification and RFC 6901 JSON pointer evaluations against live JSON payloads.
              </p>
              {onNavigateTab && (
                <button
                  onClick={() => onNavigateTab('smoke')}
                  className="text-[10px] font-mono text-[#FF65D8] hover:underline flex items-center space-x-1 cursor-pointer"
                >
                  <span>Run Tests</span>
                  <ArrowRight className="w-2.5 h-2.5" />
                </button>
              )}
            </div>
          </div>
        </div>

        {/* REST API Endpoints Reference */}
        <div className="bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4 font-mono text-xs">
          <div className="flex items-center space-x-2 text-[#65F8FF]">
            <Code className="w-4 h-4" />
            <h2 className="text-sm font-bold uppercase tracking-wider">
              2. Core REST API Endpoints
            </h2>
          </div>

          <div className="space-y-3 pt-2">
            <div className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-1">
              <div className="flex items-center space-x-2">
                <span className="px-1.5 py-0.5 bg-[#38FF8E]/10 text-[#38FF8E] font-bold text-[10px] rounded-[1px]">POST</span>
                <span className="text-[#F4F8F4] font-bold">/api/v1/audits</span>
              </div>
              <p className="text-[11px] text-[#849188]">
                Executes preflight probes for frontend and backend targets with optional smoke test checks.
              </p>
            </div>

            <div className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-1">
              <div className="flex items-center space-x-2">
                <span className="px-1.5 py-0.5 bg-[#38FF8E]/10 text-[#38FF8E] font-bold text-[10px] rounded-[1px]">POST</span>
                <span className="text-[#F4F8F4] font-bold">/api/v1/audits/smoke</span>
              </div>
              <p className="text-[11px] text-[#849188]">
                Runs a standalone, rate-limited smoke test against a target URL with RFC 6901 pointer assertion.
              </p>
            </div>

            <div className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-1">
              <div className="flex items-center space-x-2">
                <span className="px-1.5 py-0.5 bg-[#65F8FF]/10 text-[#65F8FF] font-bold text-[10px] rounded-[1px]">GET</span>
                <span className="text-[#F4F8F4] font-bold">/api/v1/audits/cors</span>
              </div>
              <p className="text-[11px] text-[#849188]">
                Simulates preflight CORS evaluation using parameters: <code className="text-[#38FF8E]">backendUrl</code>, <code className="text-[#38FF8E]">origin</code>, <code className="text-[#38FF8E]">endpointPath</code>.
              </p>
            </div>

            <div className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-1">
              <div className="flex items-center space-x-2">
                <span className="px-1.5 py-0.5 bg-[#65F8FF]/10 text-[#65F8FF] font-bold text-[10px] rounded-[1px]">GET</span>
                <span className="text-[#F4F8F4] font-bold">/api/v1/audits/history</span>
              </div>
              <p className="text-[11px] text-[#849188]">
                Retrieves user-isolated audit logs. Supports optional query parameter <code className="text-[#38FF8E]">?type=PREFLIGHT|CORS|SMOKE</code>.
              </p>
            </div>

            <div className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-1">
              <div className="flex items-center space-x-2">
                <span className="px-1.5 py-0.5 bg-[#FFE282]/10 text-[#FFE282] font-bold text-[10px] rounded-[1px]">AUTH</span>
                <span className="text-[#F4F8F4] font-bold">/api/v1/auth/login & /register</span>
              </div>
              <p className="text-[11px] text-[#849188]">
                Issues cryptographically signed HMAC-SHA256 JWT Bearer tokens for authenticated operator sessions.
              </p>
            </div>
          </div>
        </div>

        {/* Troubleshooting & Operational Boundaries */}
        <div className="bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4">
          <div className="flex items-center space-x-2 text-[#FFE282]">
            <AlertTriangle className="w-4 h-4" />
            <h2 className="font-mono text-sm font-bold uppercase tracking-wider">
              3. Operational Limits & Troubleshooting
            </h2>
          </div>

          <div className="space-y-3 text-xs text-[#C0CAC2] leading-relaxed">
            <div>
              <span className="font-bold text-[#F4F8F4] font-mono">• SSRF Private Range Rejections:</span>
              <p className="text-[#849188] mt-0.5">
                Target URLs that resolve to loopback (127.0.0.0/8), RFC 1918 private subnets (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16), or cloud metadata services (169.254.169.254) fail closed with security errors. Targets must be globally routable.
              </p>
            </div>

            <div>
              <span className="font-bold text-[#F4F8F4] font-mono">• Rate Limiting & Concurrency:</span>
              <p className="text-[#849188] mt-0.5">
                Outbound audits are rate-limited to 15 executions per minute per client IP, and governed by a concurrency semaphore of 4 parallel executions. Excess requests receive HTTP 429 Too Many Requests.
              </p>
            </div>

            <div>
              <span className="font-bold text-[#F4F8F4] font-mono">• Session Persistence & Recovery:</span>
              <p className="text-[#849188] mt-0.5">
                Operator tokens are validated via <code className="text-[#38FF8E] font-mono">/api/v1/auth/me</code> on page load. If an operator session expires or is terminated, client state resets to unauthenticated guest mode without losing durable server-side audit logs.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
