import React from 'react';
import { ShieldCheck, Lock, Server, ShieldAlert } from 'lucide-react';
import { Badge } from '../ui/Badge';

export function SecurityDocView() {
  return (
    <div className="space-y-8 max-w-4xl mx-auto">
      <div>
        <div className="text-xs font-mono uppercase tracking-widest text-[#38FF8E] mb-1">
          // ENTERPRISE SECURITY ARCHITECTURE
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#F4F8F4]">
          SSRF Defense & IP Pinning Specifications
        </h1>
        <p className="text-xs sm:text-sm text-[#C0CAC2] mt-1">
          Comprehensive review of LaunchCheck's fail-closed network transport policies and data isolation guarantees.
        </p>
      </div>

      <div className="bg-[#0C110E] border border-[#25352B] p-8 rounded-[2px] space-y-6 font-sans text-xs sm:text-sm text-[#C0CAC2] leading-relaxed">
        <div className="space-y-3">
          <h3 className="font-mono text-base font-bold text-[#F4F8F4] uppercase tracking-tight flex items-center space-x-2">
            <ShieldCheck className="w-5 h-5 text-[#38FF8E]" />
            <span>1. Upfront DNS Resolution & Pinning</span>
          </h3>
          <p>
            When LaunchCheck probes a target endpoint, hostname resolution occurs exactly once prior to connection establishment. The resolved IP candidates are immediately evaluated against global unicast safety policies. Once an approved IP is selected, the socket connection is bound directly to that IP address while preserving the correct SNI (Server Name Indication) header for TLS verification.
          </p>
        </div>

        <div className="space-y-3 pt-4 border-t border-[#1D2A22]">
          <h3 className="font-mono text-base font-bold text-[#F4F8F4] uppercase tracking-tight flex items-center space-x-2">
            <ShieldAlert className="w-5 h-5 text-[#FF5A78]" />
            <span>2. Blocked Network Ranges & Metadata Firewalls</span>
          </h3>
          <p>
            To prevent Server-Side Request Forgery and internal infrastructure probing, LaunchCheck automatically blocks and rejects connections to:
          </p>
          <ul className="space-y-1 font-mono text-xs text-[#849188] pl-4">
            <li>• Loopback addresses (<code className="text-[#38FF8E]">127.0.0.0/8</code>, <code className="text-[#38FF8E]">::1</code>)</li>
            <li>• Private IPv4 subnets (<code className="text-[#38FF8E]">10.0.0.0/8</code>, <code className="text-[#38FF8E]">172.16.0.0/12</code>, <code className="text-[#38FF8E]">192.168.0.0/16</code>)</li>
            <li>• Link-local & cloud instance metadata endpoints (<code className="text-[#FF5A78]">169.254.169.254</code>)</li>
            <li>• Multicast and broadcast allocations</li>
          </ul>
        </div>

        <div className="space-y-3 pt-4 border-t border-[#1D2A22]">
          <h3 className="font-mono text-base font-bold text-[#F4F8F4] uppercase tracking-tight flex items-center space-x-2">
            <Lock className="w-5 h-5 text-[#65F8FF]" />
            <span>3. Multi-Tenant Data Isolation</span>
          </h3>
          <p>
            All projects, audit histories, and credential hashes are secured with strict compound key isolation in PostgreSQL. Stateless JWT tokens signed with HMAC-SHA256 authenticate every operator session.
          </p>
        </div>
      </div>
    </div>
  );
}
