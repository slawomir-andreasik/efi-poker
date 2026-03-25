// Auth

export interface AuthConfigResponse {
  auth0Enabled: boolean;
  registrationEnabled: boolean;
  ldapEnabled: boolean;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
}

export type UserRole = 'ADMIN' | 'USER';

export interface UserResponse {
  id: string;
  username: string;
  role: UserRole;
  hasPassword: boolean;
  authProvider: string;
}

// Admin

export interface AdminUserResponse {
  id: string;
  username: string;
  email: string | null;
  role: UserRole;
  authProvider: string;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface PagedUsersResponse {
  content: AdminUserResponse[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface AdminCreateUserRequest {
  username: string;
  password: string;
  email?: string;
  role: UserRole;
}

export interface AdminUpdateUserRequest {
  email?: string;
  role?: UserRole;
}

export interface ChangePasswordRequest {
  currentPassword?: string;
  newPassword: string;
}

export interface AdminResetPasswordRequest {
  newPassword: string;
}

// Enums

export type StoryPoints = '0' | '0.5' | '1' | '2' | '3' | '5' | '8' | '13' | '21' | '?' | 'N/A';

export const SP_VALUES: StoryPoints[] = [
  '0',
  '0.5',
  '1',
  '2',
  '3',
  '5',
  '8',
  '13',
  '21',
  '?',
  'N/A',
];

export const SP_NOT_APPLICABLE: StoryPoints = 'N/A';

export type RoomStatus = 'OPEN' | 'REVEALED' | 'CLOSED';

export type RoomType = 'ASYNC' | 'LIVE';

// Responses

export interface ProjectResponse {
  id: string;
  name: string;
  slug: string;
  createdAt: string;
}

export interface ProjectAdminResponse {
  id: string;
  name: string;
  slug: string;
  adminCode: string;
  token?: string;
  tokenExpiresAt?: string;
  createdAt: string;
}

export interface RoomResponse {
  id: string;
  slug: string;
  projectId: string;
  title: string;
  description?: string;
  deadline: string;
  topic?: string;
  roundNumber: number;
  autoRevealOnDeadline: boolean;
  commentTemplate?: string;
  commentRequired?: boolean;
  roomType: RoomType;
  status: RoomStatus;
  createdAt: string;
}

export interface RoomDetailResponse {
  id: string;
  slug: string;
  title: string;
  description?: string;
  deadline: string;
  topic?: string;
  roundNumber: number;
  autoRevealOnDeadline: boolean;
  commentTemplate?: string;
  commentRequired?: boolean;
  roomType: RoomType;
  status: RoomStatus;
  tasks: TaskWithEstimateResponse[];
}

export interface RoomAdminResponse {
  id: string;
  slug: string;
  title: string;
  description?: string;
  deadline: string;
  topic?: string;
  roundNumber: number;
  autoRevealOnDeadline: boolean;
  commentTemplate?: string;
  commentRequired?: boolean;
  roomType: RoomType;
  status: RoomStatus;
  tasks: TaskWithAllEstimatesResponse[];
  participants: ParticipantResponse[];
}

export interface RoomProgressResponse {
  roomId: string;
  slug: string;
  status: RoomStatus;
  totalParticipants: number;
  tasks: TaskProgressResponse[];
}

export interface RoomResultsResponse {
  roomId: string;
  slug: string;
  title: string;
  status: RoomStatus;
  tasks: TaskResultResponse[];
}

export interface RoomSlugResponse {
  roomId: string;
  roomTitle: string;
  roomSlug: string;
  projectSlug: string;
  projectName: string;
}

export interface TaskResponse {
  id: string;
  roomId: string;
  title: string;
  description?: string;
  sortOrder: number;
  finalEstimate: string | null;
  revealed: boolean;
  active: boolean;
  createdAt: string;
}

export interface TaskWithEstimateResponse {
  id: string;
  title: string;
  description?: string;
  sortOrder: number;
  myEstimate: EstimateResponse | null;
  allEstimates: EstimateResponse[];
  averagePoints: number | null;
  medianPoints: number | null;
  finalEstimate: string | null;
  votedCount: number;
  questionVotesCount?: number;
  totalParticipants: number;
  revealed: boolean;
  active: boolean;
}

export interface TaskWithAllEstimatesResponse {
  id: string;
  title: string;
  description?: string;
  sortOrder: number;
  finalEstimate: string | null;
  estimates: EstimateResponse[];
  revealed: boolean;
  active: boolean;
}

export interface TaskProgressResponse {
  taskId: string;
  title: string;
  votedCount: number;
  questionVotesCount?: number;
  totalParticipants: number;
}

export interface TaskResultResponse {
  taskId: string;
  title: string;
  estimates: EstimateResponse[];
  averagePoints: number;
  medianPoints: number;
  finalEstimate: string | null;
}

export interface EstimateResponse {
  id: string;
  participantId: string;
  participantNickname: string;
  storyPoints?: string | null;
  comment?: string;
  createdAt: string;
}

export interface ParticipantResponse {
  id: string;
  nickname: string;
  token?: string;
  tokenExpiresAt?: string;
  invitedRoomIds?: string[];
  archived?: boolean;
  archivedAt?: string;
  createdAt: string;
}

// Requests

export interface LiveRoomStateResponse {
  roomId: string;
  slug: string;
  title: string;
  topic: string | null;
  status: RoomStatus;
  roundNumber: number;
  commentTemplate?: string;
  commentRequired?: boolean;
  taskId: string;
  myEstimate: EstimateResponse | null;
  questionVotesCount?: number;
  participants: LiveParticipantStatus[];
  results: LiveRoomResults | null;
}

export interface LiveParticipantStatus {
  participantId: string;
  nickname: string;
  hasVoted: boolean;
}

export interface LiveRoomResults {
  estimates: EstimateResponse[];
  averagePoints: number | null;
  medianPoints: number | null;
}

export interface NewRoundRequest {
  topic?: string;
}

export interface CreateProjectRequest {
  name: string;
}

export interface UpdateProjectRequest {
  name?: string;
}

export interface CreateRoomRequest {
  title: string;
  description?: string;
  deadline?: string;
  roomType: RoomType;
  autoRevealOnDeadline?: boolean;
  commentTemplate?: string;
  commentRequired?: boolean;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  sortOrder?: number;
}

export interface ImportTasksRequest {
  titles: string[];
}

export interface SubmitEstimateRequest {
  storyPoints?: StoryPoints | null;
  comment?: string;
}

export interface JoinProjectRequest {
  nickname: string;
  roomId?: string;
}

export interface SetFinalEstimateRequest {
  storyPoints: StoryPoints;
}

// Finish session

export interface FinishSessionResponse {
  status: RoomStatus;
  autoAssignedEstimates: AutoAssignedEstimate[];
}

export interface AutoAssignedEstimate {
  taskId: string;
  taskTitle: string;
  finalEstimate: string;
}

// Analytics

export interface RoomAnalyticsResponse {
  roomId: string;
  title: string;
  slug: string;
  summary: RoomAnalyticsSummary;
  taskAnalytics: TaskAnalyticsEntry[];
  participationMatrix: ParticipationMatrixEntry[];
}

export interface RoomAnalyticsSummary {
  totalTasks: number;
  totalStoryPoints: number;
  consensusCount: number;
  participationRate: number;
  totalParticipants: number;
}

export interface TaskAnalyticsEntry {
  taskId: string;
  title: string;
  averagePoints: number | null;
  medianPoints: number | null;
  finalEstimate: string | null;
  spread: number | null;
  voteDistribution: Record<string, number>;
}

export interface ParticipationMatrixEntry {
  participantId: string;
  nickname: string;
  taskVotes: Record<string, string>;
}

export interface ProjectAnalyticsResponse {
  projectId: string;
  projectName: string;
  slug: string;
  summary: ProjectAnalyticsSummary;
  roomStats: RoomStatsEntry[];
  topContentiousTasks: ContentiousTaskEntry[];
  participantLeaderboard: ParticipantLeaderboardEntry[];
}

export interface ProjectAnalyticsSummary {
  totalRooms: number;
  totalTasks: number;
  totalStoryPoints: number;
  averageConsensusRate: number;
}

export interface RoomStatsEntry {
  roomId: string;
  title: string;
  totalStoryPoints: number;
  taskCount: number;
  consensusRate: number;
  createdAt: string;
}

export interface ContentiousTaskEntry {
  taskId: string;
  taskTitle: string;
  roomTitle: string;
  spread: number;
  voteCount: number;
}

export interface ParticipantLeaderboardEntry {
  participantId: string;
  nickname: string;
  tasksVoted: number;
  totalTasks: number;
  participationRate: number;
}

// Participant progress

export interface ParticipantProgressResponse {
  roomId: string;
  slug: string;
  totalTasks: number;
  participants: ParticipantProgressEntry[];
}

export interface ParticipantProgressEntry {
  nickname: string;
  votedCount: number;
  totalTasks: number;
  hasCommentedAll?: boolean;
}

// Round history

export interface RoundHistoryVote {
  nickname: string;
  storyPoints: string;
}

export interface RoundHistoryEntry {
  roundNumber: number;
  topic: string | null;
  averagePoints: number | null;
  medianPoints: number | null;
  voteCount: number;
  votes: RoundHistoryVote[];
  completedAt: string;
}

// Guest JWT

export interface GuestTokenResponse {
  token: string;
  expiresAt: string;
}

export interface AdminCodeExchangeRequest {
  slug: string;
  adminCode: string;
}
