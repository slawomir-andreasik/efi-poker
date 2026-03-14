import { lazy, Suspense, useEffect, useLayoutEffect } from 'react';
import { Routes, Route, useLocation } from 'react-router-dom';
import { Header } from '@/components/layout/Header';
import { Breadcrumbs } from '@/components/layout/Breadcrumbs';
import { Footer } from '@/components/layout/Footer';
import { ErrorBoundary } from '@/components/ErrorBoundary';
import { BackendGate } from '@/components/BackendGate';
import { useToast } from '@/components/Toast';
import { Spinner } from '@/components/Spinner';
import { resetTrace } from '@/lib/tracing';
import { useVersionCheck } from '@/hooks/useVersionCheck';
import { usePageRestore } from '@/hooks/usePageRestore';

const HomePage = lazy(() => import('@/pages/HomePage').then(m => ({ default: m.HomePage })));
const ProjectPage = lazy(() => import('@/pages/ProjectPage').then(m => ({ default: m.ProjectPage })));
const JoinPage = lazy(() => import('@/pages/JoinPage').then(m => ({ default: m.JoinPage })));
const RoomPage = lazy(() => import('@/pages/RoomPage').then(m => ({ default: m.RoomPage })));
const ResultsPage = lazy(() => import('@/pages/ResultsPage').then(m => ({ default: m.ResultsPage })));
const RoomJoinRedirectPage = lazy(() => import('@/pages/RoomJoinRedirectPage').then(m => ({ default: m.RoomJoinRedirectPage })));
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage').then(m => ({ default: m.NotFoundPage })));
const AuthCallbackPage = lazy(() => import('@/pages/AuthCallbackPage').then(m => ({ default: m.AuthCallbackPage })));
const LoginPage = lazy(() => import('@/pages/LoginPage').then(m => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import('@/pages/RegisterPage').then(m => ({ default: m.RegisterPage })));
const AdminUsersPage = lazy(() => import('@/pages/AdminUsersPage').then(m => ({ default: m.AdminUsersPage })));
const RoomAnalyticsPage = lazy(() => import('@/pages/RoomAnalyticsPage').then(m => ({ default: m.RoomAnalyticsPage })));
const ProjectAnalyticsPage = lazy(() => import('@/pages/ProjectAnalyticsPage').then(m => ({ default: m.ProjectAnalyticsPage })));

export function App() {
  const location = useLocation();
  const { showToast } = useToast();
  useVersionCheck({ showToast });
  usePageRestore();

  useEffect(() => {
    resetTrace();
  }, [location.pathname]);

  // Suppress CSS transitions during SPA route changes to prevent flash on back/forward nav.
  // useLayoutEffect runs after DOM update but before paint - exactly the right timing.
  useLayoutEffect(() => {
    document.documentElement.classList.add('no-transitions');
    const frameId = requestAnimationFrame(() => {
      document.documentElement.classList.remove('no-transitions');
    });
    return () => cancelAnimationFrame(frameId);
  }, [location.pathname]);

  useEffect(() => {
    const handleOffline = () => showToast('You are offline', 'error');
    const handleOnline = () => showToast('Connection restored', 'success');
    window.addEventListener('offline', handleOffline);
    window.addEventListener('online', handleOnline);
    return () => {
      window.removeEventListener('offline', handleOffline);
      window.removeEventListener('online', handleOnline);
    };
  }, [showToast]);

  return (
    <div className="min-h-dvh flex flex-col pt-safe pb-safe">
      <Header />
      <Breadcrumbs />
      <main className="flex-1">
        <ErrorBoundary>
          <BackendGate>
            <Suspense fallback={<div className="flex justify-center py-20"><Spinner /></div>}>
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/admin/users" element={<AdminUsersPage />} />
                <Route path="/p/:slug" element={<ProjectPage />} />
                <Route path="/p/:slug/join" element={<JoinPage />} />
                <Route path="/r/:roomSlug" element={<RoomJoinRedirectPage />} />
                <Route path="/p/:slug/r/:roomId/analytics" element={<RoomAnalyticsPage />} />
                <Route path="/p/:slug/analytics" element={<ProjectAnalyticsPage />} />
                <Route path="/p/:slug/r/:roomId" element={<RoomPage />} />
                <Route path="/p/:slug/r/:roomId/results" element={<ResultsPage />} />
                <Route path="/auth/callback" element={<AuthCallbackPage />} />
                <Route path="*" element={<NotFoundPage />} />
              </Routes>
            </Suspense>
          </BackendGate>
        </ErrorBoundary>
      </main>
      <Footer />
    </div>
  );
}
