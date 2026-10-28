import React, { useState } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';

interface FaqItem {
  question: string;
  answer: string;
}

export function FaqSection() {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const faqs: FaqItem[] = [
    {
      question: 'What is SSRF-hardened probe transport?',
      answer:
        'LaunchCheck resolves target hostnames upfront, validates IP addresses against public unicast ranges, and pins connections to prevent SSRF and DNS rebinding.'
    },
    {
      question: 'How do JSON Pointer assertions work?',
      answer:
        'LaunchCheck parses response bodies into ASTs and evaluates RFC 6901 JSON Pointers for deterministic structural validation.'
    },
    {
      question: 'Can I run preflight checks without creating an account?',
      answer:
        'Yes. The public landing page includes a quick-run preflight input bar for transient audits immediately.'
    },
    {
      question: 'How does LaunchCheck verify CORS headers?',
      answer:
        'LaunchCheck simulates cross-origin requests with custom Origin headers and inspects allow-origin and allow-methods.'
    },
    {
      question: 'Are my project endpoints stored securely?',
      answer:
        'Yes. Project configurations are stored in a multi-tenant database with strict user isolation and hashed credentials.'
    }
  ];

  return (
    <section id="faq" className="py-24 bg-[#0C110E] border-t border-[#1D2A22]">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-2xl mx-auto mb-16">
          <div className="text-xs font-mono uppercase tracking-widest text-[#38FF8E] mb-2">
            // FREQUENTLY ASKED QUESTIONS
          </div>
          <h2 className="text-2xl sm:text-4xl font-extrabold tracking-tight text-[#F4F8F4]">
            Frequently asked questions.
          </h2>
          <p className="mt-3 text-sm text-[#C0CAC2] leading-relaxed">
            Technical answers regarding preflight execution and security architecture.
          </p>
        </div>

        <div className="space-y-4">
          {faqs.map((faq, idx) => {
            const isOpen = openIndex === idx;
            return (
              <div
                key={idx}
                className="bg-[#070A08] border border-[#25352B] rounded-[2px] overflow-hidden transition-colors"
              >
                <button
                  type="button"
                  onClick={() => setOpenIndex(isOpen ? null : idx)}
                  className="w-full px-6 py-4 text-left flex items-center justify-between font-mono text-sm font-bold text-[#F4F8F4] hover:text-[#38FF8E] cursor-pointer"
                >
                  <span>{faq.question}</span>
                  {isOpen ? (
                    <ChevronUp className="w-4 h-4 text-[#38FF8E] flex-shrink-0 ml-4" />
                  ) : (
                    <ChevronDown className="w-4 h-4 text-[#849188] flex-shrink-0 ml-4" />
                  )}
                </button>
                {isOpen && (
                  <div className="px-6 pb-5 text-xs text-[#C0CAC2] leading-relaxed font-sans border-t border-[#1D2A22] pt-4">
                    {faq.answer}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
