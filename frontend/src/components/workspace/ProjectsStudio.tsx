import React, { useState } from 'react';
import { FolderGit2, Plus, Globe, Server, Trash2, ArrowRight, Pencil } from 'lucide-react';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Modal } from '../ui/Modal';
import { Badge } from '../ui/Badge';
import { useToast } from '../ui/Toast';
import { Project } from '../../types';
import { api, ApiError } from '../../lib/api';

interface ProjectsStudioProps {
  projects: Project[];
  onRefreshProjects: () => void;
  onSelectProject: (proj: Project) => void;
}

export function ProjectsStudio({ projects, onRefreshProjects, onSelectProject }: ProjectsStudioProps) {
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);

  const [name, setName] = useState('');
  const [frontendUrl, setFrontendUrl] = useState('');
  const [backendUrl, setBackendUrl] = useState('');

  const [editName, setEditName] = useState('');
  const [editFrontendUrl, setEditFrontendUrl] = useState('');
  const [editBackendUrl, setEditBackendUrl] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { toast } = useToast();

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('lc_token');
      if (!token) throw new ApiError('Authentication required', 401);
      const created = await api.projects.create({ name, frontendUrl, backendUrl }, token);
      toast.success('Project initialized', `Deployment project "${created.name}" created successfully.`);
      setName('');
      setFrontendUrl('');
      setBackendUrl('');
      setCreateModalOpen(false);
      onRefreshProjects();
    } catch (err: any) {
      const message = err instanceof ApiError ? err.message : (err.message || 'Failed to create project.');
      setError(message);
      toast.error('Project creation failed', message);
    } finally {
      setLoading(false);
    }
  };

  const openEditModal = (proj: Project) => {
    setEditingProject(proj);
    setEditName(proj.name);
    setEditFrontendUrl(proj.frontendUrl);
    setEditBackendUrl(proj.backendUrl);
    setEditModalOpen(true);
    setError(null);
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingProject) return;
    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('lc_token');
      if (!token) throw new ApiError('Authentication required', 401);
      const updated = await api.projects.update(editingProject.id, {
        name: editName,
        frontendUrl: editFrontendUrl,
        backendUrl: editBackendUrl
      }, token);
      toast.success('Project updated', `Deployment project "${updated.name}" updated successfully.`);
      setEditModalOpen(false);
      setEditingProject(null);
      onRefreshProjects();
    } catch (err: any) {
      const message = err instanceof ApiError ? err.message : (err.message || 'Failed to update project.');
      setError(message);
      toast.error('Project update failed', message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this project?')) return;
    try {
      const token = localStorage.getItem('lc_token');
      if (!token) throw new ApiError('Authentication required', 401);
      await api.projects.delete(id, token);
      toast.info('Project deleted', 'Isolated deployment profile has been removed.');
      onRefreshProjects();
    } catch (err: any) {
      const message = err.message || 'Failed to delete project.';
      toast.error('Project deletion failed', message);
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <div className="text-xs font-mono uppercase tracking-widest text-[#38FF8E] mb-1">
            // PROJECT ISOLATION GATEWAY
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[#F4F8F4]">
            Managed Deployment Projects
          </h1>
          <p className="text-xs sm:text-sm text-[#C0CAC2] mt-1">
            Group frontend and backend targets under multi-tenant compound key isolation.
          </p>
        </div>
        <Button
          variant="primary"
          size="md"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => setCreateModalOpen(true)}
        >
          New Project
        </Button>
      </div>

      {projects.length === 0 ? (
        <div className="bg-[#0C110E] border border-[#25352B] p-12 text-center rounded-[2px] space-y-4">
          <FolderGit2 className="w-10 h-10 text-[#849188] mx-auto" />
          <div className="font-mono text-sm text-[#F4F8F4]">No Projects Configured Yet</div>
          <p className="text-xs text-[#C0CAC2] max-w-md mx-auto">
            Create your first deployment project to store target endpoints and track historical preflight audits.
          </p>
          <Button
            variant="primary"
            size="sm"
            onClick={() => setCreateModalOpen(true)}
            icon={<Plus className="w-3.5 h-3.5" />}
          >
            Create First Project
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {projects.map((proj) => (
            <div
              key={proj.id}
              className="bg-[#0C110E] border border-[#25352B] p-6 rounded-[2px] space-y-4 flex flex-col justify-between hover:border-[#38FF8E]/40 transition-colors"
            >
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-[10px] text-[#38FF8E] uppercase tracking-wider">// MANAGED TARGET</span>
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => openEditModal(proj)}
                      className="text-[#849188] hover:text-[#38FF8E] transition-colors cursor-pointer"
                      title="Edit Project"
                    >
                      <Pencil className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(proj.id)}
                      className="text-[#849188] hover:text-[#FF5A78] transition-colors cursor-pointer"
                      title="Delete Project"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
                <h3 className="font-mono text-base font-bold text-[#F4F8F4]">{proj.name}</h3>

                <div className="space-y-1.5 font-mono text-[11px] text-[#849188] pt-2 border-t border-[#1D2A22]">
                  <div className="flex items-center space-x-1.5 truncate">
                    <Globe className="w-3 h-3 text-[#65F8FF] flex-shrink-0" />
                    <span className="truncate">{proj.frontendUrl}</span>
                  </div>
                  <div className="flex items-center space-x-1.5 truncate">
                    <Server className="w-3 h-3 text-[#38FF8E] flex-shrink-0" />
                    <span className="truncate">{proj.backendUrl}</span>
                  </div>
                </div>
              </div>

              <div className="pt-4 border-t border-[#1D2A22] flex items-center justify-between">
                <span className="text-[10px] font-mono text-[#849188]">
                  {new Date(proj.createdAt).toLocaleDateString()}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => onSelectProject(proj)}
                  icon={<ArrowRight className="w-3.5 h-3.5" />}
                >
                  Select & Audit
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Project Modal */}
      <Modal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        title="Create Deployment Project"
        eyebrow="MULTI-TENANT GATEWAY"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Project Name"
            type="text"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Production API Gateway"
            monospace
          />
          <Input
            label="Frontend Target URL"
            type="url"
            required
            value={frontendUrl}
            onChange={(e) => setFrontendUrl(e.target.value)}
            placeholder="https://app.example.com"
            icon={<Globe className="w-4 h-4" />}
            monospace
          />
          <Input
            label="Backend Target URL"
            type="url"
            required
            value={backendUrl}
            onChange={(e) => setBackendUrl(e.target.value)}
            placeholder="https://api.example.com"
            icon={<Server className="w-4 h-4" />}
            monospace
          />

          {error && (
            <div className="p-3 bg-[#FF5A78]/10 border border-[#FF5A78]/40 text-[#FF5A78] text-xs font-mono rounded-[2px]">
              {error}
            </div>
          )}

          <div className="pt-3">
            <Button
              type="submit"
              variant="primary"
              size="md"
              isLoading={loading}
              icon={<Plus className="w-3.5 h-3.5" />}
              className="w-full"
            >
              Create Project
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit Project Modal */}
      <Modal
        isOpen={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        title="Edit Deployment Project"
        eyebrow="MULTI-TENANT GATEWAY"
      >
        <form onSubmit={handleUpdate} className="space-y-4">
          <Input
            label="Project Name"
            type="text"
            required
            value={editName}
            onChange={(e) => setEditName(e.target.value)}
            placeholder="Production API Gateway"
            monospace
          />
          <Input
            label="Frontend Target URL"
            type="url"
            required
            value={editFrontendUrl}
            onChange={(e) => setEditFrontendUrl(e.target.value)}
            placeholder="https://app.example.com"
            icon={<Globe className="w-4 h-4" />}
            monospace
          />
          <Input
            label="Backend Target URL"
            type="url"
            required
            value={editBackendUrl}
            onChange={(e) => setEditBackendUrl(e.target.value)}
            placeholder="https://api.example.com"
            icon={<Server className="w-4 h-4" />}
            monospace
          />

          {error && (
            <div className="p-3 bg-[#FF5A78]/10 border border-[#FF5A78]/40 text-[#FF5A78] text-xs font-mono rounded-[2px]">
              {error}
            </div>
          )}

          <div className="pt-3">
            <Button
              type="submit"
              variant="primary"
              size="md"
              isLoading={loading}
              icon={<Pencil className="w-3.5 h-3.5" />}
              className="w-full"
            >
              Save Project Changes
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
