import React, { useState } from 'react';
import { ArrowRight, LogIn, Menu, X, LayoutDashboard } from 'lucide-react';
import { Button } from '../ui/Button';

interface MarketingNavProps {
  onOpenWorkspace: () => void;
  onOpenAuth: () => void;
  isAuthenticated: boolean;
  userEmail: string | null;
  onScrollTo: (sectionId: string) => void;
}

export function MarketingNav({
  onOpenWorkspace,
  onOpenAuth,
  isAuthenticated,
  userEmail,
  onScrollTo
}: MarketingNavProps) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleNavClick = (sectionId: string) => {
    onScrollTo(sectionId);
    setMobileMenuOpen(false);
  };

  return (
    <header className="sticky top-0 z-40 w-full border-b border-[#1D2A22] bg-[#070A08]/85 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand identity */}
        <div className="flex items-center space-x-3 cursor-pointer" onClick={() => handleNavClick('top')}>
          <img src="/assets/launchcheklogo.png" alt="LaunchCheck logo" className="w-8 h-8 object-contain" />
          <span className="font-mono font-bold text-sm tracking-wider uppercase text-[#F4F8F4]">
            LAUNCHCHECK
          </span>
        </div>

        {/* Desktop Navigation Links */}
        <nav className="hidden md:flex items-center space-x-7 text-xs font-mono text-[#C0CAC2]">
          <button
            onClick={() => handleNavClick('features')}
            className="hover:text-[#38FF8E] transition-colors cursor-pointer"
          >
            Capabilities
          </button>
          <button
            onClick={() => handleNavClick('how-it-works')}
            className="hover:text-[#38FF8E] transition-colors cursor-pointer"
          >
            How It Works
          </button>
          <button
            onClick={() => handleNavClick('security')}
            className="hover:text-[#38FF8E] transition-colors cursor-pointer"
          >
            Security Defense
          </button>
          <button
            onClick={() => handleNavClick('faq')}
            className="hover:text-[#38FF8E] transition-colors cursor-pointer"
          >
            FAQ
          </button>
        </nav>

        {/* Action Controls */}
        <div className="hidden md:flex items-center space-x-3">
          {isAuthenticated ? (
            <div className="flex items-center space-x-3">
              <span className="text-xs font-mono text-[#849188] max-w-[140px] truncate">
                {userEmail}
              </span>
              <Button
                variant="primary"
                size="sm"
                onClick={onOpenWorkspace}
                icon={<LayoutDashboard className="w-3.5 h-3.5" />}
              >
                Workspace
              </Button>
            </div>
          ) : (
            <>
              <Button
                variant="ghost"
                size="sm"
                onClick={onOpenAuth}
                icon={<LogIn className="w-3.5 h-3.5" />}
              >
                Sign In
              </Button>
              <Button
                variant="primary"
                size="sm"
                onClick={onOpenWorkspace}
                icon={<ArrowRight className="w-3.5 h-3.5" />}
              >
                Run Preflight
              </Button>
            </>
          )}
        </div>

        {/* Mobile menu trigger */}
        <div className="md:hidden flex items-center">
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="p-1.5 text-[#C0CAC2] hover:text-[#F4F8F4] focus:outline-none focus:ring-1 focus:ring-[#38FF8E]"
            aria-label="Toggle navigation menu"
          >
            {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* Mobile drawer */}
      {mobileMenuOpen && (
        <div className="md:hidden border-b border-[#1D2A22] bg-[#0C110E] px-4 pt-3 pb-6 space-y-4">
          <div className="flex flex-col space-y-3 font-mono text-xs text-[#C0CAC2]">
            <button
              onClick={() => handleNavClick('features')}
              className="text-left py-1 hover:text-[#38FF8E]"
            >
              Capabilities
            </button>
            <button
              onClick={() => handleNavClick('how-it-works')}
              className="text-left py-1 hover:text-[#38FF8E]"
            >
              How It Works
            </button>
            <button
              onClick={() => handleNavClick('security')}
              className="text-left py-1 hover:text-[#38FF8E]"
            >
              Security Defense
            </button>
            <button
              onClick={() => handleNavClick('faq')}
              className="text-left py-1 hover:text-[#38FF8E]"
            >
              FAQ
            </button>
          </div>
          <div className="pt-3 border-t border-[#1D2A22] flex flex-col space-y-2">
            {!isAuthenticated ? (
              <>
                <Button
                  variant="outline"
                  size="md"
                  onClick={() => { setMobileMenuOpen(false); onOpenAuth(); }}
                  icon={<LogIn className="w-3.5 h-3.5" />}
                  className="w-full"
                >
                  Sign In
                </Button>
                <Button
                  variant="primary"
                  size="md"
                  onClick={() => { setMobileMenuOpen(false); onOpenWorkspace(); }}
                  icon={<ArrowRight className="w-3.5 h-3.5" />}
                  className="w-full"
                >
                  Run Preflight
                </Button>
              </>
            ) : (
              <Button
                variant="primary"
                size="md"
                onClick={() => { setMobileMenuOpen(false); onOpenWorkspace(); }}
                icon={<LayoutDashboard className="w-3.5 h-3.5" />}
                className="w-full"
              >
                Go to Workspace
              </Button>
            )}
          </div>
        </div>
      )}
    </header>
  );
}
