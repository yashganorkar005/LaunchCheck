import React from 'react';
import { Github, Terminal, ArrowUpRight } from 'lucide-react';

interface FooterProps {
  onNavigateSection: (sectionId: string) => void;
}

export function Footer({ onNavigateSection }: FooterProps) {
  return (
    <footer className="bg-[#070A08] border-t border-[#1D2A22] text-[#849188] font-mono text-xs py-16">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10 pb-12 border-b border-[#1D2A22]">
          {/* Brand Col */}
          <div className="space-y-4 md:col-span-1">
            <div className="flex items-center space-x-3">
              <img src="/assets/launchcheklogo.png" alt="LaunchCheck logo" className="w-7 h-7 object-contain" />
              <span className="font-bold tracking-wider text-[#F4F8F4]">LAUNCHCHECK</span>
            </div>
            <p className="text-[11px] text-[#C0CAC2] leading-relaxed font-sans">
              Production deployment preflight engine with SSRF-hardened IP pinning, TLS telemetry, and JSON Pointer assertions.
            </p>
          </div>

          {/* Nav Col 1 */}
          <div className="space-y-3">
            <h4 className="font-bold text-[#F4F8F4] uppercase tracking-wider text-[11px]">Capabilities</h4>
            <ul className="space-y-2 text-[11px]">
              <li>
                <button onClick={() => onNavigateSection('features')} className="hover:text-[#38FF8E] transition-colors cursor-pointer">
                  SSRF-Safe Transport
                </button>
              </li>
              <li>
                <button onClick={() => onNavigateSection('features')} className="hover:text-[#38FF8E] transition-colors cursor-pointer">
                  TLS Certificate Checks
                </button>
              </li>
              <li>
                <button onClick={() => onNavigateSection('features')} className="hover:text-[#38FF8E] transition-colors cursor-pointer">
                  CORS Origin Simulation
                </button>
              </li>
              <li>
                <button onClick={() => onNavigateSection('features')} className="hover:text-[#38FF8E] transition-colors cursor-pointer">
                  API Smoke Tests
                </button>
              </li>
            </ul>
          </div>

          {/* Nav Col 2 */}
          <div className="space-y-3">
            <h4 className="font-bold text-[#F4F8F4] uppercase tracking-wider text-[11px]">Architecture</h4>
            <ul className="space-y-2 text-[11px]">
              <li>
                <button onClick={() => onNavigateSection('security')} className="hover:text-[#38FF8E] transition-colors cursor-pointer">
                  IP Pinning & Rebinding
                </button>
              </li>
              <li>
                <button onClick={() => onNavigateSection('how-it-works')} className="hover:text-[#38FF8E] transition-colors cursor-pointer">
                  Execution Pipeline
                </button>
              </li>
              <li>
                <button onClick={() => onNavigateSection('faq')} className="hover:text-[#38FF8E] transition-colors cursor-pointer">
                  Technical FAQ
                </button>
              </li>
            </ul>
          </div>

          {/* Nav Col 3 */}
          <div className="space-y-3">
            <h4 className="font-bold text-[#F4F8F4] uppercase tracking-wider text-[11px]">References</h4>
            <ul className="space-y-2 text-[11px]">
              <li>
                <a
                  href="https://github.com"
                  target="_blank"
                  rel="noreferrer"
                  className="hover:text-[#38FF8E] transition-colors flex items-center space-x-1"
                >
                  <span>GitHub Repository</span>
                  <ArrowUpRight className="w-3 h-3" />
                </a>
              </li>
              <li>
                <span className="text-[#4D5750]">RFC 6901 JSON Pointers</span>
              </li>
              <li>
                <span className="text-[#4D5750]">Spring Boot 4.1 Enterprise</span>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="pt-8 flex flex-col sm:flex-row items-center justify-between text-[11px] text-[#849188] space-y-4 sm:space-y-0">
          <div>
            © {new Date().getFullYear()} LaunchCheck Systems Inc. All rights reserved.
          </div>
          <div className="flex items-center space-x-6">
            <span>Terminal-Luxe Design System</span>
            <span>•</span>
            <span>Zero Synthetic Hype</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
