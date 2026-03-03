import { useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getAllProjects } from '@/api/client';
import type { ProjectAuth } from '@/api/client';

interface ProjectSwitcherDropdownProps {
  currentSlug: string | undefined;
  onClose: () => void;
}

interface ProjectEntry {
  slug: string;
  name: string;
  isAdmin: boolean;
}

function getProjectEntries(): ProjectEntry[] {
  const projects = getAllProjects();
  return Object.entries(projects)
    .map(([slug, auth]: [string, ProjectAuth]) => ({
      slug,
      name: auth.projectName ?? slug,
      isAdmin: Boolean(auth.adminCode),
    }))
    .sort((a, b) => a.name.localeCompare(b.name));
}

export function ProjectSwitcherDropdown({ currentSlug, onClose }: ProjectSwitcherDropdownProps) {
  const navigate = useNavigate();
  const dropdownRef = useRef<HTMLDivElement>(null);
  const entries = getProjectEntries();

  // Close on click-outside
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        onClose();
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [onClose]);

  // Close on Escape
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  return (
    <div
      ref={dropdownRef}
      className="absolute left-0 top-full mt-1 glass-crystal rounded-lg shadow-xl w-[85vw] sm:w-auto sm:min-w-56 max-w-sm z-50 overflow-hidden"
    >
      <div className="px-3 py-2 border-b border-white/8">
        <p className="text-xs font-semibold text-efi-text-secondary uppercase tracking-wider">My Projects</p>
      </div>

      {entries.length > 0 ? (
        <div className="max-h-80 overflow-y-auto py-1">
          {entries.map((entry) => {
            const isCurrent = entry.slug === currentSlug;
            return (
              <button
                key={entry.slug}
                onClick={() => {
                  navigate(`/p/${entry.slug}`);
                  onClose();
                }}
                className={`w-full text-left px-3 py-3 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none ${
                  isCurrent
                    ? 'bg-efi-gold/15 border-l-2 border-efi-gold'
                    : 'border-l-2 border-transparent hover:bg-white/5'
                }`}
              >
                <div className="flex items-center gap-2">
                  <span className={`text-sm font-medium truncate ${isCurrent ? 'text-efi-gold-light' : 'text-efi-text-primary'}`}>
                    {entry.name}
                  </span>
                  <span className={`shrink-0 text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${
                    entry.isAdmin
                      ? 'bg-efi-gold/20 text-efi-gold-light border-efi-gold/30'
                      : 'bg-white/8 text-efi-text-secondary border-white/10'
                  }`}>
                    {entry.isAdmin ? 'Admin' : 'Voter'}
                  </span>
                </div>
              </button>
            );
          })}
        </div>
      ) : (
        <div className="px-3 py-4 text-center">
          <p className="text-xs text-efi-text-tertiary">No projects yet</p>
          <Link
            to="/"
            onClick={onClose}
            className="text-xs text-efi-gold-light hover:text-efi-gold transition-colors no-underline"
          >
            Join or create a project
          </Link>
        </div>
      )}

      <div className="border-t border-white/8 py-1">
        <Link
          to="/"
          onClick={onClose}
          className="block px-3 py-2.5 text-xs text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/5 transition-colors no-underline"
        >
          Home
        </Link>
      </div>
    </div>
  );
}
