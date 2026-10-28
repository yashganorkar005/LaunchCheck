import React from 'react';
import { ShieldCheck, ShieldAlert, Lock } from 'lucide-react';
import { Badge } from '../ui/Badge';

export function SecurityTrust() {
  return (
    <section id="security" className="py-24 bg-[#070A08] relative border-t border-[#1D2A22]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-3xl mb-16">
          <div className="text-xs font-mono uppercase tracking-widest text-[#38FF8E] mb-2">
            // FAIL-CLOSED TRANSPORT ARCHITECTURE
          </div>
          <h2 className="text-2xl sm:text-4xl font-extrabold tracking-tight text-[#F4F8F4]">
            Security built into every probe.
          </h2>
          <p className="mt-3 text-sm text-[#C0CAC2] leading-relaxed">
            SSRF defenses and TLS verification protect outbound checks.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          {/* Left Column: Visual Pipeline Diagram */}
          <div className="lg:col-span-6 bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] font-mono text-xs space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-[#1D2A22]">
              <span className="text-[#65F8FF] font-bold">PIPELINE SECURITY INSPECTION</span>
              <Badge status="PASS" label="FAIL-CLOSED" />
            </div>

            <div className="space-y-3">
              <div className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-1">
                <div className="flex items-center justify-between text-[#849188]">
                  <span>STEP 1: HOST RESOLUTION</span>
                  <span className="text-[#38FF8E]">DNS LOOKUP</span>
                </div>
                <div className="text-[#F4F8F4] font-bold">api.example.com → 93.184.216.34</div>
              </div>

              <div className="p-3 bg-[#070A08] border border-[#25352B] rounded-[2px] space-y-1 relative">
                <div className="absolute -top-3 left-4 px-2 bg-[#111913] border border-[#38FF8E]/40 text-[#38FF8E] text-[10px]">
                  POLICY VALIDATION
                </div>
                <div className="flex items-center justify-between text-[#849188] pt-1">
                  <span>RFC 1918 / Loopback / Metadata:</span>
                  <span className="text-[#38FF8E] font-bold">CLEARED (0 MATCHES)</span>
                </div>
                <div className="text-[11px] text-[#C0CAC2]">
                  IP address confirmed as public global unicast. Metadata <code className="text-[#FF5A78]">169.254.169.254</code> blocked.
                </div>
              </div>

              <div className="p-3 bg-[#070A08] border border-[#1D2A22] rounded-[2px] space-y-1">
                <div className="flex items-center justify-between text-[#849188]">
                  <span>STEP 3: SOCKET CONNECTION</span>
                  <span className="text-[#65F8FF]">IP PINNED</span>
                </div>
                <div className="text-[#F4F8F4] font-bold">Direct TLS handshake to 93.184.216.34:443 (SNI preserved)</div>
              </div>
            </div>
          </div>

          {/* Right Column: Key Security Guarantees */}
          <div className="lg:col-span-6 space-y-6">
            <div className="flex items-start space-x-4">
              <div className="w-8 h-8 rounded-[2px] bg-[#38FF8E]/10 border border-[#38FF8E]/40 flex items-center justify-center text-[#38FF8E] flex-shrink-0 mt-1">
                <ShieldCheck className="w-4 h-4" />
              </div>
              <div>
                <h3 className="font-mono text-sm font-bold uppercase tracking-tight text-[#F4F8F4]">
                  Upfront DNS Pinning & Rebinding Defense
                </h3>
                <p className="mt-1 text-xs text-[#C0CAC2] leading-relaxed font-sans">
                  Hostnames resolve once prior to connection. IP addresses are validated against forbidden ranges and bound directly to the socket.
                </p>
              </div>
            </div>

            <div className="flex items-start space-x-4">
              <div className="w-8 h-8 rounded-[2px] bg-[#FF5A78]/10 border border-[#FF5A78]/40 flex items-center justify-center text-[#FF5A78] flex-shrink-0 mt-1">
                <ShieldAlert className="w-4 h-4" />
              </div>
              <div>
                <h3 className="font-mono text-sm font-bold uppercase tracking-tight text-[#F4F8F4]">
                  Cloud Metadata & Private Subnet Rejection
                </h3>
                <p className="mt-1 text-xs text-[#C0CAC2] leading-relaxed font-sans">
                  Automatic rejection of loopback, private networks, and cloud instance metadata endpoints.
                </p>
              </div>
            </div>

            <div className="flex items-start space-x-4">
              <div className="w-8 h-8 rounded-[2px] bg-[#65F8FF]/10 border border-[#65F8FF]/40 flex items-center justify-center text-[#65F8FF] flex-shrink-0 mt-1">
                <Lock className="w-4 h-4" />
              </div>
              <div>
                <h3 className="font-mono text-sm font-bold uppercase tracking-tight text-[#F4F8F4]">
                  Strict Protocol Downgrade Protection
                </h3>
                <p className="mt-1 text-xs text-[#C0CAC2] leading-relaxed font-sans">
                  Plaintext HTTP endpoints are flagged with security warnings while TLS certificates verify expiration horizons.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
