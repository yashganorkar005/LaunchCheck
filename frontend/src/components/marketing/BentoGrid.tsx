import React from 'react';
import { ShieldAlert, Lock, Radio, Cpu, FileCode2, History, ArrowRight } from 'lucide-react';
import { SpotlightCard } from '../ui/SpotlightCard';
import { Badge } from '../ui/Badge';

interface BentoGridProps {
  onSelectFeature?: (featureKey: string) => void;
}

export function BentoGrid({ onSelectFeature }: BentoGridProps) {
  return (
    <section id="features" className="py-20 bg-[#070A08] relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-3xl mb-12">
          <div className="text-xs font-mono uppercase tracking-widest text-[#38FF8E] mb-2">
            // HARDENED PREFLIGHT CAPABILITIES
          </div>
          <h2 className="text-2xl sm:text-4xl font-extrabold tracking-tight text-[#F4F8F4]">
            Engineered for production reality.
          </h2>
          <p className="mt-3 text-sm text-[#C0CAC2] leading-relaxed">
            Enterprise-grade URL safety policy with upfront DNS pinning and SSRF defense.
          </p>
        </div>

        {/* Asymmetrical Bento Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-12 gap-5">
          {/* Card 1: Target Safety / SSRF Safe Transport (Span 7) */}
          <SpotlightCard className="lg:col-span-7 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded-[2px] bg-[#38FF8E]/10 border border-[#38FF8E]/40 flex items-center justify-center text-[#38FF8E]">
                  <ShieldAlert className="w-5 h-5" />
                </div>
                <Badge status="PASS" label="FAIL-CLOSED DEFENSE" />
              </div>
              <h3 className="text-lg font-mono font-bold text-[#F4F8F4] uppercase tracking-tight">
                Target Safety
              </h3>
              <p className="mt-2 text-xs text-[#C0CAC2] leading-relaxed font-sans">
                Block unsafe destinations before probing. Loopback, private subnets, and metadata endpoints are automatically rejected.
              </p>
            </div>

            <div className="mt-6 p-3.5 bg-[#070A08] border border-[#1D2A22] rounded-[2px] font-mono text-[11px] space-y-1.5">
              <div className="flex items-center justify-between text-[#849188]">
                <span>DNS RESOLUTION SET:</span>
                <span className="text-[#38FF8E]">PUBLIC CANDIDATES PINNED</span>
              </div>
              <div className="flex items-center justify-between text-[#849188]">
                <span>IP-FAILOVER POLICY:</span>
                <span className="text-[#F4F8F4]">STRICT TO VALIDATED RANGE</span>
              </div>
            </div>
          </SpotlightCard>

          {/* Card 2: TLS Diagnostics (Span 5) */}
          <SpotlightCard className="lg:col-span-5 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded-[2px] bg-[#65F8FF]/10 border border-[#65F8FF]/40 flex items-center justify-center text-[#65F8FF]">
                  <Lock className="w-5 h-5" />
                </div>
                <Badge status="INFO" label="STRICT SNI" />
              </div>
              <h3 className="text-lg font-mono font-bold text-[#F4F8F4] uppercase tracking-tight">
                TLS Diagnostics
              </h3>
              <p className="mt-2 text-xs text-[#C0CAC2] leading-relaxed font-sans">
                Verify certificates and hostnames. Surfaces expiration dates and SAN mismatches.
              </p>
            </div>

            <div className="mt-6 p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] font-mono text-[11px] space-y-1 text-[#849188]">
              <div>Certificate Status: <span className="text-[#38FF8E]">Valid & Trusted</span></div>
              <div>Expiration Horizon: <span className="text-[#65F8FF]">284 Days Remaining</span></div>
            </div>
          </SpotlightCard>

          {/* Card 3: CORS Checks (Span 4) */}
          <SpotlightCard className="lg:col-span-4 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded-[2px] bg-[#38FF8E]/10 border border-[#38FF8E]/40 flex items-center justify-center text-[#38FF8E]">
                  <Radio className="w-5 h-5" />
                </div>
                <Badge status="PASS" label="PREFLIGHT" />
              </div>
              <h3 className="text-base font-mono font-bold text-[#F4F8F4] uppercase tracking-tight">
                CORS Checks
              </h3>
              <p className="mt-2 text-xs text-[#C0CAC2] leading-relaxed font-sans">
                See which origins and headers are allowed. Identifies wildcard vulnerabilities before deployment.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-[#1D2A22] text-[11px] font-mono text-[#849188]">
              Simulates cross-origin requests safely.
            </div>
          </SpotlightCard>

          {/* Card 4: API Smoke Tests (Span 4) */}
          <SpotlightCard className="lg:col-span-4 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded-[2px] bg-[#FF65D8]/10 border border-[#FF65D8]/40 flex items-center justify-center text-[#FF65D8]">
                  <Cpu className="w-5 h-5" />
                </div>
                <Badge status="WARN" label="UP TO 5 CHECKS" />
              </div>
              <h3 className="text-base font-mono font-bold text-[#F4F8F4] uppercase tracking-tight">
                API Smoke Tests
              </h3>
              <p className="mt-2 text-xs text-[#C0CAC2] leading-relaxed font-sans">
                Validate status codes, headers, and JSON values with strict response time measurement.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-[#1D2A22] text-[11px] font-mono text-[#849188]">
              Zero regex approximations.
            </div>
          </SpotlightCard>

          {/* Card 5: Project Audits (Span 4) */}
          <SpotlightCard className="lg:col-span-4 flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded-[2px] bg-[#65F8FF]/10 border border-[#65F8FF]/40 flex items-center justify-center text-[#65F8FF]">
                  <FileCode2 className="w-5 h-5" />
                </div>
                <Badge status="PASS" label="MULTI-TENANT" />
              </div>
              <h3 className="text-base font-mono font-bold text-[#F4F8F4] uppercase tracking-tight">
                Project Audits
              </h3>
              <p className="mt-2 text-xs text-[#C0CAC2] leading-relaxed font-sans">
                Keep deployment checks organized by project. Secure database isolation per user.
              </p>
            </div>
            <div className="mt-4 pt-3 border-t border-[#1D2A22] text-[11px] font-mono text-[#849188]">
              Compound key database isolation.
            </div>
          </SpotlightCard>
        </div>
      </div>
    </section>
  );
}
