import React, { useEffect, useState } from 'react';
import { History, Clock, ArrowRight, FileCode2, Lock, AlertCircle, RefreshCw, Filter, Trash2 } from 'lucide-react';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { useToast } from '../ui/Toast';
import { AuditResponse, AuditSummary } from '../../types';
import { api, ApiError } from '../../lib/api';

interface HistoryStudioProps {
  onSelectAudit: (audit: AuditResponse) => void;
  token?: string | null;
  onOpenAuth?: () => void;
}

export function HistoryStudio({ onSelectAudit, token, onOpenAuth }: HistoryStudioProps) {
  const [audits, setAudits] = useState<AuditSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [fetchingDetailId, setFetchingDetailId] = useState<string | null>(null);
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const { toast } = useToast();

  const activeToken = token || localStorage.getItem('lc_token');

  const fetchHistory = async (typeFilter = selectedType) => {
    if (!activeToken) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const queryType = typeFilter === 'ALL' ? undefined : typeFilter;
      const data = await api.audits.getUserHistory(activeToken, 0, 20, queryType);
      setAudits(data.content || []);
    } catch (err: any) {
      console.error('Failed to load audit history', err);
      const message = err instanceof ApiError ? err.message : (err.message || 'Failed to load audit history.');
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory(selectedType);

    const handleAuditRecorded = () => {
      fetchHistory(selectedType);
    };

    window.addEventListener('launchcheck:audit-recorded', handleAuditRecorded);
    return () => {
      window.removeEventListener('launchcheck:audit-recorded', handleAuditRecorded);
    };
  }, [activeToken, selectedType]);

  const handleViewReport = async (summary: AuditSummary) => {
    if (!activeToken) {
      toast.error('Authentication required', 'Please sign in to inspect historical audit reports.');
      return;
    }

    setFetchingDetailId(summary.id);
    try {
      const fullDetail = await api.audits.getHistoryDetail(summary.id, activeToken);
      onSelectAudit(fullDetail);
    } catch (err: any) {
      const message = err instanceof ApiError ? err.message : (err.message || 'Could not fetch audit details.');
      toast.error('Failed to load report', message);
    } finally {
      setFetchingDetailId(null);
    }
  };

  const handleDeleteRecord = async (id: string) => {
    if (!activeToken) return;
    if (!confirm('Are you sure you want to delete this audit record?')) return;
    try {
      await api.audits.deleteRecord(id, activeToken);
      toast.success('Record deleted', 'Audit history record has been removed.');
      fetchHistory(selectedType);
    } catch (err: any) {
      const message = err instanceof ApiError ? err.message : (err.message || 'Failed to delete record.');
      toast.error('Deletion failed', message);
    }
  };

  const handleClearHistory = async () => {
    if (!activeToken) return;
    if (!confirm('Are you sure you want to clear ALL your audit history records? This action cannot be undone.')) return;
    try {
      await api.audits.clearHistory(activeToken);
      toast.success('History cleared', 'All audit history records have been removed.');
      fetchHistory(selectedType);
    } catch (err: any) {
      const message = err instanceof ApiError ? err.message : (err.message || 'Failed to clear history.');
      toast.error('Clear history failed', message);
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="text-xs font-mono uppercase tracking-widest text-[#38FF8E] mb-1">
            // UNIFIED PERSISTENT AUDIT LOGS
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#F4F8F4]">
            Execution History
          </h1>
          <p className="text-xs sm:text-sm text-[#C0CAC2] mt-1">
            Review preflight audits, CORS diagnoses, and API smoke tests with complete diagnostic snapshots.
          </p>
        </div>

        {activeToken && (
          <div className="flex items-center space-x-3">
            {audits.length > 0 && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleClearHistory}
                icon={<Trash2 className="w-3.5 h-3.5 text-[#FF5A78]" />}
                className="text-[#FF5A78] border-[#FF5A78]/40 hover:bg-[#FF5A78]/10"
              >
                Clear History
              </Button>
            )}
            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchHistory(selectedType)}
              isLoading={loading}
              icon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              Refresh
            </Button>
          </div>
        )}
      </div>

      {/* Filter Tabs */}
      {activeToken && (
        <div className="flex flex-wrap items-center gap-2 pb-2 border-b border-[#1D2A22]">
          {[
            { id: 'ALL', label: 'All Audits' },
            { id: 'PREFLIGHT', label: 'Preflight Audits' },
            { id: 'CORS', label: 'CORS Diagnoses' },
            { id: 'SMOKE', label: 'API Smoke Tests' }
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setSelectedType(tab.id)}
              className={`px-3 py-1.5 rounded-[2px] text-xs font-mono transition-colors cursor-pointer ${
                selectedType === tab.id
                  ? 'bg-[#38FF8E]/10 text-[#38FF8E] border border-[#38FF8E]/40 font-bold'
                  : 'bg-[#0C110E] text-[#849188] border border-[#25352B] hover:text-[#F4F8F4]'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      )}

      {!activeToken ? (
        <div className="bg-[#0C110E] border border-[#25352B] p-12 text-center rounded-[2px] space-y-4">
          <Lock className="w-10 h-10 text-[#FFE282] mx-auto" />
          <div className="font-mono text-sm font-bold text-[#F4F8F4]">Operator Authentication Required</div>
          <p className="text-xs text-[#C0CAC2] max-w-md mx-auto">
            Historical audit logs and diagnostic telemetry are cryptographically bound to authenticated operator accounts.
          </p>
          {onOpenAuth && (
            <Button
              variant="primary"
              size="sm"
              onClick={onOpenAuth}
              icon={<ArrowRight className="w-3.5 h-3.5" />}
            >
              Sign In to View History
            </Button>
          )}
        </div>
      ) : loading ? (
        <div className="py-20 text-center font-mono text-xs text-[#849188] animate-pulse">
          Loading execution history from secure storage...
        </div>
      ) : error ? (
        <div className="bg-[#0C110E] border border-[#FF5A78]/40 p-8 rounded-[2px] space-y-4">
          <div className="flex items-center space-x-2 text-[#FF5A78]">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span className="font-mono text-xs font-bold uppercase">Audit History Retrieval Error</span>
          </div>
          <p className="text-xs font-mono text-[#C0CAC2]">{error}</p>
          <Button variant="outline" size="sm" onClick={() => fetchHistory(selectedType)}>
            Retry Request
          </Button>
        </div>
      ) : audits.length === 0 ? (
        <div className="bg-[#0C110E] border border-[#25352B] p-12 text-center rounded-[2px] space-y-3">
          <History className="w-10 h-10 text-[#849188] mx-auto" />
          <div className="font-mono text-sm text-[#F4F8F4]">No Audit History Recorded</div>
          <p className="text-xs text-[#C0CAC2] max-w-md mx-auto">
            Execute preflight audits, CORS tests, or smoke tests to populate persistent historical logs.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {audits.map((audit) => (
            <div
              key={audit.id}
              className="bg-[#0C110E] border border-[#25352B] p-5 rounded-[2px] flex flex-col lg:flex-row lg:items-center justify-between gap-4 hover:border-[#38FF8E]/40 transition-colors"
            >
              <div className="space-y-2 font-mono text-xs">
                <div className="flex flex-wrap items-center gap-3">
                  <span className="px-2 py-0.5 bg-[#1D2A22] text-[#65F8FF] font-bold text-[10px] rounded-[2px]">
                    {audit.auditType || 'PREFLIGHT'}
                  </span>
                  <Badge status={audit.overallStatus} size="sm" />
                  <div className="text-[11px] text-[#849188] flex items-center space-x-2">
                    <Clock className="w-3 h-3" />
                    <span>{new Date(audit.startedAt).toLocaleString()}</span>
                    <span>•</span>
                    <span>Duration: {audit.durationMs} ms</span>
                  </div>
                </div>

                {audit.targetLabel && (
                  <div className="text-[11px] text-[#C0CAC2] truncate max-w-xl">
                    Target: <span className="text-[#38FF8E]">{audit.targetLabel}</span>
                  </div>
                )}

                <div className="flex flex-wrap items-center gap-4 text-[11px] text-[#849188] pt-1 border-t border-[#1D2A22]">
                  {audit.auditType === 'CORS' ? (
                    <div>
                      CORS Status: <span className="text-[#F4F8F4]">{audit.corsStatus || audit.overallStatus}</span>
                    </div>
                  ) : audit.auditType === 'SMOKE' ? (
                    <div>
                      Smoke Test Status: <span className="text-[#F4F8F4]">{audit.overallStatus}</span>
                    </div>
                  ) : (
                    <>
                      <div>
                        Frontend: <span className="text-[#F4F8F4]">{audit.frontendStatus || 'N/A'}</span>
                        {audit.frontendHttpStatus && <span className="text-[#849188]"> ({audit.frontendHttpStatus})</span>}
                      </div>
                      <span>•</span>
                      <div>
                        Backend: <span className="text-[#F4F8F4]">{audit.backendStatus || 'N/A'}</span>
                        {audit.backendHttpStatus && <span className="text-[#849188]"> ({audit.backendHttpStatus})</span>}
                      </div>
                      <span>•</span>
                      <div>
                        CORS: <span className="text-[#F4F8F4]">{audit.corsStatus || 'N/A'}</span>
                      </div>
                      <span>•</span>
                      <div>
                        Smoke Tests: <span className="text-[#38FF8E]">{audit.apiSmokeTestPassedCount}</span>/{audit.apiSmokeTestCount} Passed
                      </div>
                    </>
                  )}
                </div>
              </div>

              <div className="flex items-center space-x-3 self-end lg:self-center">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleDeleteRecord(audit.id)}
                  icon={<Trash2 className="w-3.5 h-3.5 text-[#FF5A78]" />}
                  title="Delete Record"
                />
                <Button
                  variant="outline"
                  size="sm"
                  isLoading={fetchingDetailId === audit.id}
                  onClick={() => handleViewReport(audit)}
                  icon={<FileCode2 className="w-3.5 h-3.5" />}
                >
                  View Full Report
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
