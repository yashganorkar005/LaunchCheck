import React, { useState, useEffect, useCallback } from 'react';
import { MarketingNav } from './components/marketing/MarketingNav';
import { Hero } from './components/marketing/Hero';
import { BentoGrid } from './components/marketing/BentoGrid';
import { HowItWorks } from './components/marketing/HowItWorks';
import { SecurityTrust } from './components/marketing/SecurityTrust';
import { FaqSection } from './components/marketing/FaqSection';
import { Footer } from './components/marketing/Footer';
import { AuthModal } from './components/auth/AuthModal';

import { WorkspaceShell } from './components/workspace/WorkspaceShell';
import { AuditStudio } from './components/workspace/AuditStudio';
import { ResultsView } from './components/workspace/ResultsView';
import { CorsStudio } from './components/workspace/CorsStudio';
import { SmokeTestStudio } from './components/workspace/SmokeTestStudio';
import { ProjectsStudio } from './components/workspace/ProjectsStudio';
import { HistoryStudio } from './components/workspace/HistoryStudio';
import { SecurityDocView } from './components/workspace/SecurityDocView';
import { DocumentationView } from './components/workspace/DocumentationView';
import { ToastProvider, useToast } from './components/ui/Toast';

import { Project, AuditResponse, AuthResponse } from './types';
import { api } from './lib/api';

function LaunchCheckWorkspace() {
  const [viewMode, setViewMode] = useState<'marketing' | 'workspace'>('marketing');
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('lc_token'));
  const [userEmail, setUserEmail] = useState<string | null>(() => localStorage.getItem('lc_email'));
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const { toast } = useToast();

  // Workspace tab & data state
  const [activeTab, setActiveTab] = useState<'audit' | 'cors' | 'smoke' | 'projects' | 'history' | 'security' | 'docs'>('audit');
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);
  const [currentAuditResult, setCurrentAuditResult] = useState<AuditResponse | null>(null);

  // Hero quick-run transient targets
  const [transientFrontend, setTransientFrontend] = useState('https://jsonplaceholder.typicode.com');
  const [transientBackend, setTransientBackend] = useState('https://jsonplaceholder.typicode.com');

  const loadProjects = useCallback(async () => {
    if (!token) return;
    try {
      const data = await api.projects.list(token);
      setProjects(data);
    } catch (err) {
      console.error('Failed to load projects', err);
    }
  }, [token]);

  useEffect(() => {
    if (token) {
      api.auth.me(token)
        .then((user) => {
          if (user && user.email) {
            setUserEmail(user.email);
            localStorage.setItem('lc_email', user.email);
          }
          loadProjects();
        })
        .catch((err: any) => {
          if (err && err.status === 401) {
            localStorage.removeItem('lc_token');
            localStorage.removeItem('lc_email');
            setToken(null);
            setUserEmail(null);
            toast.warn('Session expired', 'Please sign in again.');
          } else {
            // Temporary network/server failure: preserve session
            loadProjects();
          }
        });
    }
  }, [token, loadProjects]);

  const handleAuthSuccess = (data: AuthResponse) => {
    setToken(data.token);
    setUserEmail(data.user.email);
    setAuthModalOpen(false);
    toast.success('Operator session established', `Authenticated as ${data.user.email}`);
    loadProjects();
  };

  const handleLogout = () => {
    localStorage.removeItem('lc_token');
    localStorage.removeItem('lc_email');
    setToken(null);
    setUserEmail(null);
    setProjects([]);
    setSelectedProject(null);
    toast.info('Operator session terminated', 'Signed out of LaunchCheck Studio.');
  };

  const handleScrollToSection = (sectionId: string) => {
    if (viewMode !== 'marketing') {
      setViewMode('marketing');
      setTimeout(() => {
        document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    } else {
      document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleQuickRun = (frontendUrl: string, backendUrl: string) => {
    setTransientFrontend(frontendUrl);
    setTransientBackend(backendUrl);
    setViewMode('workspace');
    setActiveTab('audit');
  };

  return (
    <div className="min-h-screen bg-[#070A08] text-[#F4F8F4] selection:bg-[#38FF8E] selection:text-[#070A08]">
      {viewMode === 'marketing' ? (
        <>
          <MarketingNav
            onOpenWorkspace={() => setViewMode('workspace')}
            onOpenAuth={() => setAuthModalOpen(true)}
            isAuthenticated={!!token}
            userEmail={userEmail}
            onScrollTo={handleScrollToSection}
          />
          <Hero
            onQuickRun={handleQuickRun}
            onExploreArchitecture={() => handleScrollToSection('security')}
          />
          <BentoGrid />
          <HowItWorks />
          <SecurityTrust />
          <FaqSection />
          <Footer onNavigateSection={handleScrollToSection} />
        </>
      ) : (
        <WorkspaceShell
          activeTab={activeTab}
          onTabChange={setActiveTab}
          onReturnToMarketing={() => setViewMode('marketing')}
          userEmail={userEmail}
          onLogout={handleLogout}
          onOpenAuth={() => setAuthModalOpen(true)}
          projects={projects}
          selectedProject={selectedProject}
          onSelectProject={setSelectedProject}
          onCreateProjectClick={() => setActiveTab('projects')}
        >
          {activeTab === 'audit' && (
            currentAuditResult ? (
              <ResultsView
                auditResult={currentAuditResult}
                onBackToStudio={() => setCurrentAuditResult(null)}
              />
            ) : (
              <AuditStudio
                selectedProject={selectedProject}
                onAuditComplete={(res) => setCurrentAuditResult(res)}
                initialFrontend={transientFrontend}
                initialBackend={transientBackend}
              />
            )
          )}

          {activeTab === 'cors' && <CorsStudio />}
          {activeTab === 'smoke' && <SmokeTestStudio />}
          {activeTab === 'projects' && (
            <ProjectsStudio
              projects={projects}
              onRefreshProjects={loadProjects}
              onSelectProject={(proj) => {
                setSelectedProject(proj);
                setActiveTab('audit');
              }}
            />
          )}
          {activeTab === 'history' && (
            <HistoryStudio
              token={token}
              onOpenAuth={() => setAuthModalOpen(true)}
              onSelectAudit={(audit) => {
                setCurrentAuditResult(audit);
                setActiveTab('audit');
              }}
            />
          )}
          {activeTab === 'security' && <SecurityDocView />}
          {activeTab === 'docs' && <DocumentationView onNavigateTab={setActiveTab} />}
        </WorkspaceShell>
      )}

      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        onSuccess={handleAuthSuccess}
      />
    </div>
  );
}

export function App() {
  return (
    <ToastProvider>
      <LaunchCheckWorkspace />
    </ToastProvider>
  );
}

export default App;
