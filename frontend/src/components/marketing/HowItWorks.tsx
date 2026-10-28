import React from 'react';
import { Globe, Shield, Terminal, CheckSquare } from 'lucide-react';

export function HowItWorks() {
  const steps = [
    {
      num: '1',
      title: 'Enter your targets.',
      icon: Globe,
      description: 'Provide your frontend web URL and backend API endpoint.'
    },
    {
      num: '2',
      title: 'Resolve destinations safely.',
      icon: Shield,
      description: 'Hostnames resolve upfront with strict SSRF protection.'
    },
    {
      num: '3',
      title: 'Run diagnostic checks.',
      icon: Terminal,
      description: 'Verify TLS, CORS preflight headers, and API smoke tests.'
    },
    {
      num: '4',
      title: 'Review actionable results.',
      icon: CheckSquare,
      description: 'Inspect clear failure telemetry and remediation guidance.'
    }
  ];

  return (
    <section id="how-it-works" className="py-20 border-t border-[#1D2A22] bg-[#0C110E]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="max-w-3xl mb-14">
          <div className="text-xs font-mono uppercase tracking-widest text-[#38FF8E] mb-2">
            // EXECUTION PIPELINE
          </div>
          <h2 className="text-2xl sm:text-4xl font-extrabold tracking-tight text-[#F4F8F4]">
            How LaunchCheck operates.
          </h2>
          <p className="mt-3 text-sm text-[#C0CAC2] leading-relaxed">
            Deterministic, safe, read-only diagnostics executed within bounded timeouts.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {steps.map((step) => {
            const IconComponent = step.icon;
            return (
              <div
                key={step.num}
                className="relative bg-[#070A08] border border-[#25352B] p-6 rounded-[2px] space-y-4 hover:border-[#38FF8E]/40 transition-colors"
              >
                <div className="flex items-center justify-between">
                  <span className="font-mono text-xl font-bold text-[#38FF8E]">
                    0{step.num}
                  </span>
                  <div className="w-8 h-8 rounded-[2px] bg-[#111913] border border-[#25352B] flex items-center justify-center text-[#65F8FF]">
                    <IconComponent className="w-4 h-4" />
                  </div>
                </div>

                <h3 className="font-mono text-sm font-bold uppercase tracking-tight text-[#F4F8F4]">
                  {step.title}
                </h3>

                <p className="text-xs text-[#C0CAC2] leading-relaxed font-sans">
                  {step.description}
                </p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
