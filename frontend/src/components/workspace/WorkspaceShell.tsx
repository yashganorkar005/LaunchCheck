import React, { useState } from 'react';
import {
  LayoutDashboard,
  Radio,
  Cpu,
  FolderGit2,
  History,
  Lock,
  FileText,
  LogOut,
  LogIn,
  ChevronDown,
  ArrowLeft,
  Plus
} from 'lucide-react';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { Project } from '../../types';

interface WorkspaceShellProps {
  activeTab: 'audit' | 'cors' | 'smoke' | 'projects' | 'history' | 'security' | 'docs';
  onTabChange: (tab: 'audit' | 'cors' | 'smoke' | 'projects' | 'history' | 'security' | 'docs') => void;
  onReturnToMarketing: () => void;
  userEmail: string | null;
  onLogout: () => void;
  onOpenAuth: () => void;
  projects: Project[];
  selectedProject: Project | null;
  onSelectProject: (proj: Project | null) => void;
  onCreateProjectClick: () => void;
  children: React.ReactNode;
}

export function WorkspaceShell({
  activeTab,
  onTabChange,
  onReturnToMarketing,
  userEmail,
  onLogout,
  onOpenAuth,
  projects,
  selectedProject,
  onSelectProject,
  onCreateProjectClick,
  children
}: WorkspaceShellProps) {
  const [projectDropdownOpen, setProjectDropdownOpen] = useState(false);

  const navItems = [
    { id: 'audit', label: 'Preflight Audit', icon: LayoutDashboard },
    { id: 'cors', label: 'CORS Diagnostics', icon: Radio },
    { id: 'smoke', label: 'API Smoke Tests', icon: Cpu },
    { id: 'projects', label: 'Projects', icon: FolderGit2 },
    { id: 'history', label: 'Audit History', icon: History },
    { id: 'security', label: 'Security Architecture', icon: Lock },
    { id: 'docs', label: 'Documentation', icon: FileText }
  ];

  return (
    <div className="min-h-screen bg-[#070A08] text-[#F4F8F4] flex flex-col font-sans">
      {/* Top Workspace Header */}
      <header className="h-16 border-b border-[#1D2A22] bg-[#0C110E] px-4 sm:px-6 flex items-center justify-between z-30 sticky top-0">
        <div className="flex items-center space-x-4">
          <button
            onClick={onReturnToMarketing}
            className="flex items-center space-x-1.5 text-xs font-mono text-[#849188] hover:text-[#38FF8E] transition-colors cursor-pointer"
            title="Return to Marketing Landing Page"
          >
            <ArrowLeft className="w-4 h-4" />
            <span className="hidden sm:inline">Landing</span>
          </button>
          <div className="h-4 w-px bg-[#25352B]" />
          <div className="flex items-center space-x-2.5">
            <img src="/assets/launchcheklogo.png" alt="LaunchCheck logo" className="w-7 h-7 object-contain" />
            <span className="font-mono font-bold text-sm tracking-wider uppercase">
              LAUNCHCHECK <span className="text-[#38FF8E]">STUDIO</span>
            </span>
          </div>

          {/* Project Selector Dropdown */}
          <div className="relative hidden lg:block ml-4">
            <button
              onClick={() => setProjectDropdownOpen(!projectDropdownOpen)}
              className="flex items-center space-x-2 px-3 py-1.5 bg-[#070A08] border border-[#25352B] rounded-[2px] text-xs font-mono hover:border-[#38FF8E]/50 transition-colors cursor-pointer"
            >
              <span className="text-[#849188]">PROJECT:</span>
              <span className="text-[#F4F8F4] font-bold">
                {selectedProject ? selectedProject.name : 'All Projects (Transient)'}
              </span>
              <ChevronDown className="w-3.5 h-3.5 text-[#849188]" />
            </button>

            {projectDropdownOpen && (
              <div className="absolute top-full left-0 mt-1 w-64 bg-[#0C110E] border border-[#25352B] rounded-[2px] shadow-2xl z-50 py-1">
                <button
                  onClick={() => { onSelectProject(null); setProjectDropdownOpen(false); }}
                  className={`w-full px-3 py-2 text-left text-xs font-mono hover:bg-[#111913] transition-colors ${
                    !selectedProject ? 'text-[#38FF8E] font-bold bg-[#111913]' : 'text-[#C0CAC2]'
                  }`}
                >
                  [Transient Audit — No Project]
                </button>
                {projects.map((proj) => (
                  <button
                    key={proj.id}
                    onClick={() => { onSelectProject(proj); setProjectDropdownOpen(false); }}
                    className={`w-full px-3 py-2 text-left text-xs font-mono hover:bg-[#111913] transition-colors truncate ${
                      selectedProject?.id === proj.id ? 'text-[#38FF8E] font-bold bg-[#111913]' : 'text-[#C0CAC2]'
                    }`}
                  >
                    {proj.name}
                  </button>
                ))}
                <div className="border-t border-[#1D2A22] mt-1 pt-1 px-1">
                  <button
                    onClick={() => { setProjectDropdownOpen(false); onCreateProjectClick(); }}
                    className="w-full flex items-center space-x-1 px-2 py-1.5 text-xs font-mono text-[#65F8FF] hover:bg-[#65F8FF]/10 rounded-[2px]"
                  >
                    <Plus className="w-3 h-3" />
                    <span>New Project</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right side user info & logout */}
        <div className="flex items-center space-x-3 sm:space-x-4">
          {userEmail ? (
            <>
              <Badge status="PASS" label="OPERATOR SESSION" size="sm" />
              <span className="hidden sm:inline text-xs font-mono text-[#849188]">
                {userEmail}
              </span>
              <Button
                variant="ghost"
                size="sm"
                onClick={onLogout}
                icon={<LogOut className="w-3.5 h-3.5" />}
                title="Sign Out"
              >
                Sign Out
              </Button>
            </>
          ) : (
            <>
              <Badge status="WARN" label="GUEST MODE" size="sm" />
              <span className="hidden sm:inline text-xs font-mono text-[#849188]">
                Unauthenticated
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={onOpenAuth}
                icon={<LogIn className="w-3.5 h-3.5" />}
                title="Sign In"
              >
                Sign In
              </Button>
            </>
          )}
        </div>
      </header>

      {/* Secondary Sub-nav Bar */}
      <div className="bg-[#070A08] border-b border-[#1D2A22] px-4 sm:px-6 overflow-x-auto">
        <div className="flex space-x-2 py-2">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => onTabChange(item.id as any)}
                className={`flex items-center space-x-2 px-3.5 py-2 rounded-[2px] text-xs font-mono transition-colors whitespace-nowrap cursor-pointer ${
                  isActive
                    ? 'bg-[#38FF8E]/10 text-[#38FF8E] border border-[#38FF8E]/40 font-bold'
                    : 'text-[#849188] hover:text-[#F4F8F4] hover:bg-[#0C110E] border border-transparent'
                }`}
              >
                <Icon className="w-3.5 h-3.5" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Main Workspace Content */}
      <main className="flex-1 p-4 sm:p-8 max-w-7xl mx-auto w-full">
        {children}
      </main>
    </div>
  );
}
