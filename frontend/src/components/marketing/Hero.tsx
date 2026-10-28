import React, { useState } from 'react';
import { ArrowRight, Globe, Server, Zap } from 'lucide-react';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import AeroShards from '../ui/AeroShards/AeroShards';
import TechText from '../ui/tech-text/TechText';

interface HeroProps {
  onQuickRun: (frontendUrl: string, backendUrl: string) => void;
  onExploreArchitecture: () => void;
}

export function Hero({ onQuickRun, onExploreArchitecture }: HeroProps) {
  const [frontendUrl, setFrontendUrl] = useState('https://github.com');
  const [backendUrl, setBackendUrl] = useState('https://api.github.com');

  const handleStart = (e: React.FormEvent) => {
    e.preventDefault();
    onQuickRun(frontendUrl, backendUrl);
  };

  return (
    <section className="relative pt-12 pb-20 lg:pt-20 lg:pb-28 overflow-hidden bg-[#070A08]">
      {/* Official AeroShards WebGPU animated background */}
      <div className="absolute inset-0 z-0 overflow-hidden">
        <AeroShards
          backgroundColor="#070A08"
          shardColor="#38FF8E"
          accentColor="#65F8FF"
          material="glass"
          placement="full"
          count={1200}
          className="w-full h-full"
        />
      </div>

      {/* Subtle vignette overlay that does not block pointer events */}
      <div className="absolute inset-0 bg-radial-vignette pointer-events-none z-1" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="text-center max-w-4xl mx-auto space-y-6">
          {/* Semantic HTML h1 for accessibility / screen readers */}
          <h1 className="sr-only">
            Make your next deployment a confident one.
          </h1>

          {/* Official ReactBits TechText animated headline */}
          <div className="flex flex-col items-center justify-center w-full max-w-4xl mx-auto space-y-1 sm:space-y-2 select-none">
            <div className="w-full h-[52px] sm:h-[68px] md:h-[84px] lg:h-[98px] relative">
              <TechText
                text="Make your next deployment"
                color="#F4F7F4"
                accentColor="#38FF8E"
                fontWeight={700}
                fontSize={100}
                align="center"
                className="w-full h-full"
              />
            </div>
            <div className="w-full h-[52px] sm:h-[68px] md:h-[84px] lg:h-[98px] relative">
              <TechText
                text="a confident one."
                color="#F4F7F4"
                accentColor="#38FF8E"
                fontWeight={700}
                fontSize={100}
                align="center"
                className="w-full h-full"
              />
            </div>
          </div>

          {/* Subheading */}
          <p className="text-sm sm:text-base text-[#C0CAC2] max-w-2xl mx-auto leading-relaxed font-sans">
            Check reachability, TLS, CORS, and API health in one place.
          </p>

          {/* Quick Preflight Input Bar */}
          <form
            onSubmit={handleStart}
            className="mt-8 max-w-3xl mx-auto p-2 bg-[#0C110E] border border-[#25352B] rounded-[2px] shadow-[0_0_30px_rgba(0,0,0,0.6)] flex flex-col sm:flex-row gap-2 relative z-20"
          >
            <div className="flex-1 relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#65F8FF]">
                <Globe className="w-4 h-4" />
              </div>
              <input
                type="url"
                required
                value={frontendUrl}
                onChange={(e) => setFrontendUrl(e.target.value)}
                placeholder="https://github.com"
                className="w-full bg-[#070A08] border border-[#1D2A22] rounded-[2px] pl-9 pr-3 py-2.5 text-xs font-mono text-[#F4F8F4] placeholder-[#4D5750] focus:outline-none focus:border-[#38FF8E]"
              />
            </div>

            <div className="flex-1 relative">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-[#38FF8E]">
                <Server className="w-4 h-4" />
              </div>
              <input
                type="url"
                required
                value={backendUrl}
                onChange={(e) => setBackendUrl(e.target.value)}
                placeholder="https://api.github.com"
                className="w-full bg-[#070A08] border border-[#1D2A22] rounded-[2px] pl-9 pr-3 py-2.5 text-xs font-mono text-[#F4F8F4] placeholder-[#4D5750] focus:outline-none focus:border-[#38FF8E]"
              />
            </div>

            <Button
              type="submit"
              variant="primary"
              size="md"
              icon={<Zap className="w-3.5 h-3.5" />}
              className="sm:w-auto"
            >
              Run a preflight
            </Button>
          </form>

          {/* Direct CTA alternative */}
          <div className="pt-2 flex items-center justify-center space-x-4 text-xs font-mono text-[#849188]">
            <span>SSRF-HARDENED PROBES</span>
            <span>•</span>
            <span>IP PINNING</span>
            <span>•</span>
            <button
              type="button"
              onClick={onExploreArchitecture}
              className="text-[#65F8FF] hover:underline flex items-center space-x-1 cursor-pointer bg-transparent border-none p-0"
            >
              <span>Explore the workspace</span>
              <ArrowRight className="w-3 h-3" />
            </button>
          </div>
        </div>

        {/* Live Product Preview Card (Interactive Mock / Telemetry Showcase) */}
        <div className="mt-14 max-w-4xl mx-auto border border-[#25352B] bg-[#0C110E] rounded-[2px] shadow-2xl overflow-hidden relative">
          {/* Top chrome bar */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-[#1D2A22] bg-[#070A08]">
            <div className="flex items-center space-x-2">
              <span className="w-2.5 h-2.5 rounded-full bg-[#FF5A78]/80" />
              <span className="w-2.5 h-2.5 rounded-full bg-[#FFE282]/80" />
              <span className="w-2.5 h-2.5 rounded-full bg-[#38FF8E]/80" />
              <span className="ml-2 text-xs font-mono text-[#849188]">launchcheck://preflight-telemetry</span>
            </div>
            <div className="flex items-center space-x-2">
              <span className="text-[10px] font-mono text-[#849188]">SAMPLE PREFLIGHT RUN</span>
              <Badge status="PASS" label="SYSTEM ALL-GREEN" />
            </div>
          </div>

          {/* Telemetry body preview */}
          <div className="p-6 grid grid-cols-1 md:grid-cols-3 gap-4 font-mono text-xs">
            {/* Target 1 */}
            <div className="bg-[#070A08] border border-[#1D2A22] p-4 rounded-[2px] space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] text-[#65F8FF] font-bold">FRONTEND ROUTE</span>
                <Badge status="PASS" size="sm" />
              </div>
              <div className="text-sm font-bold text-[#F4F8F4] truncate">github.com</div>
              <div className="text-[#849188] text-[11px] space-y-1 pt-1">
                <div>HTTP Status: <span className="text-[#38FF8E]">200 OK</span></div>
                <div>Latency: <span className="text-[#F4F8F4]">42 ms</span></div>
                <div>TLS: <span className="text-[#38FF8E]">Valid (Expires 2027)</span></div>
              </div>
            </div>

            {/* Target 2 */}
            <div className="bg-[#070A08] border border-[#1D2A22] p-4 rounded-[2px] space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] text-[#38FF8E] font-bold">BACKEND API</span>
                <Badge status="PASS" size="sm" />
              </div>
              <div className="text-sm font-bold text-[#F4F8F4] truncate">api.github.com</div>
              <div className="text-[#849188] text-[11px] space-y-1 pt-1">
                <div>HTTP Status: <span className="text-[#38FF8E]">200 OK</span></div>
                <div>Latency: <span className="text-[#F4F8F4]">88 ms</span></div>
                <div>CORS Preflight: <span className="text-[#38FF8E]">Origin Permitted</span></div>
              </div>
            </div>

            {/* Target 3: Smoke Test */}
            <div className="bg-[#070A08] border border-[#1D2A22] p-4 rounded-[2px] space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] text-[#FF65D8] font-bold">API SMOKE TESTS</span>
                <Badge status="PASS" label="2/2 CHECKS" size="sm" />
              </div>
              <div className="text-sm font-bold text-[#F4F8F4]">GET /repos/octocat/Spoon-Knife</div>
              <div className="text-[#849188] text-[11px] space-y-1 pt-1">
                <div>Header Check: <span className="text-[#38FF8E]">application/json</span></div>
                <div>JSON /full_name: <span className="text-[#38FF8E]">"octocat/Spoon-Knife"</span></div>
                <div>Failover: <span className="text-[#65F8FF]">0 retries needed</span></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
