import { useRef } from 'react';
import { Link } from 'react-router-dom';
import { getAllProjects } from '@/api/client';
import type { ProjectAuth } from '@/api/client';
import { useDropdownDismiss } from '@/hooks/useDropdownDismiss';
import { RoleBadge } from '@/components/RoleBadge';

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
  const dropdownRef = useRef<HTMLDivElement>(null);
  const entries = getProjectEntries();

  useDropdownDismiss(dropdownRef, true, onClose);

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
              <Link
                key={entry.slug}
                to={`/p/${entry.slug}`}
                onClick={onClose}
                className={`block px-3 py-3 transition-colors no-underline focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none ${
                  isCurrent
                    ? 'bg-efi-gold/15 border-l-2 border-efi-gold'
                    : 'border-l-2 border-transparent hover:bg-white/5'
                }`}
              >
                <div className="flex items-center gap-2">
                  <span className={`text-sm font-medium truncate ${isCurrent ? 'text-efi-gold-light' : 'text-efi-text-primary'}`}>
                    {entry.name}
                  </span>
                  <RoleBadge isAdmin={entry.isAdmin} />
                </div>
              </Link>
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
