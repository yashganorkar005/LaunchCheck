import React from 'react';
import {
  ShieldCheck,
  AlertTriangle,
  XCircle,
  Globe,
  Server,
  Lock,
  Radio,
  Cpu,
  ArrowLeft,
  CheckCircle2,
  Clock,
  FileCode2,
  Download,
  FileJson
} from 'lucide-react';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { AuditResponse } from '../../types';
import {
  auditFilename,
  createAuditJsonBlob,
  createAuditPdfBlob,
  downloadAuditBlob
} from '../../lib/auditReport';

interface ResultsViewProps {
  auditResult: AuditResponse;
  onBackToStudio: () => void;
}

function getDiagnosticStyle(status?: string) {
  switch (status?.toUpperCase()) {
    case 'PASS':
      return 'bg-[#38FF8E]/10 border-[#38FF8E]/40 text-[#38FF8E]';
    case 'WARN':
      return 'bg-[#FFE282]/10 border-[#FFE282]/40 text-[#FFE282]';
    case 'FAIL':
      return 'bg-[#FF5A78]/10 border-[#FF5A78]/40 text-[#FF5A78]';
    case 'SKIPPED':
      return 'bg-[#849188]/10 border-[#849188]/40 text-[#849188]';
    default:
      return 'bg-[#070A08] border-[#1D2A22] text-[#C0CAC2]';
  }
}

export function ResultsView({ auditResult, onBackToStudio }: ResultsViewProps) {
  const frontendResult = auditResult.targets.find(t => t.role === 'FRONTEND');

  const handleDownloadPdf = () => {
    downloadAuditBlob(createAuditPdfBlob(auditResult), auditFilename(auditResult, 'pdf'));
  };

  const handleDownloadJson = () => {
    downloadAuditBlob(createAuditJsonBlob(auditResult), auditFilename(auditResult, 'json'));
  };
  const backendResult = auditResult.targets.find(t => t.role === 'BACKEND');
  const certificateExpiry = frontendResult?.tls.certificateExpiresAt
    ? new Date(frontendResult.tls.certificateExpiresAt)
    : undefined;
  const formattedCertificateExpiry = certificateExpiry && !Number.isNaN(certificateExpiry.getTime())
    ? certificateExpiry.toLocaleDateString()
    : 'Active';

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Header bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-[#1D2A22]">
        <div className="space-y-1">
          <button
            onClick={onBackToStudio}
            className="flex items-center space-x-1 text-xs font-mono text-[#849188] hover:text-[#38FF8E] transition-colors cursor-pointer mb-2"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Configure New Audit</span>
          </button>
          <div className="flex items-center space-x-3">
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#F4F8F4]">
              Preflight Telemetry Report
            </h1>
            <Badge status={auditResult.overallStatus} size="md" />
          </div>
          <div className="flex items-center space-x-4 text-xs font-mono text-[#849188] pt-1">
            <span>AUDIT ID: #{auditResult.auditId}</span>
            <span>•</span>
            <span className="flex items-center space-x-1">
              <Clock className="w-3 h-3" />
              <span>{new Date(auditResult.startedAt).toLocaleString()}</span>
            </span>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant="primary"
            size="sm"
            onClick={handleDownloadPdf}
            icon={<Download className="w-3.5 h-3.5" />}
          >
            Download Full Report
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={handleDownloadJson}
            icon={<FileJson className="w-3.5 h-3.5" />}
          >
            JSON
          </Button>
          <Button variant="outline" size="sm" onClick={onBackToStudio}>
            New Audit
          </Button>
        </div>
      </div>

      {/* Targets Overview Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Frontend Target Result */}
        {frontendResult && (
          <div className="bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-[#1D2A22]">
              <div className="flex items-center space-x-2">
                <Globe className="w-4 h-4 text-[#65F8FF]" />
                <span className="font-mono text-xs font-bold text-[#65F8FF] uppercase tracking-wider">
                  FRONTEND REACHABILITY
                </span>
              </div>
              <Badge status={frontendResult.status} size="sm" />
            </div>

            <div className="space-y-2 font-mono text-xs">
              <div className="text-[#F4F8F4] font-bold text-sm truncate">{frontendResult.host}</div>
              <div className="flex justify-between text-[#849188]">
                <span>HTTP Status Code:</span>
                <span className="text-[#38FF8E]">{frontendResult.httpStatus || 'N/A'}</span>
              </div>
              <div className="flex justify-between text-[#849188]">
                <span>Response Latency:</span>
                <span className="text-[#F4F8F4]">{frontendResult.responseTimeMs} ms</span>
              </div>
            </div>

            {frontendResult.diagnostic && (
              <div className={`p-3 border text-xs font-mono rounded-[2px] ${getDiagnosticStyle(frontendResult.status)}`}>
                {frontendResult.diagnostic}
              </div>
            )}
          </div>
        )}

        {/* Backend Target Result */}
        {backendResult && (
          <div className="bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-[#1D2A22]">
              <div className="flex items-center space-x-2">
                <Server className="w-4 h-4 text-[#38FF8E]" />
                <span className="font-mono text-xs font-bold text-[#38FF8E] uppercase tracking-wider">
                  BACKEND API TARGET
                </span>
              </div>
              <Badge status={backendResult.status} size="sm" />
            </div>

            <div className="space-y-2 font-mono text-xs">
              <div className="text-[#F4F8F4] font-bold text-sm truncate">{backendResult.host}</div>
              <div className="flex justify-between text-[#849188]">
                <span>HTTP Status Code:</span>
                <span className="text-[#38FF8E]">{backendResult.httpStatus || 'N/A'}</span>
              </div>
              <div className="flex justify-between text-[#849188]">
                <span>Response Latency:</span>
                <span className="text-[#F4F8F4]">{backendResult.responseTimeMs} ms</span>
              </div>
            </div>

            {backendResult.diagnostic && (
              <div className={`p-3 border text-xs font-mono rounded-[2px] ${getDiagnosticStyle(backendResult.status)}`}>
                {backendResult.diagnostic}
              </div>
            )}
          </div>
        )}
      </div>

      {/* TLS & CORS Diagnostics Section */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* TLS Certificate Telemetry */}
        {frontendResult?.tls && (
          <div className="bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-[#1D2A22]">
              <div className="flex items-center space-x-2">
                <Lock className="w-4 h-4 text-[#65F8FF]" />
                <span className="font-mono text-xs font-bold text-[#65F8FF] uppercase tracking-wider">
                  TLS CERTIFICATE TELEMETRY
                </span>
              </div>
              <Badge status={frontendResult.tls.status} size="sm" />
            </div>

            <div className="space-y-2 font-mono text-xs text-[#849188]">
              <div className="flex justify-between">
                <span>Valid Until:</span>
                <span className="text-[#38FF8E]">{formattedCertificateExpiry}</span>
              </div>
              {typeof frontendResult.tls.daysRemaining === 'number' && (
                <div className="flex justify-between">
                  <span>Days Remaining:</span>
                  <span className="text-[#65F8FF]">{frontendResult.tls.daysRemaining} days</span>
                </div>
              )}
              <div className={`p-3 border rounded-[2px] ${getDiagnosticStyle(frontendResult.tls.status)}`}>
                {frontendResult.tls.diagnostic}
              </div>
            </div>
          </div>
        )}

        {/* CORS Diagnostic Result */}
        {auditResult.cors && (
          <div className="bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-[#1D2A22]">
              <div className="flex items-center space-x-2">
                <Radio className="w-4 h-4 text-[#FF65D8]" />
                <span className="font-mono text-xs font-bold text-[#FF65D8] uppercase tracking-wider">
                  CORS PREFLIGHT EVALUATION
                </span>
              </div>
              <Badge status={auditResult.cors.status} size="sm" />
            </div>

            <div className="space-y-2 font-mono text-xs text-[#849188]">
              <div className="flex justify-between">
                <span>Access-Control-Allow-Origin:</span>
                <span className="text-[#38FF8E]">{auditResult.cors.responseHeaders?.allowOrigin || '*'}</span>
              </div>
              <div className="flex justify-between">
                <span>Allowed Methods:</span>
                <span className="text-[#F4F8F4]">{auditResult.cors.responseHeaders?.allowMethods || 'GET, POST, OPTIONS'}</span>
              </div>
              <div className="flex justify-between">
                <span>Credentials Permitted:</span>
                <span className="text-[#F4F8F4]">{auditResult.cors.responseHeaders?.allowCredentials || 'False'}</span>
              </div>
              {auditResult.cors.diagnostic && (
                <div className={`p-3 border rounded-[2px] mt-2 ${getDiagnosticStyle(auditResult.cors.status)}`}>
                  {auditResult.cors.diagnostic}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* HTTP Security Headers Assessments */}
      {auditResult.targets.some(t => t.securityHeaders && t.securityHeaders.length > 0) && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {auditResult.targets.map((target, idx) => target.securityHeaders && target.securityHeaders.length > 0 && (
            <div key={idx} className="bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-[#1D2A22]">
                <div className="flex items-center space-x-2">
                  <ShieldCheck className="w-4 h-4 text-[#38FF8E]" />
                  <span className="font-mono text-xs font-bold text-[#38FF8E] uppercase tracking-wider">
                    {target.role} SECURITY HEADERS TELEMETRY
                  </span>
                </div>
              </div>

              <div className="space-y-3 font-mono text-xs">
                {target.securityHeaders.map((sh, sIdx) => (
                  <div key={sIdx} className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-[#F4F8F4]">{sh.headerName}</span>
                      <Badge status={sh.status} size="sm" />
                    </div>
                    {sh.value && <div className="text-[10px] text-[#65F8FF] truncate">Value: {sh.value}</div>}
                    <div className="text-[11px] text-[#849188]">{sh.diagnostic}</div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Smoke Tests & JSON Pointer Assertions */}
      {auditResult.apiTests && auditResult.apiTests.length > 0 && (
        <div className="bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-[#1D2A22]">
            <div className="flex items-center space-x-2">
              <Cpu className="w-4 h-4 text-[#38FF8E]" />
              <span className="font-mono text-xs font-bold text-[#38FF8E] uppercase tracking-wider">
                API SMOKE TEST RESULTS ({auditResult.apiTests.length})
              </span>
            </div>
            <Badge status={auditResult.apiTests.every(s => s.status === 'PASS') ? 'PASS' : 'FAIL'} size="sm" />
          </div>

          <div className="space-y-3">
            {auditResult.apiTests.map((st, idx) => (
              <div key={idx} className="p-4 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-2 font-mono text-xs">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-[#F4F8F4]">{st.path}</span>
                  <Badge status={st.status} size="sm" />
                </div>
                <div className="flex items-center space-x-6 pt-1 text-[11px] text-[#849188]">
                  <div>Status: <span className="text-[#38FF8E]">{st.actualStatus || 'N/A'}</span> (Expected: {st.expectedStatus})</div>
                </div>
                {st.diagnostic && (
                  <div className={`p-2.5 rounded-[2px] border text-[11px] ${getDiagnosticStyle(st.status)}`}>
                    {st.diagnostic}
                  </div>
                )}
                {st.jsonAssertionDiagnostic && (
                  <div className={`p-2.5 rounded-[2px] border text-[11px] ${getDiagnosticStyle(st.jsonAssertionPassed ? 'PASS' : 'FAIL')}`}>
                    {st.jsonAssertionDiagnostic}
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
